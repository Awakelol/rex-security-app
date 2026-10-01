package io.github.awakelol.rex.core

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

object AlertText {
    private const val DISCORD_LIMIT = 2000
    private val timeFormat = DateTimeFormatter.ofPattern("EEE d MMM yyyy, HH:mm")

    fun newApp(
        label: String,
        packageName: String,
        installer: String?,
        report: RiskReport,
        time: Instant,
        zone: ZoneId = ZoneId.systemDefault(),
    ): String {
        val icon = when (report.level) {
            RiskLevel.HIGH -> "🔴"
            RiskLevel.MEDIUM -> "🟠"
            RiskLevel.LOW -> "🟢"
        }
        val text = buildString {
            appendLine("$icon **New app installed: $label**")
            appendLine("Package: `$packageName`")
            appendLine("Installed from: ${installerName(installer)}")
            appendLine("Risk: **${report.level}**")
            if (report.reasons.isNotEmpty()) {
                appendLine("Why:")
                report.reasons.forEach { appendLine("- $it") }
            }
            append("Time: ${timeFormat.format(time.atZone(zone))}")
        }
        return text.take(DISCORD_LIMIT)
    }

    fun removed(label: String, packageName: String, time: Instant, zone: ZoneId = ZoneId.systemDefault()) =
        "✅ Removed: **$label** (`$packageName`) at ${timeFormat.format(time.atZone(zone))}"

    fun installerName(installer: String?): String = when (installer) {
        null -> "unknown (sideloaded or adb)"
        in Rules.trustedInstallers -> Rules.trustedInstallers.getValue(installer)
        else -> "`$installer`"
    }
}
