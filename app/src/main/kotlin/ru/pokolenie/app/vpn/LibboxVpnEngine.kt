package ru.pokolenie.app.vpn

import android.net.VpnService
import android.os.ParcelFileDescriptor
import android.util.Log
import ru.pokolenie.core.VpnEngine
import java.io.File
import java.lang.reflect.InvocationHandler
import java.lang.reflect.Method
import java.lang.reflect.Proxy

/**
 * Bridge to sing-box libbox (`io.nekohasekai.libbox.*`).
 * API: Libbox.setup(SetupOptions) + Libbox.newService(config, platform) + BoxService.start().
 */
class LibboxVpnEngine : VpnEngine {
    @Volatile
    private var running = false

    @Volatile
    private var boxInstance: Any? = null

    @Volatile
    private var tunPfd: ParcelFileDescriptor? = null

    override val isAvailable: Boolean = hasLibbox()

    override fun start(configJson: String) {
        error("Use start(service, configJson, meta) from PokolenieVpnService")
    }

    fun start(service: VpnService, configJson: String, meta: TunMeta) {
        if (!isAvailable) error("libbox.aar missing")
        stop()

        val base = service.filesDir
        val workDir = File(base, "singbox").apply { mkdirs() }
        val tempDir = File(service.cacheDir, "singbox").apply { mkdirs() }
        File(workDir, "config.json").writeText(configJson)

        val libbox = Class.forName("io.nekohasekai.libbox.Libbox")
        setup(libbox, workDir, tempDir)

        val platform = buildPlatformInterface(service, meta)
        val newService = libbox.methods.firstOrNull {
            it.name == "newService" && it.parameterTypes.size == 2
        } ?: error("Libbox.newService not found")

        try {
            boxInstance = newService.invoke(null, configJson, platform)
        } catch (e: Exception) {
            val cause = e.cause ?: e
            error("newService failed: ${cause.message ?: cause.javaClass.simpleName}")
        }

        val startMethod = boxInstance!!.javaClass.methods.firstOrNull {
            it.name == "start" && it.parameterCount == 0
        } ?: error("BoxService.start not found")
        try {
            startMethod.invoke(boxInstance)
        } catch (e: Exception) {
            val cause = e.cause ?: e
            stop()
            error("BoxService.start failed: ${cause.message ?: cause.javaClass.simpleName}")
        }

        running = true
        val version = runCatching {
            libbox.getMethod("version").invoke(null)?.toString()
        }.getOrNull()
        VpnDiagnostics.log("libbox started${version?.let { " · $it" } ?: ""}")
        Log.i(TAG, "libbox started version=$version")
    }

    override fun stop() {
        val instance = boxInstance
        boxInstance = null
        running = false
        runCatching {
            instance?.javaClass?.methods?.firstOrNull { it.name == "close" && it.parameterCount == 0 }
                ?.invoke(instance)
        }
        runCatching { tunPfd?.close() }
        tunPfd = null
    }

    override fun isRunning(): Boolean = running

    private fun setup(libbox: Class<*>, workDir: File, tempDir: File) {
        val optionsClass = Class.forName("io.nekohasekai.libbox.SetupOptions")
        val options = optionsClass.getDeclaredConstructor().newInstance()
        optionsClass.getMethod("setBasePath", String::class.java).invoke(options, workDir.absolutePath)
        optionsClass.getMethod("setWorkingPath", String::class.java).invoke(options, workDir.absolutePath)
        optionsClass.getMethod("setTempPath", String::class.java).invoke(options, tempDir.absolutePath)
        runCatching {
            optionsClass.getMethod("setFixAndroidStack", Boolean::class.javaPrimitiveType)
                .invoke(options, true)
        }
        libbox.getMethod("setup", optionsClass).invoke(null, options)
    }

    private fun buildPlatformInterface(service: VpnService, meta: TunMeta): Any {
        val iface = Class.forName("io.nekohasekai.libbox.PlatformInterface")
        val handler = PlatformHandler(service, meta) { pfd ->
            tunPfd = pfd
        }
        return Proxy.newProxyInstance(iface.classLoader, arrayOf(iface), handler)
    }

