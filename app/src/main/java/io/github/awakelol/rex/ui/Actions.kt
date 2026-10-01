package io.github.awakelol.rex.ui

import android.app.Activity
import android.app.KeyguardManager
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri

fun Activity.uninstall(pkg: String) = afterUnlock {
    // Hands off to the system's own "Do you want to uninstall?" dialog.
    startSafely(Intent(Intent.ACTION_DELETE, Uri.fromParts("package", pkg, null)))
}

fun Activity.dial(phone: String) = afterUnlock {
    startSafely(Intent(Intent.ACTION_DIAL, Uri.fromParts("tel", phone, null)))
}

fun Context.startSafely(intent: Intent): Boolean = try {
    startActivity(intent)
    true
} catch (_: ActivityNotFoundException) {
    false
}

/** The warning can show on the lock screen; uninstall and dial need the phone unlocked first. */
private fun Activity.afterUnlock(block: () -> Unit) {
    val km = getSystemService(KeyguardManager::class.java)
    if (!km.isKeyguardLocked) {
        block()
        return
    }
    km.requestDismissKeyguard(this, object : KeyguardManager.KeyguardDismissCallback() {
        override fun onDismissSucceeded() = block()
    })
}
