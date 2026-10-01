package io.github.awakelol.rex.ui

import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import io.github.awakelol.rex.container
import io.github.awakelol.rex.notify.NotificationGuard
import io.github.awakelol.rex.watch.GuardService

data class ProtectionStatus(
    val serviceRunning: Boolean,
    val notificationsAllowed: Boolean,
    val notificationAccess: Boolean,
    val batteryUnrestricted: Boolean,
    val fullScreenAllowed: Boolean,
    val overlayAllowed: Boolean,
) {
    // Overlay is a nice-to-have, everything else is needed for Rex to do its job.
    val needsFixing: Boolean
        get() = !serviceRunning || !notificationsAllowed || !notificationAccess ||
            !batteryUnrestricted || !fullScreenAllowed
}

object Protection {

    fun check(context: Context, serviceRunning: Boolean) = ProtectionStatus(
        serviceRunning = serviceRunning,
        notificationsAllowed = context.container.notifications.canPost(),
        notificationAccess = context.packageName in NotificationManagerCompat.getEnabledListenerPackages(context),
        batteryUnrestricted = context.getSystemService(PowerManager::class.java)
            .isIgnoringBatteryOptimizations(context.packageName),
        fullScreenAllowed = context.container.notifications.canUseFullScreen(),
        overlayAllowed = Settings.canDrawOverlays(context),
    )

    fun openNotificationSettings(context: Context) {
        context.startSafely(
            Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName),
        )
    }

    fun openNotificationAccess(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val component = ComponentName(context, NotificationGuard::class.java).flattenToString()
            val detail = Intent(Settings.ACTION_NOTIFICATION_LISTENER_DETAIL_SETTINGS)
                .putExtra(Settings.EXTRA_NOTIFICATION_LISTENER_COMPONENT_NAME, component)
            if (context.startSafely(detail)) return
        }
        context.startSafely(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
    }

    @SuppressLint("BatteryLife")  // Play policy only; Rex is installed with adb
    fun openBatterySettings(context: Context) {
        val ask = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, packageUri(context))
        if (!context.startSafely(ask)) {
            context.startSafely(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
        }
    }

    fun openOverlaySettings(context: Context) {
        context.startSafely(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, packageUri(context)))
    }

    fun openFullScreenSettings(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            context.startSafely(Intent(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT, packageUri(context)))
        }
    }

    fun openAppInfo(context: Context) {
        context.startSafely(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, packageUri(context)))
    }

    private fun packageUri(context: Context) = Uri.fromParts("package", context.packageName, null)
}

/** Re-checked every time the screen comes back, e.g. after returning from system settings. */
@Composable
fun rememberProtectionStatus(): ProtectionStatus {
    val context = LocalContext.current
    val running by GuardService.running.collectAsState()
    var resumes by remember { mutableIntStateOf(0) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { resumes++ }
    return remember(running, resumes) { Protection.check(context, running) }
}
