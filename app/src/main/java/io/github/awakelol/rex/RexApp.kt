package io.github.awakelol.rex

import android.app.Application
import android.content.Context
import io.github.awakelol.rex.watch.CheckWorker

class RexApp : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        container.notifications.createChannels()
        CheckWorker.schedule(this)
    }
}

val Context.container: AppContainer
    get() = (applicationContext as RexApp).container
