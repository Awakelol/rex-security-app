package io.github.awakelol.rex.notify

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import io.github.awakelol.rex.container
import io.github.awakelol.rex.core.Decision
import io.github.awakelol.rex.core.NotificationFilter
import io.github.awakelol.rex.data.LogEntry
import io.github.awakelol.rex.data.LogKind
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

/**
 * Removes notifications from pending apps and scareware pushed through browsers.
 * Only runs once the user grants notification access in system settings.
 */
class NotificationGuard : NotificationListenerService() {

    private var sweeps: Job? = null

    override fun onListenerConnected() {
        val c = container
        sweeps = c.scope.launch {
            sweep()
            c.sweepRequests.collect { sweep() }
        }
    }

    override fun onListenerDisconnected() {
        sweeps?.cancel()
        sweeps = null
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        container.scope.launch { check(sbn, pending()) }
    }

    private suspend fun sweep() {
        val active = try {
            activeNotifications.orEmpty()
        } catch (e: SecurityException) {
            return  // not connected any more
        }
        val pending = pending()
        active.forEach { check(it, pending) }
    }

    private suspend fun pending() = container.db.pending().names().toSet()

    private suspend fun check(sbn: StatusBarNotification, pending: Set<String>) {
        val extras = sbn.notification.extras
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)
        val text = extras.getCharSequence(Notification.EXTRA_BIG_TEXT)
            ?: extras.getCharSequence(Notification.EXTRA_TEXT)

        val decision = NotificationFilter.decide(sbn.packageName, title, text, pending, packageName)
        if (decision == Decision.KEEP) return

        try {
            // Ongoing notifications (foreground services etc.) can't be cancelled by a listener.
            if (sbn.isClearable) cancelNotification(sbn.key) else snoozeNotification(sbn.key, SNOOZE_MS)
        } catch (e: Exception) {
            Log.w("Rex", "Couldn't remove notification from ${sbn.packageName}", e)
            return
        }

        val c = container
        val name = c.inspector.label(sbn.packageName) ?: sbn.packageName
        // Title only: never store message text.
        val shown = title?.toString()?.take(80)?.let { " \"$it\"" }.orEmpty()
        c.db.log().insert(
            LogEntry(
                time = System.currentTimeMillis(),
                kind = LogKind.NOTIFICATION_BLOCKED,
                packageName = sbn.packageName,
                text = "Blocked notification from $name$shown",
            ),
        )
    }

    private companion object {
        val SNOOZE_MS = TimeUnit.HOURS.toMillis(1)
    }
}
