package io.github.awakelol.rex.ui.admin

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.awakelol.rex.R
import io.github.awakelol.rex.container
import io.github.awakelol.rex.data.LogKind
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val timeFormat = DateTimeFormatter.ofPattern("d MMM, HH:mm")

@Composable
fun LogTab() {
    val c = LocalContext.current.container
    val entries by remember { c.db.log().recent() }.collectAsState(initial = emptyList())

    if (entries.isEmpty()) {
        Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
            Text(stringResource(R.string.log_empty))
        }
        return
    }

    val zone = ZoneId.systemDefault()
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items(entries, key = { it.id }) { e ->
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    "${timeFormat.format(Instant.ofEpochMilli(e.time).atZone(zone))} · ${kindLabel(e.kind)}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.secondary,
                )
                Text(e.text, style = MaterialTheme.typography.bodyMedium)
                HorizontalDivider(Modifier.padding(top = 6.dp))
            }
        }
    }
}

@Composable
private fun kindLabel(kind: LogKind) = stringResource(
    when (kind) {
        LogKind.INSTALLED -> R.string.log_installed
        LogKind.REMOVED -> R.string.log_removed
        LogKind.APPROVED -> R.string.log_approved
        LogKind.NOTIFICATION_BLOCKED -> R.string.log_blocked
        LogKind.INFO -> R.string.log_info
    },
)
