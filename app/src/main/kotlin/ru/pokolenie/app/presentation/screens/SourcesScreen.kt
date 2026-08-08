package ru.pokolenie.app.presentation.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier as ComposeModifier
import androidx.compose.ui.unit.dp
import ru.pokolenie.app.data.db.SourceEntity
import ru.pokolenie.app.presentation.components.GhostButton
import ru.pokolenie.app.presentation.components.Panel
import ru.pokolenie.app.presentation.components.PrimaryButton
import ru.pokolenie.app.presentation.components.ScreenScaffold
import ru.pokolenie.app.presentation.components.TinyAction
import ru.pokolenie.app.presentation.theme.Brass
import ru.pokolenie.app.presentation.theme.Mist
import ru.pokolenie.app.presentation.theme.MistDim

@Composable
fun SourcesScreen(
    sources: List<SourceEntity>,
    busy: Boolean,
    onRefresh: () -> Unit,
    onToggle: (SourceEntity, Boolean) -> Unit,
    onDelete: (Long) -> Unit,
    onAdd: (String, String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var url by remember { mutableStateOf("") }

    ScreenScaffold(
        title = "Источники",
        subtitle = "Несколько GitHub raw-лент whitelist VLESS/Trojan",
        actions = {
            GhostButton("Обновить все", onClick = onRefresh, enabled = !busy)
        }
    ) {
        Panel {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Добавить источник", color = Brass, style = MaterialTheme.typography.titleLarge)
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Название") },
                    modifier = ComposeModifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = url,
                    onValueChange = { url = it },
                    label = { Text("URL (raw.githubusercontent.com/…)") },
                    modifier = ComposeModifier.fillMaxWidth(),
                    singleLine = true
                )
                PrimaryButton(
                    text = "Добавить",
                    onClick = {
                        if (name.isNotBlank() && url.isNotBlank()) {
                            onAdd(name.trim(), url.trim())
                            name = ""
                            url = ""
                        }
                    },
                    enabled = name.isNotBlank() && url.isNotBlank()
                )
            }
        }

        LazyColumn(
            modifier = ComposeModifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(sources, key = { it.id }) { source ->
                Panel {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            modifier = ComposeModifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(source.name, color = Mist, style = MaterialTheme.typography.titleLarge)
                            Switch(
                                checked = source.enabled,
                                onCheckedChange = { onToggle(source, it) },
                                colors = SwitchDefaults.colors(checkedTrackColor = Brass)
                            )
                        }
                        Text(source.url, color = MistDim, style = MaterialTheme.typography.bodyMedium)
                        source.lastError?.let {
                            Text("Ошибка: $it", color = MaterialTheme.colorScheme.error)
                        }
                        TinyAction("Удалить") { onDelete(source.id) }
                    }
                }
            }
        }
    }
}
