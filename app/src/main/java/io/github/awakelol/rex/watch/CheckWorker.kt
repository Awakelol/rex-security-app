package io.github.awakelol.rex.watch

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import io.github.awakelol.rex.container
import java.util.concurrent.TimeUnit

/** Safety net in case the service got killed: re-checks installed apps every 15 minutes. */
class CheckWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        if (!GuardService.running.value) GuardService.start(applicationContext)
        val c = applicationContext.container
        c.watcher.reconcile()
        c.db.log().trim()
        return Result.success()
    }

    companion object {
        fun schedule(context: Context) {
            // 15 minutes is the shortest interval WorkManager allows.
            val request = PeriodicWorkRequestBuilder<CheckWorker>(15, TimeUnit.MINUTES).build()
            WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork("check", ExistingPeriodicWorkPolicy.KEEP, request)
        }
    }
}
