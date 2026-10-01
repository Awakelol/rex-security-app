package io.github.awakelol.rex.alert

import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

object Discord {

    sealed interface Result {
        data object Sent : Result
        data class TryLater(val why: String) : Result
        data class Failed(val why: String) : Result
    }

    /** Blocking; call off the main thread. */
    fun post(webhookUrl: String, content: String): Result {
        if (!webhookUrl.startsWith("https://")) return Result.Failed("Webhook URL must start with https://")

        val body = JSONObject()
            .put("content", content)
            // App names come from whoever wrote the app; don't let one ping @everyone.
            .put("allowed_mentions", JSONObject().put("parse", JSONArray()))
            .toString()
            .toByteArray()

        val conn = try {
            URL(webhookUrl).openConnection() as HttpURLConnection
        } catch (e: Exception) {
            return Result.Failed("Bad webhook URL")
        }
        return try {
            conn.requestMethod = "POST"
            conn.connectTimeout = 15_000
            conn.readTimeout = 15_000
            conn.doOutput = true
            conn.setRequestProperty("Content-Type", "application/json; charset=utf-8")
            conn.setRequestProperty("User-Agent", "Rex (Android)")
            conn.outputStream.use { it.write(body) }

            when (val code = conn.responseCode) {
                in 200..299 -> Result.Sent
                429, in 500..599 -> Result.TryLater("HTTP $code")
                else -> Result.Failed("HTTP $code")
            }
        } catch (e: IOException) {
            Result.TryLater(e.javaClass.simpleName)
        } finally {
            conn.disconnect()
        }
    }
}
