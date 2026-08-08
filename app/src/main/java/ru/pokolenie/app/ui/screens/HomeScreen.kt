package ru.pokolenie.app.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import ru.pokolenie.app.ui.components.GhostButton
import ru.pokolenie.app.ui.components.Panel
import ru.pokolenie.app.ui.components.PrimaryButton
import ru.pokolenie.app.ui.theme.Brass
import ru.pokolenie.app.ui.theme.BrassSoft
import ru.pokolenie.app.ui.theme.Mist
import ru.pokolenie.app.ui.theme.MistDim
import ru.pokolenie.app.ui.theme.SignalGreen
import ru.pokolenie.app.ui.theme.SignalRed
import ru.pokolenie.app.viewmodel.HomeUiState
import ru.pokolenie.app.vpn.VpnConnectionState

@Composable
fun HomeScreen(
    state: HomeUiState,
    onConnectProxy: () -> Unit,
    onConnectWarp: () -> Unit,
    onDisconnect: () -> Unit,
    onRefresh: () -> Unit,
    onPingAll: () -> Unit
) {
    val connected = state.vpnState == VpnConnectionState.Connected
    val ringColor by animateColorAsState(
        if (connected) SignalGreen else Brass,
        animationSpec = tween(500),
        label = "ring"
    )
    val pulse by animateFloatAsState(
        if (connected) 1.05f else 1f,
        animationSpec = tween(700),
        label = "pulse"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF0B0F14), Color(0xFF121820), Color(0xFF0B0F14))
                )
            )
    ) {
        Atmosphere()
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("POKOLENIE", style = MaterialTheme.typography.displayLarge, color = BrassSoft)
            Text(
                "whitelist · warp · vless/trojan",
                style = MaterialTheme.typography.bodyMedium,
                color = MistDim,
                modifier = Modifier.padding(top = 4.dp, bottom = 28.dp)
            )

            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(220.dp)) {
                Canvas(modifier = Modifier
                    .size(220.dp)
                    .scale(pulse)) {
                    drawCircle(
                        color = ringColor.copy(alpha = 0.12f),
                        radius = size.minDimension / 2f
                    )
                    drawCircle(
                        color = ringColor.copy(alpha = 0.35f),
                        radius = size.minDimension / 2.4f,
                        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 4.dp.toPx())
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        when (state.vpnState) {
                            VpnConnectionState.Connected -> "ОНЛАЙН"
                            VpnConnectionState.Connecting -> "…"
                            VpnConnectionState.Error -> "ОШИБКА"
                            VpnConnectionState.Disconnected -> "ОФФЛАЙН"
                        },
                        style = MaterialTheme.typography.titleLarge,
                        color = if (connected) SignalGreen else Mist
                    )
                    Text(
                        if (state.settings.whitelistForced) "whitelist включён" else "",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MistDim
                    )
                }
            }

            Spacer(Modifier = Modifier.height(12.dp))

            Panel {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    MetaRow("Сервер", state.selectedServer?.name ?: "не выбран")
                    MetaRow(
                        "Протокол",
                        state.selectedServer?.protocol?.name
                            ?: state.selectedWarp?.let { "WARP" }
                            ?: "—"
                    )
                    MetaRow("Узлов", state.serverCount.toString())
                    MetaRow(
                        "Лучший пинг",
                        state.bestLatency?.let { "$it ms" } ?: "нет данных"
                    )
                    MetaRow(
                        "Ядро",
                        if (state.libboxReady) "libbox" else "stub (положите libbox.aar)"
                    )
                    state.statusMessage?.let {
                        Text(it, color = MistDim, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }

            Spacer(Modifier = Modifier.height(16.dp))

            if (state.busy) {
                CircularProgressIndicator(color = Brass, modifier = Modifier.padding(8.dp))
            }

            if (connected) {
                PrimaryButton("Отключить", onClick = onDisconnect)
            } else {
                PrimaryButton(
                    text = "Подключить лучший / выбранный",
                    onClick = onConnectProxy,
                    enabled = !state.busy
                )
                Spacer(Modifier = Modifier.height(10.dp))
                PrimaryButton(
                    text = "Подключить Warp",
                    onClick = onConnectWarp,
                    enabled = !state.busy
                )
            }

            Spacer(Modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                GhostButton("Обновить ключи", onClick = onRefresh, modifier = Modifier.weight(1f), enabled = !state.busy)
                GhostButton("Пинг всех", onClick = onPingAll, modifier = Modifier.weight(1f), enabled = !state.busy)
            }

            if (state.vpnState == VpnConnectionState.Error) {
                Text(
                    state.statusMessage ?: "Ошибка VPN",
                    color = SignalRed,
                    modifier = Modifier.padding(top = 12.dp)
                )
            }
        }
    }
}

@Composable
private fun MetaRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = MistDim, style = MaterialTheme.typography.bodyMedium)
        Text(
            value,
            color = Mist,
            style = MaterialTheme.typography.bodyLarge,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(start = 12.dp)
        )
    }
}

@Composable
private fun Atmosphere() {
    Box(modifier = Modifier.fillMaxSize()) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Brass.copy(alpha = 0.14f), Color.Transparent),
                    center = Offset(size.width * 0.2f, size.height * 0.15f),
                    radius = size.minDimension * 0.55f
                ),
                radius = size.minDimension * 0.55f,
                center = Offset(size.width * 0.2f, size.height * 0.15f)
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFF1E3A5F).copy(alpha = 0.35f), Color.Transparent),
                    center = Offset(size.width * 0.85f, size.height * 0.75f),
                    radius = size.minDimension * 0.7f
                ),
                radius = size.minDimension * 0.7f,
                center = Offset(size.width * 0.85f, size.height * 0.75f)
            )
        }
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 40.dp, end = 28.dp)
                .size(8.dp)
                .background(Brass.copy(alpha = 0.5f), CircleShape)
        )
    }
}
