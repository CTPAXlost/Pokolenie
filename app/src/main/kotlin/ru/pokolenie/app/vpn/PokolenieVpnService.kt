package ru.pokolenie.app.vpn

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.net.VpnService
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.ParcelFileDescriptor
import android.util.Log
import androidx.core.app.NotificationCompat
import org.json.JSONObject
import ru.pokolenie.app.MainActivity
import ru.pokolenie.app.R
import ru.pokolenie.core.StubVpnEngine
import ru.pokolenie.core.VpnEngineProvider

class PokolenieVpnService : VpnService() {

    private var tun: ParcelFileDescriptor? = null
    private val trafficHandler = Handler(Looper.getMainLooper())
    private var trafficEngineLabel = "stub"
    private val trafficTicker = object : Runnable {
        override fun run() {
            VpnDiagnostics.pollTraffic(trafficEngineLabel)
            trafficHandler.postDelayed(this, 1000L)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_CONNECT -> {
                val config = intent.getStringExtra(EXTRA_CONFIG).orEmpty()
                val label = intent.getStringExtra(EXTRA_LABEL) ?: "Pokolenie"
                startForeground(NOTIFICATION_ID, buildNotification(getString(R.string.vpn_notification_connecting)))
                VpnDiagnostics.log("Connecting: $label")
                connectInternal(config, label)
            }
            ACTION_DISCONNECT -> disconnectInternal()
        }
        return START_STICKY
    }

    private fun connectInternal(configJson: String, label: String) {
        try {
            disconnectTunOnly()
            val meta = runCatching { JSONObject(configJson).optJSONObject("_pokolenie") }.getOrNull()
            val mtu = meta?.optInt("mtu", 1280) ?: 1280
            val splitMode = meta?.optString("split_mode", "ALL") ?: "ALL"
            val packages = meta?.optJSONArray("split_packages")

            val builder = Builder()
                .setSession("Pokolenie")
                .setMtu(mtu)
                .addAddress("172.19.0.1", 30)
                .addRoute("0.0.0.0", 0)
                .addDnsServer("1.1.1.1")
                .setBlocking(false)

            if (packages != null && splitMode != "ALL") {
                for (i in 0 until packages.length()) {
                    val pkg = packages.optString(i)
                    if (pkg.isNullOrBlank()) continue
                    try {
                        when (splitMode) {
                            "INCLUDE" -> builder.addAllowedApplication(pkg)
                            "EXCLUDE" -> builder.addDisallowedApplication(pkg)
                        }
                    } catch (e: Exception) {
                        Log.w(TAG, "split package ignored: $pkg", e)
                    }
                }
            }

            try {
                builder.addDisallowedApplication(packageName)
            } catch (_: Exception) {
            }

            val established = builder.establish()
            if (established == null) {
                VpnController.onError("Не удалось создать TUN")
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
                return
            }
            tun = established

            val cleanConfig = JSONObject(configJson).also { it.remove("_pokolenie") }.toString()
            val engine = VpnEngineProvider.get()
            trafficEngineLabel = when (engine) {
                is LibboxVpnEngine -> "libbox"
                is StubVpnEngine -> "stub"
                else -> engine.javaClass.simpleName
            }
            when (engine) {
                is LibboxVpnEngine -> {
                    VpnDiagnostics.log("Starting libbox…")
                    engine.start(this, cleanConfig, established.fd)
                }
                is StubVpnEngine -> {
                    engine.start(cleanConfig)
                    VpnDiagnostics.log("WARN: libbox.aar missing — stub TUN (трафик не проксируется)")
                    Log.w(TAG, "libbox.aar not found — stub mode (config stored, UI works)")
                }
                else -> engine.start(cleanConfig)
            }

            startForeground(NOTIFICATION_ID, buildNotification(getString(R.string.vpn_notification_connected)))
            VpnDiagnostics.markConnected(trafficEngineLabel)
            trafficHandler.removeCallbacks(trafficTicker)
            trafficHandler.post(trafficTicker)
            VpnController.onConnected(label)
        } catch (e: Exception) {
            Log.e(TAG, "connect failed", e)
            VpnDiagnostics.log("ERROR: ${e.message}")
            VpnController.onError(e.message ?: "VPN error")
            disconnectInternal()
        }
    }

    private fun disconnectInternal() {
        trafficHandler.removeCallbacks(trafficTicker)
        VpnDiagnostics.markDisconnected()
        runCatching { VpnEngineProvider.get().stop() }
        disconnectTunOnly()
        VpnController.onDisconnected()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun disconnectTunOnly() {
        runCatching { tun?.close() }
        tun = null
    }

    override fun onDestroy() {
        disconnectInternal()
        super.onDestroy()
    }

    override fun onRevoke() {
        disconnectInternal()
        super.onRevoke()
    }

    private fun buildNotification(text: String): Notification {
        val channelId = "pokolenie_vpn"
        val nm = getSystemService(NotificationManager::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            nm.createNotificationChannel(
                NotificationChannel(channelId, "Pokolenie VPN", NotificationManager.IMPORTANCE_LOW)
            )
        }
        val pi = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, channelId)
            .setContentTitle(getString(R.string.vpn_notification_title))
            .setContentText(text)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(pi)
            .setOngoing(true)
            .build()
    }

    companion object {
        private const val TAG = "PokolenieVpn"
        const val ACTION_CONNECT = "ru.pokolenie.app.vpn.CONNECT"
        const val ACTION_DISCONNECT = "ru.pokolenie.app.vpn.DISCONNECT"
        const val EXTRA_CONFIG = "config"
        const val EXTRA_LABEL = "label"
        private const val NOTIFICATION_ID = 42
    }
}
