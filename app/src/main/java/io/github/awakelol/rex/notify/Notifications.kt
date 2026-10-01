package io.github.awakelol.rex.notify

import android.Manifest
import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import io.github.awakelol.rex.MainActivity
import io.github.awakelol.rex.R
import io.github.awakelol.rex.data.PendingApp
import io.github.awakelol.rex.warning.WarningActivity

class Notifications(private val context: Context) {
    private val nm = NotificationManagerCompat.from(context)

    fun createChannels() {
        val channels = listOf(
            NotificationChannel(CH_GUARD, context.getString(R.string.channel_guard), NotificationManager.IMPORTANCE_MIN)
                .apply { setShowBadge(false) },
            NotificationChannel(CH_WARNING, context.getString(R.string.channel_warning), NotificationManager.IMPORTANCE_HIGH),
            NotificationChannel(CH_INFO, context.getString(R.string.channel_info), NotificationManager.IMPORTANCE_DEFAULT),
        )
        context.getSystemService(NotificationManager::class.java).createNotificationChannels(channels)
    }

    fun guard(): Notification =
        NotificationCompat.Builder(context, CH_GUARD)
            .setSmallIcon(R.drawable.ic_stat_rex)
            .setContentTitle(context.getString(R.string.guard_notification))
            .setContentIntent(openApp())
            .setOngoing(true)
            .setSilent(true)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .build()

    fun canUseFullScreen(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE ||
            context.getSystemService(NotificationManager::class.java).canUseFullScreenIntent()

    @SuppressLint("MissingPermission")
    fun showWarning(app: PendingApp) {
        if (!canPost()) return
        val open = PendingIntent.getActivity(
            context,
            app.packageName.hashCode(),
            WarningActivity.intent(context, app.packageName),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val builder = NotificationCompat.Builder(context, CH_WARNING)
            .setSmallIcon(R.drawable.ic_stat_rex)
            .setContentTitle(context.getString(R.string.warning_notif_title, app.label))
            .setContentText(context.getString(R.string.warning_notif_text))
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setContentIntent(open)
            .setAutoCancel(true)
        // Without the full-screen grant this still shows as a heads-up notification.
        if (canUseFullScreen()) builder.setFullScreenIntent(open, true)
        nm.notify(app.packageName, ID_WARNING, builder.build())
    }

    @SuppressLint("MissingPermission")
    fun showNewApp(app: PendingApp) {
        if (!canPost()) return
        val n = NotificationCompat.Builder(context, CH_INFO)
            .setSmallIcon(R.drawable.ic_stat_rex)
            .setContentTitle(context.getString(R.string.info_notif_title, app.label))
            .setContentText(context.getString(R.string.info_notif_text))
            .setContentIntent(openApp())
            .setAutoCancel(true)
            .build()
        nm.notify(app.packageName, ID_INFO, n)
    }

    fun cancelFor(pkg: String) {
        nm.cancel(pkg, ID_WARNING)
        nm.cancel(pkg, ID_INFO)
    }

    fun canPost(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED

    private fun openApp() = PendingIntent.getActivity(
        context,
        0,
        Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        PendingIntent.FLAG_IMMUTABLE,
    )

    companion object {
        const val ID_GUARD = 1
        private const val ID_WARNING = 2
        private const val ID_INFO = 3

        private const val CH_GUARD = "guard"
        private const val CH_WARNING = "warning"
        private const val CH_INFO = "info"
    }
}
