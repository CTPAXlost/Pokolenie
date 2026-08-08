package ru.pokolenie.app.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.modifier.Modifier
import androidx.compose.ui.unit.dp
import ru.pokolenie.app.settings.SplitMode
import ru.pokolenie.app.ui.components.Panel
import ru.pokolenie.app.ui.components.ScreenScaffold
import ru.pokolenie.app.ui.theme.Brass
import ru.pokolenie.app.ui.theme.Ink
import ru.pokolenie.app.ui.theme.Mist
import ru.pokolenie.app.ui.theme.MistDim
import ru.pokolenie.app.viewmodel.InstalledApp

@Composable
fun AppsScreen(
    apps: List<InstalledApp>,
    splitMode: SplitMode,
    selected: Set<String>,
    onLoad: () -> Unit,
    onMode: (SplitMode) -> Unit,
    onToggle: (String) -> Unit
) {
    LaunchedEffect(Unit) { onLoad() }

    ScreenScaffold(
        title = "Приложения",
        subtitle = "Раздельное туннелирование поверх обязательного whitelist"
    ) {
        Panel {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Режим", color = Brass, style = MaterialTheme.typography.titleLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ModeChip("Все", splitMode == SplitMode.ALL) { onMode(SplitMode.ALL) }
                    ModeChip("Только выбранные", splitMode == SplitMode.INCLUDE) { onMode(SplitMode.INCLUDE) }
                    ModeChip("Исключить", splitMode == SplitMode.EXCLUDE) { onMode(SplitMode.EXCLUDE) }
                }
                Text(
                    when (splitMode) {
                        SplitMode.ALL -> "VPN применяется ко всем приложениям (whitelist доменов всё равно действует)."
                        SplitMode.INCLUDE -> "В VPN попадают только отмеченные приложения."
                        SplitMode.EXCLUDE -> "Отмеченные приложения идут мимо VPN."
                    },
                    color = MistDim,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        if (apps.isEmpty()) {
            Panel { Text("Загрузка списка приложений…", color = MistDim) }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(apps, key = { it.packageName }) { app ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(enabled = splitMode != SplitMode.ALL) {
                                onToggle(app.packageName)
                            },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = selected.contains(app.packageName),
                            onCheckedChange = { onToggle(app.packageName) },
                            enabled = splitMode != SplitMode.ALL,
                            colors = CheckboxDefaults.colors(checkedColor = Brass)
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(app.label, color = Mist, style = MaterialTheme.typography.bodyLarge)
                            Text(app.packageName, color = MistDim, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ModeChip(label: String, selected: Boolean, onClick: () -> Unit) {
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
