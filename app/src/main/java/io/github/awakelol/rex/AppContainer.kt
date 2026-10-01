package io.github.awakelol.rex

import android.content.Context
import androidx.datastore.preferences.preferencesDataStore
import io.github.awakelol.rex.data.RexDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

private val Context.prefs by preferencesDataStore(name = "rex")

// Hand-wired dependencies. Small enough that a DI framework isn't worth it.
class AppContainer(context: Context) {
    private val appContext = context.applicationContext

    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    val prefs get() = appContext.prefs

    val db: RexDatabase by lazy { RexDatabase.build(appContext) }
}
