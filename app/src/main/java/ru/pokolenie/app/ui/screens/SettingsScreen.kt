package ru.pokolenie.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier.modifier
import androidx.compose.ui.unit.dp
import ru.pokolenie.app.settings.DnsMode
import ru.pokolenie.app.settings.SettingsState
import ru.pokolenie.app.ui.components.Panel
import ru.pokolenie.app.ui.components.ScreenScaffold
import ru.pokolenie.app.ui.theme.Brass
import ru.pokolenie.app.ui.theme.Ink
import ru.pokolenie.app.ui.theme.Mist
import ru.pokolenie.app.ui.theme.MistDim

@Composable
fun SettingsScreen(
    settings: SettingsState,
    onMtu: (Int) -> Unit,
    onDnsMode: (DnsMode) -> Unit,
    onDnsServers: (String) -> Unit,
    onDohUrl: (String) -> Unit,
    onIpv6: (Boolean) -> Unit,
    onAllowLan: (Boolean) -> Unit,
    onKeepalive: (Int) -> Unit,
    onAutoPing: (Boolean) -> Unit,
    onPingTimeout: (Int) -> Unit
) {
    ScreenScaffold(
        title = "Настройки",
        subtitle = "MTU, DNS и сеть. Whitelist принудительно включён."
    ) {
        Column(
            modifier = Modifier.verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Panel {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Whitelist", color = Brass, style = MaterialTheme.typography.titleLarge)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Обязательный режим", color = Mist)
                            Text(
                                "Через VPN только белые домены/категории, остальное — direct.",
                                color = MistDim,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                        Switch(
                            checked = true,
                            onCheckedChange = null,
                            enabled = false,
                            colors = SwitchDefaults.colors(checkedTrackColor = Brass)
                        )
                    }
                }
            }

            Panel {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("MTU: ${settings.mtu}", color = Brass, style = MaterialTheme.typography.titleLarge)
                    Slider(
                        value = settings.mtu.toFloat(),
                        onValueChange = { onMtu(it.toInt()) },
                        valueRange = 576f..1500f,
                        steps = 18
                    )
                    Text("Для Warp обычно 1280", color = MistDim, style = MaterialTheme.typography.bodyMedium)
                }
            }

            Panel {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("DNS", color = Brass, style = MaterialTheme.typography.titleLarge)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        DnsChip("System", settings.dnsMode == DnsMode.SYSTEM) { onDnsMode(DnsMode.SYSTEM) }
                        DnsChip("Custom", settings.dnsMode == DnsMode.CUSTOM) { onDnsMode(DnsMode.CUSTOM) }
                        DnsChip("DoH", settings.dnsMode == DnsMode.DOH) { onDnsMode(DnsMode.DOH) }
                    }
                    if (settings.dnsMode == DnsMode.CUSTOM) {
                        OutlinedTextField(
                            value = settings.dnsServers,
                            onValueChange = onDnsServers,
                            label = { Text("Серверы (через запятую)") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    if (settings.dnsMode == DnsMode.DOH) {
                        OutlinedTextField(
                            value = settings.dohUrl,
                            onValueChange = onDohUrl,
                            label = { Text("DoH URL") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            Panel {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Сеть", color = Brass, style = MaterialTheme.typography.titleLarge)
                    SwitchRow("IPv6", settings.ipv6, onIpv6)
                    SwitchRow("Allow LAN", settings.allowLan, onAllowLan)
                    Text("Keepalive: ${settings.keepalive}s", color = Mist)
                    Slider(
                        value = settings.keepalive.toFloat(),
                        onValueChange = { onKeepalive(it.toInt()) },
                        valueRange = 0f..60f
                    )
                }
            }

            Panel {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Пинг", color = Brass, style = MaterialTheme.typography.titleLarge)
                    SwitchRow("Авто-пинг после обновления", settings.autoPingAfterRefresh, onAutoPing)
                    Text("Таймаут: ${settings.pingTimeoutMs} ms", color = Mist)
                    Slider(
                        value = settings.pingTimeoutMs.toFloat(),
                        onValueChange = { onPingTimeout(it.toInt()) },
                        valueRange = 500f..10000f
                    )
                    Text(
                        "Нет ответа → сервер удаляется из списка.",
                        color = MistDim,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}

@Composable
private fun SwitchRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = Mist)
        Switch(
            checked = checked,
            onCheckedChange = onChange,
            colors = SwitchDefaults.colors(checkedTrackColor = Brass)
        )
    }
}

@Composable
private fun DnsChip(label: String, selected: Boolean, onClick: () -> Unit) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label) },
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = Brass,
            selectedLabelColor = Ink
        )
    )
}
