package io.github.awakelol.rex.alert

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import io.github.awakelol.rex.container
import io.github.awakelol.rex.data.LogEntry
import io.github.awakelol.rex.data.LogKind
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

/** Sends one Discord message. WorkManager holds it until there's a network and retries on failure. */
class AlertWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val content = inputData.getString(KEY_CONTENT) ?: return Result.failure()
        val c = applicationContext.container
        val url = c.settings.webhookUrl.first()
        if (url.isBlank()) {
            log("No Discord webhook set, alert not sent")
            return Result.success()
        }

        return when (val r = withContext(Dispatchers.IO) { Discord.post(url, content) }) {
            Discord.Result.Sent -> Result.success()
            is Discord.Result.TryLater -> if (runAttemptCount < MAX_ATTEMPTS) {
                Result.retry()
            } else {
                log("Gave up sending Discord alert (${r.why})")
                Result.failure()
            }
            is Discord.Result.Failed -> {
                log("Discord alert failed: ${r.why}")
                Result.failure()
            }
        }
    }

    private suspend fun log(text: String) {
        applicationContext.container.db.log()
            .insert(LogEntry(time = System.currentTimeMillis(), kind = LogKind.INFO, packageName = null, text = text))
    }

    companion object {
        private const val KEY_CONTENT = "content"
        private const val MAX_ATTEMPTS = 20

        fun enqueue(context: Context, content: String) {
            val request = OneTimeWorkRequestBuilder<AlertWorker>()
                .setInputData(workDataOf(KEY_CONTENT to content))
                .setConstraints(Constraints(requiredNetworkType = NetworkType.CONNECTED))
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
                .build()
            WorkManager.getInstance(context).enqueue(request)
        }
    }
}
