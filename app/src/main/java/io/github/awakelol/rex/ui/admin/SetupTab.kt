package io.github.awakelol.rex.ui.admin

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.awakelol.rex.R
import io.github.awakelol.rex.ui.Protection
import io.github.awakelol.rex.ui.rememberProtectionStatus
import io.github.awakelol.rex.ui.theme.DangerRed
import io.github.awakelol.rex.ui.theme.SafeGreen
import io.github.awakelol.rex.watch.GuardService

@Composable
fun SetupTab() {
    val context = LocalContext.current
    val status = rememberProtectionStatus()

    // Once refused twice, Android stops showing the prompt, so fall back to the settings page.
    val askNotifications = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (!granted) Protection.openNotificationSettings(context)
    }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Item(
            title = stringResource(R.string.setup_service),
            detail = stringResource(R.string.setup_service_detail),
            done = status.serviceRunning,
            action = stringResource(R.string.setup_start),
        ) { GuardService.start(context) }

        Item(
            title = stringResource(R.string.setup_notifications),
            detail = stringResource(R.string.setup_notifications_detail),
            done = status.notificationsAllowed,
        ) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                askNotifications.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                Protection.openNotificationSettings(context)
            }
        }

        Item(
            title = stringResource(R.string.setup_access),
            detail = stringResource(R.string.setup_access_detail),
            done = status.notificationAccess,
        ) { Protection.openNotificationAccess(context) }

        Item(
            title = stringResource(R.string.setup_battery),
            detail = stringResource(R.string.setup_battery_detail),
            done = status.batteryUnrestricted,
        ) { Protection.openBatterySettings(context) }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            Item(
                title = stringResource(R.string.setup_fullscreen),
                detail = stringResource(R.string.setup_fullscreen_detail),
                done = status.fullScreenAllowed,
            ) { Protection.openFullScreenSettings(context) }
        }

        Item(
            title = stringResource(R.string.setup_overlay),
            detail = stringResource(R.string.setup_overlay_detail),
            done = status.overlayAllowed,
        ) { Protection.openOverlaySettings(context) }

        Item(
            title = stringResource(R.string.setup_samsung),
            detail = stringResource(R.string.setup_samsung_detail),
            done = null,
            action = stringResource(R.string.setup_app_info),
        ) { Protection.openAppInfo(context) }
    }
}

@Composable
private fun Item(
    title: String,
    detail: String,
    done: Boolean?,
    action: String = stringResource(R.string.setup_open),
    onClick: () -> Unit,
) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                when (done) {
                    true -> Text(stringResource(R.string.setup_done), color = SafeGreen, fontWeight = FontWeight.Bold)
                    false -> Text(stringResource(R.string.setup_not_done), color = DangerRed, fontWeight = FontWeight.Bold)
                    null -> Unit
                }
            }
            Text(detail, style = MaterialTheme.typography.bodyMedium)
            if (done != true) {
                FilledTonalButton(onClick = onClick) { Text(action) }
            }
        }
    }
}
