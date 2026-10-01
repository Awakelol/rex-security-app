package io.github.awakelol.rex

import android.content.Context
import androidx.datastore.preferences.preferencesDataStore
import io.github.awakelol.rex.data.RexDatabase
import io.github.awakelol.rex.data.Settings
import io.github.awakelol.rex.notify.Notifications
import io.github.awakelol.rex.watch.Enforcer
import io.github.awakelol.rex.watch.NotifyOnlyEnforcer
import io.github.awakelol.rex.watch.PackageInspector
import io.github.awakelol.rex.watch.PackageWatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow

private val Context.prefs by preferencesDataStore(name = "rex")

// Hand-wired dependencies. Small enough that a DI framework isn't worth it.
class AppContainer(context: Context) {
    private val appContext = context.applicationContext

    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    val settings = Settings(appContext.prefs)
    val db: RexDatabase by lazy { RexDatabase.build(appContext) }
    val inspector = PackageInspector(appContext)
    val notifications = Notifications(appContext)

    /** Asks the notification guard to re-check everything currently showing. */
    val sweepRequests = MutableSharedFlow<Unit>(extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)

    val enforcer: Enforcer = NotifyOnlyEnforcer(appContext, notifications) { sweepRequests.tryEmit(Unit) }

    val watcher by lazy {
        PackageWatcher(appContext.packageName, inspector, settings, db, enforcer)
    }

    /** True while the helper's PIN area is open. Reset whenever the app leaves the screen. */
    val adminUnlocked = MutableStateFlow(false)
}
