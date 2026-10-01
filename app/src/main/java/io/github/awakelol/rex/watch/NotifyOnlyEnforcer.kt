package io.github.awakelol.rex.watch

import android.content.Context
import android.provider.Settings
import android.util.Log
import io.github.awakelol.rex.alert.AlertWorker
import io.github.awakelol.rex.core.AlertText
import io.github.awakelol.rex.core.RiskLevel
import io.github.awakelol.rex.core.RiskReport
import io.github.awakelol.rex.data.PendingApp
import io.github.awakelol.rex.notify.Notifications
import io.github.awakelol.rex.warning.WarningActivity
import java.time.Instant

/** Warns and reports, but never touches the app itself. */
class NotifyOnlyEnforcer(
    private val context: Context,
    private val notifications: Notifications,
    private val requestSweep: () -> Unit,
) : Enforcer {

    override suspend fun onNewPackage(app: PendingApp) {
        AlertWorker.enqueue(
            context,
            AlertText.newApp(
                label = app.label,
                packageName = app.packageName,
                installer = app.installer,
                report = RiskReport(app.level, app.reasons),
                time = Instant.ofEpochMilli(app.detectedAt),
            ),
        )
        // Its notifications may already be up by the time we get here.
        requestSweep()

        if (app.level == RiskLevel.LOW) {
            notifications.showNewApp(app)
        } else {
            warn(app)
        }
    }

    override suspend fun onPendingPackageStillPresent(app: PendingApp) = warn(app)

    override suspend fun onPendingPackageRemoved(app: PendingApp) {
        notifications.cancelFor(app.packageName)
        AlertWorker.enqueue(context, AlertText.removed(app.label, app.packageName, Instant.now()))
    }

    override suspend fun onPackageApproved(app: PendingApp) {
        notifications.cancelFor(app.packageName)
    }

    private fun warn(app: PendingApp) {
        notifications.showWarning(app)
        // Holding "display over other apps" is one of the few ways Android still lets
        // a background app open a screen directly.
        if (Settings.canDrawOverlays(context)) {
            try {
                context.startActivity(WarningActivity.intent(context, app.packageName))
            } catch (e: Exception) {
                Log.w("Rex", "Couldn't open warning screen directly", e)
            }
        }
    }
}
