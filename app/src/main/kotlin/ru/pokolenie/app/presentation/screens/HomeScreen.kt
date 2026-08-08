package ru.pokolenie.app.presentation.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier as ComposeModifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import ru.pokolenie.app.data.db.ServerEntity
import ru.pokolenie.app.presentation.components.GhostButton
import ru.pokolenie.app.presentation.components.Panel
import ru.pokolenie.app.presentation.components.PrimaryButton
import ru.pokolenie.app.presentation.theme.Brass
import ru.pokolenie.app.presentation.theme.BrassSoft
import ru.pokolenie.app.presentation.theme.InkElevated
import ru.pokolenie.app.presentation.theme.Mist
import ru.pokolenie.app.presentation.theme.MistDim
import ru.pokolenie.app.presentation.theme.SignalGreen
import ru.pokolenie.app.presentation.theme.SignalRed
import ru.pokolenie.app.viewmodel.HomeUiState
import ru.pokolenie.app.vpn.VpnConnectionState

@Composable
fun HomeScreen(
    state: HomeUiState,
    servers: List<ServerEntity>,
    onToggleVpn: () -> Unit,
    onSelectServer: (Long) -> Unit,
    onConnectWarp: () -> Unit,
    onRefresh: () -> Unit,
    onPingAll: () -> Unit
) {
    val connected = state.vpnState == VpnConnectionState.Connected
    val connecting = state.vpnState == VpnConnectionState.Connecting
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
    var pickerOpen by remember { mutableStateOf(false) }

    Box(
        modifier = ComposeModifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF0B0F14), Color(0xFF121820), Color(0xFF0B0F14))
                )
            )
    ) {
        Atmosphere()
        Column(
            modifier = ComposeModifier
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
                modifier = ComposeModifier.padding(top = 4.dp, bottom = 20.dp)
            )

            Box(
                contentAlignment = Alignment.Center,
                modifier = ComposeModifier
                    .size(220.dp)
                    .clickable(enabled = !state.busy && !connecting) { onToggleVpn() }
            ) {
                Canvas(
                    modifier = ComposeModifier
                        .size(220.dp)
                        .scale(pulse)
                ) {
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
                            VpnConnectionState.Connected -> "ОТКЛЮЧИТЬ"
                            VpnConnectionState.Connecting -> "…"
                            VpnConnectionState.Error -> "ПОВТОРИТЬ"
                            VpnConnectionState.Disconnected -> "ПОДКЛЮЧИТЬ"
                        },
                        style = MaterialTheme.typography.titleLarge,
                        color = if (connected) SignalGreen else Mist
                    )
                    Text(
                        if (state.settings.whitelistEnabled) "whitelist ON" else "whitelist OFF",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MistDim
                    )
                }
            }

            Spacer(modifier = ComposeModifier.height(12.dp))

            Panel {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Сервер", color = Brass, style = MaterialTheme.typography.titleLarge)
                    Box {
                        Row(
                            modifier = ComposeModifier
                                .fillMaxWidth()
                                .background(InkElevated, RoundedCornerShape(12.dp))
                                .clickable { pickerOpen = true }
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = ComposeModifier.weight(1f)) {
                                Text(
                                    state.selectedServer?.name ?: "не выбран",
                                    color = Mist,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    state.selectedServer?.let { "${it.protocol} · ${it.host}:${it.port}" }
                                        ?: "Выбери сервер из списка",
                                    color = MistDim,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                            Text(
                                state.selectedServer?.latencyMs?.let { "$it ms" } ?: "▾",
                                color = if (state.selectedServer?.latencyMs != null) SignalGreen else Brass
                            )
                        }
                        DropdownMenu(expanded = pickerOpen, onDismissRequest = { pickerOpen = false }) {
                            if (servers.isEmpty()) {
                                DropdownMenuItem(
                                    text = { Text("Нет серверов — обнови источники") },
                                    onClick = { pickerOpen = false }
                                )
                            } else {
                                servers.take(40).forEach { server ->
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                "${server.name} · ${server.latencyMs?.let { "$it ms" } ?: "—"}"
                                            )
                                        },
                                        onClick = {
                                            onSelectServer(server.id)
                                            pickerOpen = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                    MetaRow(
                        "Режим",
                        buildString {
                            append(if (state.settings.whitelistEnabled) "whitelist" else "полный VPN")
                            if (state.settings.fakeIpEnabled) append(" · fakeIP")
                            if (state.settings.fakeDnsEnabled) append(" · fakeDNS")
                        }
                    )
                    MetaRow("Узлов", state.serverCount.toString())
                    MetaRow("Warp", state.selectedWarp?.name ?: "bundled / сгенерируй")
                    state.statusMessage?.let {
                        Text(it, color = MistDim, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }

            Spacer(modifier = ComposeModifier.height(16.dp))

            if (state.busy || connecting) {
                CircularProgressIndicator(color = Brass, modifier = ComposeModifier.padding(8.dp))
            }

            PrimaryButton(
                text = if (connected) "Отключить VPN" else "Подключить выбранный сервер",
                onClick = onToggleVpn,
                enabled = !state.busy && !connecting
            )
            Spacer(modifier = ComposeModifier.height(10.dp))
            PrimaryButton(
                text = "Подключить Warp",
                onClick = onConnectWarp,
                enabled = !state.busy && !connecting && !connected
            )

            Spacer(modifier = ComposeModifier.height(12.dp))
            Row(
                modifier = ComposeModifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                GhostButton(
                    "Обновить ключи",
                    onClick = onRefresh,
                    modifier = ComposeModifier.weight(1f),
                    enabled = !state.busy
                )
                GhostButton(
                    "Пинг всех",
                    onClick = onPingAll,
                    modifier = ComposeModifier.weight(1f),
                    enabled = !state.busy
                )
            }

            if (state.vpnState == VpnConnectionState.Error) {
                Text(
                    state.statusMessage ?: "Ошибка VPN",
                    color = SignalRed,
                    modifier = ComposeModifier.padding(top = 12.dp)
                )
            }
        }
    }
}

@Composable
private fun MetaRow(label: String, value: String) {
    Row(
        modifier = ComposeModifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = MistDim, style = MaterialTheme.typography.bodyMedium)
        Text(
            value,
            color = Mist,
            style = MaterialTheme.typography.bodyLarge,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = ComposeModifier.padding(start = 12.dp)
        )
    }
}

@Composable
private fun Atmosphere() {
    Box(modifier = ComposeModifier.fillMaxSize()) {
        Canvas(modifier = ComposeModifier.fillMaxSize()) {
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
            modifier = ComposeModifier
                .align(Alignment.TopEnd)
                .padding(top = 40.dp, end = 28.dp)
                .size(8.dp)
                .background(Brass.copy(alpha = 0.5f), CircleShape)
        )
    }
}
