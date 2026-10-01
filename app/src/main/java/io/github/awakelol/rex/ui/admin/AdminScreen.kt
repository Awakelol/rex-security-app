package io.github.awakelol.rex.ui.admin

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.awakelol.rex.R
import io.github.awakelol.rex.container

@Composable
fun AdminScreen(onLock: () -> Unit) {
    val c = LocalContext.current.container
    val pending by remember { c.db.pending().observeAll() }.collectAsState(initial = emptyList())
    var tab by rememberSaveable { mutableIntStateOf(0) }

    val titles = listOf(
        stringResource(R.string.tab_pending, pending.size),
        stringResource(R.string.tab_setup),
        stringResource(R.string.tab_settings),
        stringResource(R.string.tab_log),
    )

    Column(Modifier.fillMaxSize().safeDrawingPadding()) {
        Row(
            Modifier.fillMaxWidth().padding(start = 16.dp, end = 8.dp, top = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                stringResource(R.string.admin_title),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.weight(1f),
            )
            TextButton(onClick = onLock) { Text(stringResource(R.string.admin_lock)) }
        }
        PrimaryTabRow(selectedTabIndex = tab) {
            titles.forEachIndexed { i, title ->
                Tab(selected = tab == i, onClick = { tab = i }, text = { Text(title, maxLines = 1) })
            }
        }
        when (tab) {
            0 -> PendingTab(pending)
            1 -> SetupTab()
            2 -> SettingsTab()
            else -> LogTab()
        }
    }
}
