package ru.pokolenie.app.presentation.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ru.pokolenie.app.data.db.WarpProfileEntity
import ru.pokolenie.app.presentation.components.Panel
import ru.pokolenie.app.presentation.components.PrimaryButton
import ru.pokolenie.app.presentation.components.ScreenScaffold
import ru.pokolenie.app.presentation.components.TinyAction
import ru.pokolenie.app.presentation.theme.Brass
import ru.pokolenie.app.presentation.theme.Mist
import ru.pokolenie.app.presentation.theme.MistDim
import ru.pokolenie.app.presentation.theme.SignalGreen

@Composable
fun WarpScreen(
    profiles: List<WarpProfileEntity>,
    busy: Boolean,
    onGenerate: () -> Unit,
    onSelect: (Long) -> Unit,
    onCopy: (WarpProfileEntity) -> Unit,
    onDelete: (Long) -> Unit,
    onConnect: () -> Unit,
    onPing: (WarpProfileEntity) -> Unit,
    onPingAll: () -> Unit
) {
    ScreenScaffold(
        title = "Warp",
        subtitle = "WARP_STR* · пинг = TCP до endpoint (не через туннель)"
    ) {
        PrimaryButton(
            text = if (busy) "Генерация…" else "Сгенерировать Warp",
            onClick = onGenerate,
            enabled = !busy
        )
        if (profiles.isNotEmpty()) {
            PrimaryButton(text = "Подключить выбранный Warp", onClick = onConnect, enabled = !busy)
            PrimaryButton(text = "Пинг всех Warp", onClick = onPingAll, enabled = !busy)
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(profiles, key = { it.id }) { profile ->
                Panel(modifier = Modifier.clickable { onSelect(profile.id) }) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                profile.name,
                                color = if (profile.isSelected) Brass else Mist,
                                style = MaterialTheme.typography.titleLarge,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                profile.latencyMs?.let { "$it ms" } ?: "—",
                                color = when {
                                    profile.latencyMs == null -> MistDim
                                    profile.latencyMs < 200 -> SignalGreen
                                    else -> Brass
                                }
                            )
                        }
                        Text(
                            "${profile.endpointHost}:${profile.endpointPort}",
                            color = MistDim,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(profile.addressV4, color = MistDim, style = MaterialTheme.typography.bodyMedium)
                        if (profile.isSelected) {
                            Text("выбран", color = Brass)
                        }
                        Row(modifier = Modifier.fillMaxWidth()) {
                            TinyAction("Пинг") { onPing(profile) }
                            TinyAction("Копировать .conf") { onCopy(profile) }
                            TinyAction("Удалить") { onDelete(profile.id) }
                        }
                    }
                }
            }
        }
    }
}
