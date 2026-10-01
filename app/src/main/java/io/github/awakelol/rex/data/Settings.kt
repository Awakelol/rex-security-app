package io.github.awakelol.rex.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import io.github.awakelol.rex.core.PinHasher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.IOException
import java.util.Base64

sealed interface PinCheck {
    data object Ok : PinCheck
    data class Wrong(val triesLeft: Int) : PinCheck
    data class LockedOut(val until: Long) : PinCheck
}

class Settings(private val store: DataStore<Preferences>) {

    private val prefs: Flow<Preferences> = store.data.catch { e ->
        if (e is IOException) emit(emptyPreferences()) else throw e
    }

    val approved: Flow<Set<String>> = prefs.map { it[APPROVED].orEmpty() }
    val baselineDone: Flow<Boolean> = prefs.map { it[BASELINE_DONE] ?: false }
    val webhookUrl: Flow<String> = prefs.map { it[WEBHOOK].orEmpty() }
    val helperName: Flow<String> = prefs.map { it[HELPER_NAME].orEmpty() }
    val helperPhone: Flow<String> = prefs.map { it[HELPER_PHONE].orEmpty() }
    val hasPin: Flow<Boolean> = prefs.map { it[PIN_HASH] != null }

    suspend fun saveBaseline(packages: Set<String>) {
        store.edit {
            it[APPROVED] = packages
            it[BASELINE_DONE] = true
        }
    }

    suspend fun approve(pkg: String) {
        store.edit { it[APPROVED] = it[APPROVED].orEmpty() + pkg }
    }

    suspend fun unapprove(packages: Set<String>) {
        store.edit { it[APPROVED] = it[APPROVED].orEmpty() - packages }
    }

    suspend fun setWebhookUrl(url: String) = store.edit { it[WEBHOOK] = url.trim() }

    suspend fun setHelper(name: String, phone: String) = store.edit {
        it[HELPER_NAME] = name.trim()
        it[HELPER_PHONE] = phone.trim()
    }

    suspend fun setPin(pin: String) {
        require(PinHasher.isValidPin(pin))
        val salt = PinHasher.newSalt()
        val hash = withContext(Dispatchers.Default) { PinHasher.hash(pin, salt) }
        store.edit {
            it[PIN_SALT] = b64.encodeToString(salt)
            it[PIN_HASH] = b64.encodeToString(hash)
            it[PIN_FAILURES] = 0
            it.remove(PIN_LOCKED_UNTIL)
        }
    }

    suspend fun checkPin(pin: String, now: Long = System.currentTimeMillis()): PinCheck {
        val p = prefs.first()
        val lockedUntil = p[PIN_LOCKED_UNTIL] ?: 0L
        if (now < lockedUntil) return PinCheck.LockedOut(lockedUntil)

        val salt = p[PIN_SALT]?.let(b64d::decode)
        val hash = p[PIN_HASH]?.let(b64d::decode)
        if (salt == null || hash == null) return PinCheck.Ok

        val ok = withContext(Dispatchers.Default) { PinHasher.matches(pin, salt, hash) }
        if (ok) {
            store.edit { it[PIN_FAILURES] = 0 }
            return PinCheck.Ok
        }

        var result: PinCheck = PinCheck.Wrong(0)
        store.edit {
            val failures = (it[PIN_FAILURES] ?: 0) + 1
            result = if (failures >= MAX_TRIES) {
                it[PIN_FAILURES] = 0
                it[PIN_LOCKED_UNTIL] = now + LOCKOUT_MS
                PinCheck.LockedOut(now + LOCKOUT_MS)
            } else {
                it[PIN_FAILURES] = failures
                PinCheck.Wrong(MAX_TRIES - failures)
            }
        }
        return result
    }

    private companion object {
        const val MAX_TRIES = 5
        const val LOCKOUT_MS = 30_000L

        val b64: Base64.Encoder = Base64.getEncoder()
        val b64d: Base64.Decoder = Base64.getDecoder()

        val APPROVED = stringSetPreferencesKey("approved")
        val BASELINE_DONE = booleanPreferencesKey("baseline_done")
        val WEBHOOK = stringPreferencesKey("webhook_url")
        val HELPER_NAME = stringPreferencesKey("helper_name")
        val HELPER_PHONE = stringPreferencesKey("helper_phone")
        val PIN_SALT = stringPreferencesKey("pin_salt")
        val PIN_HASH = stringPreferencesKey("pin_hash")
        val PIN_FAILURES = intPreferencesKey("pin_failures")
        val PIN_LOCKED_UNTIL = longPreferencesKey("pin_locked_until")
    }
}
