package ru.pokolenie.app.vpn

import android.net.TrafficStats
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.CopyOnWriteArrayList

data class TrafficSnapshot(
    val rxBytes: Long = 0,
    val txBytes: Long = 0,
    val rxRate: Long = 0,
    val txRate: Long = 0,
    val engine: String = "stub"
)

object VpnDiagnostics {
    private val logLines = CopyOnWriteArrayList<String>()
    private val _logs = MutableStateFlow<List<String>>(emptyList())
    val logs: StateFlow<List<String>> = _logs.asStateFlow()

    private val _traffic = MutableStateFlow(TrafficSnapshot())
    val traffic: StateFlow<TrafficSnapshot> = _traffic.asStateFlow()

    private var lastRx = 0L
    private var lastTx = 0L
    private var lastTs = 0L
    private var baselineRx = 0L
    private var baselineTx = 0L

    fun log(message: String) {
        val ts = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
        val line = "[$ts] $message"
        logLines.add(0, line)
        while (logLines.size > 80) logLines.removeAt(logLines.lastIndex)
        _logs.value = logLines.toList()
    }

    fun clearLogs() {
        logLines.clear()
        _logs.value = emptyList()
    }

    fun markConnected(engine: String) {
        baselineRx = TrafficStats.getTotalRxBytes().coerceAtLeast(0)
        baselineTx = TrafficStats.getTotalTxBytes().coerceAtLeast(0)
        lastRx = baselineRx
        lastTx = baselineTx
        lastTs = System.currentTimeMillis()
        _traffic.value = TrafficSnapshot(engine = engine)
        log("VPN connected · engine=$engine")
    }

    fun markDisconnected() {
        log("VPN disconnected")
        _traffic.value = TrafficSnapshot(engine = _traffic.value.engine)
    }

    fun pollTraffic(engine: String) {
        val now = System.currentTimeMillis()
        val rx = TrafficStats.getTotalRxBytes().coerceAtLeast(0)
        val tx = TrafficStats.getTotalTxBytes().coerceAtLeast(0)
        val dt = (now - lastTs).coerceAtLeast(1)
        val rxRate = ((rx - lastRx) * 1000L) / dt
        val txRate = ((tx - lastTx) * 1000L) / dt
        lastRx = rx
        lastTx = tx
        lastTs = now
        _traffic.value = TrafficSnapshot(
            rxBytes = (rx - baselineRx).coerceAtLeast(0),
            txBytes = (tx - baselineTx).coerceAtLeast(0),
            rxRate = rxRate.coerceAtLeast(0),
            txRate = txRate.coerceAtLeast(0),
            engine = engine
        )
    }

    fun formatBytes(value: Long): String {
        if (value < 1024) return "$value B"
        val kb = value / 1024.0
        if (kb < 1024) return String.format(Locale.US, "%.1f KB", kb)
        val mb = kb / 1024.0
        if (mb < 1024) return String.format(Locale.US, "%.2f MB", mb)
        return String.format(Locale.US, "%.2f GB", mb / 1024.0)
    }

    fun formatRate(value: Long): String = "${formatBytes(value)}/s"
}
