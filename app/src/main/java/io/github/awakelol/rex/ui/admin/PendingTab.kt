package io.github.awakelol.rex.ui.admin

import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import io.github.awakelol.rex.R
import io.github.awakelol.rex.container
import io.github.awakelol.rex.core.AlertText
import io.github.awakelol.rex.core.RiskLevel
import io.github.awakelol.rex.data.PendingApp
import io.github.awakelol.rex.ui.theme.DangerRed
import io.github.awakelol.rex.ui.theme.SafeGreen
import io.github.awakelol.rex.ui.theme.WarnAmber
import io.github.awakelol.rex.ui.uninstall
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun PendingTab(pending: List<PendingApp>) {
    val c = LocalContext.current.container
    val activity = LocalActivity.current
    val scope = rememberCoroutineScope()
    var confirming by remember { mutableStateOf<PendingApp?>(null) }

    if (pending.isEmpty()) {
        Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
            Text(stringResource(R.string.pending_empty), style = MaterialTheme.typography.bodyLarge)
        }
        return
    }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(pending, key = { it.packageName }) { app ->
            PendingCard(
                app = app,
                onApprove = { confirming = app },
                onRemove = { activity?.uninstall(app.packageName) },
            )
        }
    }

    confirming?.let { app ->
        AlertDialog(
            onDismissRequest = { confirming = null },
            title = { Text(stringResource(R.string.approve_title, app.label)) },
            text = { Text(stringResource(R.string.approve_text)) },
            confirmButton = {
                TextButton(onClick = {
                    confirming = null
                    scope.launch { c.watcher.approve(app.packageName) }
                }) { Text(stringResource(R.string.approve)) }
            },
            dismissButton = {
                TextButton(onClick = { confirming = null }) { Text(stringResource(R.string.cancel)) }
            },
        )
    }
}

@Composable
private fun PendingCard(app: PendingApp, onApprove: () -> Unit, onRemove: () -> Unit) {
    val c = LocalContext.current.container
    val icon by produceState<ImageBitmap?>(null, app.packageName) {
        value = withContext(Dispatchers.IO) {
            c.inspector.icon(app.packageName)?.toBitmap(96, 96)?.asImageBitmap()
        }
    }

    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                icon?.let { Image(it, contentDescription = null, modifier = Modifier.size(48.dp)) }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(app.label, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(app.packageName, style = MaterialTheme.typography.bodySmall)
                }
                LevelBadge(app.level)
            }
            Text(
                stringResource(R.string.pending_from, AlertText.installerName(app.installer).replace("`", "")),
                style = MaterialTheme.typography.bodyMedium,
            )
            app.reasons.forEach { Text("• $it", style = MaterialTheme.typography.bodyMedium) }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(onClick = onApprove, modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.approve))
                }
                Button(
                    onClick = onRemove,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = DangerRed, contentColor = Color.White),
                ) {
                    Text(stringResource(R.string.remove))
                }
            }
        }
    }
}

@Composable
private fun LevelBadge(level: RiskLevel) {
    val (bg, fg) = when (level) {
        RiskLevel.HIGH -> DangerRed to Color.White
        RiskLevel.MEDIUM -> WarnAmber to Color.Black
        RiskLevel.LOW -> SafeGreen to Color.White
    }
    Text(
        level.name,
        color = fg,
        fontWeight = FontWeight.Bold,
        style = MaterialTheme.typography.labelMedium,
        modifier = Modifier
            .background(bg, RoundedCornerShape(6.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp),
    )
}
