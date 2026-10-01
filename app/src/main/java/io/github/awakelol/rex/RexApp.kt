package io.github.awakelol.rex

import android.app.Application

class RexApp : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}

val android.content.Context.container: AppContainer
    get() = (applicationContext as RexApp).container