    private class PlatformHandler(
        private val service: VpnService,
        private val meta: TunMeta,
        private val onTun: (ParcelFileDescriptor) -> Unit
    ) : InvocationHandler {
        override fun invoke(proxy: Any, method: Method, args: Array<out Any>?): Any? {
            return when (method.name) {
                "openTun" -> openTun(args?.firstOrNull())
                "useProcFS" -> true
                "usePlatformAutoDetectInterfaceControl" -> true
                "autoDetectInterfaceControl" -> {
                    val fd = (args?.firstOrNull() as? Number)?.toInt() ?: return null
                    service.protect(fd)
                    null
                }
                "findConnectionOwner" -> -1
                "packageNameByUid" -> ""
                "uidByPackageName" -> -1
                "startDefaultInterfaceMonitor", "closeDefaultInterfaceMonitor" -> null
                "getInterfaces" -> emptyNetworkInterfaceIterator()
                "underNetworkExtension", "includeAllNetworks" -> false
                "readWIFIState" -> null
                "clearDNSCache", "sendNotification" -> null
                "writeLog" -> {
                    val line = args?.firstOrNull()?.toString().orEmpty()
                    Log.d(TAG, line)
                    if (line.isNotBlank()) VpnDiagnostics.log(line.take(200))
                    null
                }
                else -> LibboxVpnEngine.companionDefault(method.returnType)
            }
        }

        private fun openTun(options: Any?): Int {
            val builder = service.Builder()
                .setSession("Pokolenie")
                .setBlocking(false)

            val mtu = invokeInt(options, "getMTU").takeIf { it > 0 } ?: meta.mtu
            builder.setMtu(mtu)

            var addedAddress = false
            addedAddress = addRoutePrefixes(builder, options, "getInet4Address", asRoute = false) || addedAddress
            addedAddress = addRoutePrefixes(builder, options, "getInet6Address", asRoute = false) || addedAddress
            if (!addedAddress) {
                builder.addAddress("172.19.0.1", 30)
            }

            val hasRoutes =
                addRoutePrefixes(builder, options, "getInet4RouteAddress", asRoute = true) ||
                    addRoutePrefixes(builder, options, "getInet4RouteRange", asRoute = true) ||
                    addRoutePrefixes(builder, options, "getInet6RouteAddress", asRoute = true) ||
                    addRoutePrefixes(builder, options, "getInet6RouteRange", asRoute = true)
            if (!hasRoutes) {
                builder.addRoute("0.0.0.0", 0)
                if (meta.ipv6) builder.addRoute("::", 0)
            }

            builder.addDnsServer("1.1.1.1")

            applyPackages(builder, options, "getIncludePackage", include = true)
            applyPackages(builder, options, "getExcludePackage", include = false)
            when (meta.splitMode) {
                "INCLUDE" -> meta.splitPackages.forEach {
                    runCatching { builder.addAllowedApplication(it) }
                }
                "EXCLUDE" -> meta.splitPackages.forEach {
                    runCatching { builder.addDisallowedApplication(it) }
                }
            }
            runCatching { builder.addDisallowedApplication(service.packageName) }

            val established = builder.establish()
                ?: error("VpnService.Builder.establish() returned null")
            onTun(established)
            VpnDiagnostics.log("TUN fd=${established.fd} mtu=$mtu")
            return established.fd
        }

        private fun applyPackages(builder: VpnService.Builder, options: Any?, method: String, include: Boolean) {
            val iterator = invokeOrNull(options, method) ?: return
            while (invokeBool(iterator, "hasNext")) {
                val pkg = invokeOrNull(iterator, "next")?.toString().orEmpty()
                if (pkg.isBlank()) continue
                runCatching {
                    if (include) builder.addAllowedApplication(pkg)
                    else builder.addDisallowedApplication(pkg)
                }
            }
        }

        private fun addRoutePrefixes(
            builder: VpnService.Builder,
            options: Any?,
            method: String,
            asRoute: Boolean
        ): Boolean {
            val iterator = invokeOrNull(options, method) ?: return false
            var any = false
            while (invokeBool(iterator, "hasNext")) {
                val prefix = invokeOrNull(iterator, "next") ?: continue
                val address = invokeOrNull(prefix, "address")?.toString()
                    ?: invokeOrNull(prefix, "getAddress")?.toString()
                    ?: continue
                val prefixLen = invokeInt(prefix, "prefix")
                    .takeIf { it > 0 }
                    ?: invokeInt(prefix, "getPrefix")
                if (prefixLen <= 0) continue
                any = true
                if (asRoute) builder.addRoute(address, prefixLen)
                else builder.addAddress(address, prefixLen)
            }
            return any
        }

        private fun emptyNetworkInterfaceIterator(): Any {
            val clazz = Class.forName("io.nekohasekai.libbox.NetworkInterfaceIterator")
            return Proxy.newProxyInstance(clazz.classLoader, arrayOf(clazz)) { _, m, _ ->
                when (m.name) {
                    "hasNext" -> false
                    "next" -> null
                    else -> LibboxVpnEngine.companionDefault(m.returnType)
                }
            }
        }

        private fun invokeOrNull(target: Any?, name: String): Any? {
            if (target == null) return null
            return runCatching {
                target.javaClass.methods.firstOrNull { it.name == name && it.parameterCount == 0 }
                    ?.invoke(target)
            }.getOrNull()
        }

        private fun invokeBool(target: Any?, name: String): Boolean =
            invokeOrNull(target, name) as? Boolean ?: false

        private fun invokeInt(target: Any?, name: String): Int =
            (invokeOrNull(target, name) as? Number)?.toInt() ?: 0
    }

    data class TunMeta(
        val mtu: Int = 1280,
        val ipv6: Boolean = false,
        val splitMode: String = "ALL",
        val splitPackages: List<String> = emptyList()
    )

    companion object {
        private const val TAG = "LibboxVpnEngine"

        fun hasLibbox(): Boolean = try {
            Class.forName("io.nekohasekai.libbox.Libbox")
            true
        } catch (_: Throwable) {
            false
        }

        fun companionDefault(type: Class<*>): Any? = when (type) {
            java.lang.Boolean.TYPE -> false
            java.lang.Integer.TYPE -> 0
            java.lang.Long.TYPE -> 0L
            java.lang.Float.TYPE -> 0f
            java.lang.Double.TYPE -> 0.0
            else -> null
        }
    }
}
