package ru.pokolenie.core

/**
 * Abstraction over sing-box libbox so the UI module can build without the AAR.
 */
interface VpnEngine {
    val isAvailable: Boolean
    fun start(configJson: String)
    fun stop()
    fun isRunning(): Boolean
}

class StubVpnEngine : VpnEngine {
    @Volatile
    private var running = false
    private var lastConfig: String? = null

    override val isAvailable: Boolean = false

    override fun start(configJson: String) {
        lastConfig = configJson
        running = true
    }

    override fun stop() {
        running = false
    }

    override fun isRunning(): Boolean = running

    fun lastConfigOrNull(): String? = lastConfig
}

object VpnEngineProvider {
    @Volatile
    private var engine: VpnEngine? = null

    fun get(): VpnEngine {
        engine?.let { return it }
        synchronized(this) {
            engine?.let { return it }
            val created = create()
            engine = created
            return created
        }
    }

    fun resetForTests() {
        engine = null
    }

    private fun create(): VpnEngine {
        return try {
            Class.forName("io.nekohasekai.libbox.Libbox")
            val clazz = Class.forName("ru.pokolenie.app.vpn.LibboxVpnEngine")
            clazz.getDeclaredConstructor().newInstance() as VpnEngine
        } catch (_: Throwable) {
            StubVpnEngine()
        }
    }
}
