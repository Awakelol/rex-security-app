package io.github.awakelol.rex.core

enum class RiskLevel { LOW, MEDIUM, HIGH }

enum class Weight { MEDIUM, HIGH }

enum class Signal {
    UNTRUSTED_SOURCE,
    DRAWS_OVER_APPS,
    ACCESSIBILITY_SERVICE,
    DEVICE_ADMIN,
    HOME_SCREEN,
    CLONE_NAME,
    INSTALLS_APPS,
    READS_NOTIFICATIONS,
    ODD_NAME,
    BAIT_NAME,
}

data class PackageFacts(
    val packageName: String,
    val label: String,
    val installer: String?,
    val drawsOverApps: Boolean = false,
    val hasAccessibilityService: Boolean = false,
    val hasDeviceAdmin: Boolean = false,
    val isHomeApp: Boolean = false,
    val installsApps: Boolean = false,
    val readsNotifications: Boolean = false,
)

data class RiskReport(val level: RiskLevel, val reasons: List<String>)
