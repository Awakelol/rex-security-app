package io.github.awakelol.rex.watch

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/** Restarts protection after a reboot, and after Rex itself is updated with adb. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED, Intent.ACTION_MY_PACKAGE_REPLACED -> {
                GuardService.start(context)
                CheckWorker.schedule(context)
            }
        }
    }
}
