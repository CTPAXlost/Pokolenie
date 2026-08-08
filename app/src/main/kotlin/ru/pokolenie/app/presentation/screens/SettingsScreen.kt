package ru.pokolenie.app.presentation.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import ru.pokolenie.app.presentation.components.Panel
import ru.pokolenie.app.presentation.components.ScreenScaffold
import ru.pokolenie.app.presentation.theme.Brass
import ru.pokolenie.app.presentation.theme.Ink
import ru.pokolenie.app.presentation.theme.Mist
import ru.pokolenie.app.presentation.theme.MistDim
import ru.pokolenie.app.settings.DnsMode
import ru.pokolenie.app.settings.SettingsState

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
    onPingTimeout: (Int) -> Unit,
    onWhitelist: (Boolean) -> Unit,
    onFakeIp: (Boolean) -> Unit,
    onFakeDns: (Boolean) -> Unit
) {
    ScreenScaffold(
        title = "Настройки",
        subtitle = "Whitelist, Fake IP/DNS, MTU и сеть"
    ) {
        Column(
            modifier = Modifier.verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Panel {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Маршрутизация", color = Brass, style = MaterialTheme.typography.titleLarge)
                    SwitchRow(
                        label = "White list",
                        checked = settings.whitelistEnabled,
                        onChange = onWhitelist,
                        hint = "ON — только белые домены через VPN. OFF — весь трафик через VPN."
                    )
                    SwitchRow(
                        label = "Fake IP",
                        checked = settings.fakeIpEnabled,
                        onChange = onFakeIp,
                        hint = "DNS отвечает фейковыми IP (198.18.0.0/15), меньше утечек SNI."
                    )
                    SwitchRow(
                        label = "Fake DNS",
                        checked = settings.fakeDnsEnabled,
                        onChange = onFakeDns,
                        hint = "DNS-запросы идут через прокси (hijack-dns)."
                    )
                }
            }

            Panel {
                var mtuText by remember(settings.mtu) { mutableStateOf(settings.mtu.toString()) }
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("MTU", color = Brass, style = MaterialTheme.typography.titleLarge)
                    OutlinedTextField(
                        value = mtuText,
                        onValueChange = { raw ->
                            val digits = raw.filter { it.isDigit() }.take(4)
                            mtuText = digits
                            digits.toIntOrNull()?.let { value ->
                                if (value in 576..1500) onMtu(value)
                            }
                        },
                        label = { Text("576–1500") },
                        supportingText = {
                            Text(
                                "Текущее: ${settings.mtu}. Для Warp обычно 1280.",
                                color = MistDim
                            )
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(1280, 1400, 1420, 1500).forEach { preset ->
                            DnsChip(preset.toString(), settings.mtu == preset) {
                                mtuText = preset.toString()
                                onMtu(preset)
                            }
                        }
                    }
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
                        "Пинг = TCP connect к хосту (не через туннель). Нет ответа → сервер удаляется.",
                        color = MistDim,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}

@Composable
private fun SwitchRow(
    label: String,
    checked: Boolean,
    onChange: (Boolean) -> Unit,
    hint: String? = null
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
            Text(label, color = Mist)
            if (hint != null) {
                Text(hint, color = MistDim, style = MaterialTheme.typography.bodyMedium)
            }
        }
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
