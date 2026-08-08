package ru.pokolenie.app.vpn

import android.content.Context
import android.util.Log
import ru.pokolenie.core.VpnEngine
import java.io.File
import java.lang.reflect.Proxy

/**
 * Reflection bridge to sing-box libbox (`io.nekohasekai.libbox.*`).
 * Compiles without the AAR; becomes active when [app/libs/libbox.aar] is present.
 */
class LibboxVpnEngine : VpnEngine {
    @Volatile
    private var running = false

    @Volatile
    private var boxInstance: Any? = null

    override val isAvailable: Boolean = hasLibbox()

    override fun start(configJson: String) {
        error("Use start(context, configJson, tunFd) from PokolenieVpnService")
    }

    fun start(context: Context, configJson: String, tunFd: Int) {
        if (!isAvailable) error("libbox.aar missing")
        val base = context.filesDir
        val workDir = File(base, "singbox").apply { mkdirs() }
        val configFile = File(workDir, "config.json")
        configFile.writeText(configJson)

        val libbox = Class.forName("io.nekohasekai.libbox.Libbox")
        runCatching {
            val setup = libbox.methods.firstOrNull { it.name == "setup" }
            setup?.invoke(
                null,
                workDir.absolutePath,
                workDir.absolutePath,
                File(context.cacheDir, "singbox").apply { mkdirs() }.absolutePath,
                false,
                null
            )
        }

        // Prefer BoxService.newService(config, platform) if present
        val serviceClass = Class.forName("io.nekohasekai.libbox.BoxService")
        val platform = buildPlatformInterface(tunFd)
        val ctor = serviceClass.constructors.firstOrNull { it.parameterTypes.size >= 1 }
            ?: error("BoxService constructor not found")

        boxInstance = when (ctor.parameterTypes.size) {
            1 -> ctor.newInstance(configFile.readText())
            2 -> ctor.newInstance(configFile.readText(), platform)
            else -> ctor.newInstance(*Array(ctor.parameterTypes.size) { idx ->
                when (idx) {
                    0 -> configFile.readText()
                    1 -> platform
                    else -> null
                }
            })
        }

        val startMethod = serviceClass.methods.firstOrNull { it.name == "start" && it.parameterCount == 0 }
        startMethod?.invoke(boxInstance)
        running = true
        Log.i(TAG, "libbox started")
    }

    override fun stop() {
        val instance = boxInstance ?: run {
            running = false
            return
        }
        runCatching {
            instance.javaClass.methods.firstOrNull { it.name == "close" || it.name == "stop" }
                ?.invoke(instance)
        }
        boxInstance = null
        running = false
    }

    override fun isRunning(): Boolean = running

    private fun buildPlatformInterface(tunFd: Int): Any {
        val iface = Class.forName("io.nekohasekai.libbox.PlatformInterface")
        return Proxy.newProxyInstance(
            iface.classLoader,
            arrayOf(iface)
        ) { _, method, args ->
            when (method.name) {
                "openTun" -> tunFd.toLong()
                "useProcFS" -> false
                "findConnectionOwner" -> -1
                "packageNameByUid", "uidByPackageName" -> ""
                "startDefaultInterfaceMonitor", "closeDefaultInterfaceMonitor",
                "getInterfaces", "underNetworkExtension", "includeAllNetworks",
                "readWIFIState", "localDNSTransport" -> null
                "clearDNSCache" -> null
                "findProcessInfo" -> null
                "sendNotification" -> null
                "usePlatformAutoDetectInterfaceControl" -> false
                "autoDetectInterfaceControl" -> null
                "writeLog" -> {
                    Log.d(TAG, args?.firstOrNull()?.toString() ?: "")
                    null
                }
                else -> defaultValue(method.returnType)
            }
        }
    }

    private fun defaultValue(type: Class<*>): Any? = when (type) {
        java.lang.Boolean.TYPE -> false
        java.lang.Integer.TYPE -> 0
        java.lang.Long.TYPE -> 0L
        java.lang.Float.TYPE -> 0f
        java.lang.Double.TYPE -> 0.0
        else -> null
    }

    companion object {
        private const val TAG = "LibboxVpnEngine"
        fun hasLibbox(): Boolean = try {
            Class.forName("io.nekohasekai.libbox.Libbox")
            true
        } catch (_: Throwable) {
            false
        }
    }
}
