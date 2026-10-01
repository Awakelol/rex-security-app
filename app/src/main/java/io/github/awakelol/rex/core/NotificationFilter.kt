package io.github.awakelol.rex.core

enum class Decision { KEEP, CANCEL }

object NotificationFilter {

    fun decide(
        packageName: String,
        title: CharSequence?,
        text: CharSequence?,
        pending: Set<String>,
        ownPackage: String,
    ): Decision {
        if (packageName == ownPackage) return Decision.KEEP
        if (packageName in pending) return Decision.CANCEL

        // Browsers come preinstalled on most phones, so this has to run before
        // anything that trusts system apps.
        if (packageName in Rules.browsers && looksLikeScareware(title, text)) {
            return Decision.CANCEL
        }
        return Decision.KEEP
    }

    fun looksLikeScareware(title: CharSequence?, text: CharSequence?): Boolean {
        val haystack = listOfNotNull(title, text)
            .joinToString(" ")
            .lowercase()
            .replace(Regex("\\s+"), " ")
        return Rules.scarewarePhrases.any { it in haystack }
    }
}
