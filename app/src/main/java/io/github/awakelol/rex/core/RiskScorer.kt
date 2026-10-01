package io.github.awakelol.rex.core

object RiskScorer {

    /**
     * @param approvedApps package name -> label for every approved app, used to spot clones.
     */
    fun score(facts: PackageFacts, approvedApps: Map<String, String>): RiskReport {
        val hits = mutableListOf<Pair<Signal, String>>()

        if (facts.installer !in Rules.trustedInstallers) {
            val from = facts.installer ?: "an unknown source"
            hits += Signal.UNTRUSTED_SOURCE to "Not installed from the Play Store or Galaxy Store (came from $from)."
        }
        if (facts.drawsOverApps) {
            hits += Signal.DRAWS_OVER_APPS to "Asks to draw over other apps."
        }
        if (facts.hasAccessibilityService) {
            hits += Signal.ACCESSIBILITY_SERVICE to "Includes an accessibility service, which can read and tap the screen."
        }
        if (facts.hasDeviceAdmin) {
            hits += Signal.DEVICE_ADMIN to "Can ask to become a device admin, which makes it hard to uninstall."
        }
        if (facts.isHomeApp) {
            hits += Signal.HOME_SCREEN to "Can replace the home screen."
        }
        if (facts.installsApps) {
            hits += Signal.INSTALLS_APPS to "Can install other apps."
        }
        if (facts.readsNotifications) {
            hits += Signal.READS_NOTIFICATIONS to "Can read notifications."
        }

        val name = normalize(facts.label)
        if (name.isNotEmpty()) {
            val original = approvedApps.entries.firstOrNull { (pkg, label) ->
                pkg != facts.packageName && normalize(label) == name
            }
            if (original != null) {
                hits += Signal.CLONE_NAME to
                    "Has the same name as \"${original.value}\", which is already installed."
            }
        }

        val first = facts.label.trimStart().firstOrNull()
        if (first != null && !first.isLetterOrDigit()) {
            hits += Signal.ODD_NAME to "Name starts with a symbol (\"$first\")."
        }

        val lower = facts.label.lowercase()
        val bait = Rules.baitWords.firstOrNull { it in lower }
        if (bait != null) {
            hits += Signal.BAIT_NAME to "Name sounds like a cleaner or virus scanner (\"$bait\")."
        }

        val weights = hits.map { Rules.weights.getValue(it.first) }
        val level = when {
            Weight.HIGH in weights -> RiskLevel.HIGH
            weights.count { it == Weight.MEDIUM } >= 2 -> RiskLevel.HIGH
            Weight.MEDIUM in weights -> RiskLevel.MEDIUM
            else -> RiskLevel.LOW
        }
        return RiskReport(level, hits.map { it.second })
    }

    /** "#WhatsApp", "whats app" and "WhatsApp™" all become "whatsapp". */
    fun normalize(label: String): String =
        label.filter { it.isLetterOrDigit() }.lowercase()
}
