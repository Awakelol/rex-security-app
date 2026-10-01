package io.github.awakelol.rex.watch

import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import io.github.awakelol.rex.container
import io.github.awakelol.rex.notify.Notifications
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Keeps Rex alive so it hears about installs as they happen.
 * Since Android 8, apps can't get package broadcasts through the manifest, only
 * through a receiver registered while the app is running, hence the foreground service.
 */
class GuardService : Service() {

    private val packageReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val pkg = intent.data?.schemeSpecificPart ?: return
            // An update sends REMOVED + ADDED with EXTRA_REPLACING, then REPLACED.
            val replacing = intent.getBooleanExtra(Intent.EXTRA_REPLACING, false)
            val watcher = container.watcher
            container.scope.launch {
                when (intent.action) {
                    Intent.ACTION_PACKAGE_ADDED -> if (!replacing) watcher.onAdded(pkg)
                    Intent.ACTION_PACKAGE_REPLACED -> watcher.onUpdated(pkg)
                    Intent.ACTION_PACKAGE_REMOVED -> if (!replacing) watcher.onRemoved(pkg)
                }
            }
        }
    }

    private val unlockReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            container.scope.launch { container.watcher.onUnlock() }
        }
    }

    override fun onCreate() {
        super.onCreate()
        val packages = IntentFilter().apply {
            addAction(Intent.ACTION_PACKAGE_ADDED)
            addAction(Intent.ACTION_PACKAGE_REPLACED)
            addAction(Intent.ACTION_PACKAGE_REMOVED)
            addDataScheme("package")
        }
        // System broadcasts still arrive with NOT_EXPORTED; it just keeps other apps from faking them.
        ContextCompat.registerReceiver(this, packageReceiver, packages, ContextCompat.RECEIVER_NOT_EXPORTED)
        ContextCompat.registerReceiver(
            this, unlockReceiver, IntentFilter(Intent.ACTION_USER_PRESENT), ContextCompat.RECEIVER_NOT_EXPORTED,
        )
        _running.value = true

        // Catch anything installed while we weren't running.
        container.scope.launch { container.watcher.reconcile() }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
        } else {
            0
        }
        ServiceCompat.startForeground(this, Notifications.ID_GUARD, container.notifications.guard(), type)
        return START_STICKY
    }

    override fun onDestroy() {
        unregisterReceiver(packageReceiver)
        unregisterReceiver(unlockReceiver)
        _running.value = false
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        private val _running = MutableStateFlow(false)
        val running: StateFlow<Boolean> = _running.asStateFlow()

        fun start(context: Context) {
            try {
                ContextCompat.startForegroundService(context, Intent(context, GuardService::class.java))
            } catch (e: IllegalStateException) {
                // Android 12+ refuses background starts unless the app is exempt
                // (battery optimisation off counts). The periodic check will try again.
                Log.w("Rex", "Not allowed to start guard service right now", e)
            }
        }
    }
}
