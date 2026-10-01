package io.github.awakelol.rex.core

/**
 * Everything tunable in one place: weights, trusted stores, keyword lists.
 * Matching is case-insensitive everywhere these are used.
 */
object Rules {

    val weights: Map<Signal, Weight> = mapOf(
        Signal.UNTRUSTED_SOURCE to Weight.HIGH,
        Signal.DRAWS_OVER_APPS to Weight.HIGH,
        Signal.ACCESSIBILITY_SERVICE to Weight.HIGH,
        Signal.DEVICE_ADMIN to Weight.HIGH,
        Signal.HOME_SCREEN to Weight.HIGH,
        Signal.CLONE_NAME to Weight.HIGH,
        Signal.INSTALLS_APPS to Weight.MEDIUM,
        Signal.READS_NOTIFICATIONS to Weight.MEDIUM,
        Signal.ODD_NAME to Weight.MEDIUM,
        Signal.BAIT_NAME to Weight.MEDIUM,
    )

    val trustedInstallers = mapOf(
        "com.android.vending" to "Google Play Store",
        "com.sec.android.app.samsungapps" to "Galaxy Store",
    )

    val baitWords = listOf(
        "cleaner", "clean master", "booster", "antivirus", "virus", "junk",
        "battery saver", "phone repair", "recover", "restore", "speed up",
    )

    val browsers = setOf(
        "com.android.chrome",
        "com.chrome.beta",
        "com.sec.android.app.sbrowser",
        "com.sec.android.app.sbrowser.beta",
        "org.mozilla.firefox",
        "org.mozilla.firefox_beta",
        "com.microsoft.emmx",
        "com.opera.browser",
        "com.opera.mini.native",
        "com.opera.gx",
        "com.brave.browser",
    )

    // Only applied to browser notifications (web push spam). Never to messaging apps.
    val scarewarePhrases = listOf(
        "virus", "infected", "malware", "trojan", "spyware", "hacked",
        "clean now", "clean your phone", "tap to clean", "remove now",
        "phone is damaged", "phone damaged", "battery is damaged", "battery damaged",
        "storage full", "storage is full", "storage almost full",
        "security alert", "security warning", "at risk",
    )
}
