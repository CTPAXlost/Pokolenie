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
import androidx.compose.ui.Modifier as ComposeModifier
import androidx.compose.ui.unit.dp
import ru.pokolenie.app.data.db.WarpProfileEntity
import ru.pokolenie.app.presentation.components.Panel
import ru.pokolenie.app.presentation.components.PrimaryButton
import ru.pokolenie.app.presentation.components.ScreenScaffold
import ru.pokolenie.app.presentation.components.TinyAction
import ru.pokolenie.app.presentation.theme.Brass
import ru.pokolenie.app.presentation.theme.Mist
import ru.pokolenie.app.presentation.theme.MistDim

@Composable
fun WarpScreen(
    profiles: List<WarpProfileEntity>,
    busy: Boolean,
    onGenerate: () -> Unit,
    onSelect: (Long) -> Unit,
    onCopy: (WarpProfileEntity) -> Unit,
    onDelete: (Long) -> Unit,
    onConnect: () -> Unit
) {
    ScreenScaffold(
        title = "Warp",
        subtitle = "Генерация через Cloudflare API + Amnezia-пресет"
    ) {
        PrimaryButton(
            text = if (busy) "Генерация…" else "Сгенерировать Warp",
            onClick = onGenerate,
            enabled = !busy
        )
        if (profiles.isNotEmpty()) {
            PrimaryButton(text = "Подключить выбранный Warp", onClick = onConnect, enabled = !busy)
        }

        LazyColumn(
            modifier = ComposeModifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(profiles, key = { it.id }) { profile ->
                Panel(modifier = ComposeModifier.clickable { onSelect(profile.id) }) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            profile.name,
                            color = if (profile.isSelected) Brass else Mist,
                            style = MaterialTheme.typography.titleLarge
                        )
                        Text(
                            "${profile.endpointHost}:${profile.endpointPort}",
                            color = MistDim,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(profile.addressV4, color = MistDim, style = MaterialTheme.typography.bodyMedium)
                        if (profile.isSelected) {
                            Text("выбран", color = Brass)
                        }
                        Row(modifier = ComposeModifier.fillMaxWidth()) {
                            TinyAction("Копировать .conf") { onCopy(profile) }
                            TinyAction("Удалить") { onDelete(profile.id) }
                        }
                    }
                }
            }
        }
    }
}
