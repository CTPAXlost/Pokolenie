package ru.pokolenie.app.presentation.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ru.pokolenie.app.data.db.ServerEntity
import ru.pokolenie.app.presentation.components.GhostButton
import ru.pokolenie.app.presentation.components.Panel
import ru.pokolenie.app.presentation.components.ScreenScaffold
import ru.pokolenie.app.presentation.components.TinyAction
import ru.pokolenie.app.presentation.theme.Brass
import ru.pokolenie.app.presentation.theme.Mist
import ru.pokolenie.app.presentation.theme.MistDim
import ru.pokolenie.app.presentation.theme.SignalGreen

@Composable
fun ServersScreen(
    servers: List<ServerEntity>,
    busy: Boolean,
    onSelect: (Long) -> Unit,
    onPing: (ServerEntity) -> Unit,
    onPingAll: () -> Unit,
    onDelete: (Long) -> Unit
) {
    ScreenScaffold(
        title = "Серверы",
        subtitle = "Нажми карточку — сервер выбран. Пинг без ответа удаляет узел.",
        actions = {
            GhostButton("Пинг всех", onClick = onPingAll, enabled = !busy)
        }
    ) {
        if (servers.isEmpty()) {
            Panel {
                Text(
                    "Список пуст. Откройте «Источники» и нажмите «Обновить все».",
                    color = MistDim
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(servers, key = { it.id }) { server ->
                    Panel(
                        modifier = Modifier.clickable { onSelect(server.id) }
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    server.name,
                                    style = MaterialTheme.typography.titleLarge,
                                    color = if (server.isSelected) Brass else Mist,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    server.latencyMs?.let { "$it ms" } ?: "—",
                                    color = if (server.latencyMs != null) SignalGreen else MistDim
                                )
                            }
                            Text(
                                "${server.protocol} · ${server.host}:${server.port}",
                                color = MistDim,
                                style = MaterialTheme.typography.bodyMedium
                            )
                            if (server.isSelected) {
                                Text("выбран", color = Brass, style = MaterialTheme.typography.labelLarge)
                            }
                            Row(
                                modifier = Modifier.padding(top = 4.dp),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                TinyAction("Пинг") { onPing(server) }
                                TinyAction("Удалить") { onDelete(server.id) }
                            }
                        }
                    }
                }
            }
        }
    }
}
