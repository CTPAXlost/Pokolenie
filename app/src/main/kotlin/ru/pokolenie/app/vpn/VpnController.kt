package ru.pokolenie.app.vpn

import android.content.Context
import android.content.Intent
import android.net.VpnService
import androidx.core.content.ContextCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class VpnConnectionState {
    Disconnected, Connecting, Connected, Error
}

object VpnController {
    private val _state = MutableStateFlow(VpnConnectionState.Disconnected)
    val state: StateFlow<VpnConnectionState> = _state.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    private val _activeConfig = MutableStateFlow<String?>(null)
    val activeConfig: StateFlow<String?> = _activeConfig.asStateFlow()

    fun prepareIntent(context: Context): Intent? = VpnService.prepare(context)

    fun connect(context: Context, configJson: String, modeLabel: String) {
        _state.value = VpnConnectionState.Connecting
        _message.value = "Подключение ($modeLabel)…"
        _activeConfig.value = configJson
        val intent = Intent(context, PokolenieVpnService::class.java).apply {
            action = PokolenieVpnService.ACTION_CONNECT
            putExtra(PokolenieVpnService.EXTRA_CONFIG, configJson)
            putExtra(PokolenieVpnService.EXTRA_LABEL, modeLabel)
        }
        ContextCompat.startForegroundService(context, intent)
    }

    fun disconnect(context: Context) {
        val intent = Intent(context, PokolenieVpnService::class.java).apply {
            action = PokolenieVpnService.ACTION_DISCONNECT
        }
        context.startService(intent)
    }

    internal fun onConnected(label: String) {
        _state.value = VpnConnectionState.Connected
        _message.value = "Whitelist VPN: $label"
    }

    internal fun onDisconnected() {
        _state.value = VpnConnectionState.Disconnected
        _message.value = null
        _activeConfig.value = null
    }

    internal fun onError(error: String) {
        _state.value = VpnConnectionState.Error
        _message.value = error
    }
}
