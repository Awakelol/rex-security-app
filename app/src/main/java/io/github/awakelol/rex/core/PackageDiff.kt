package io.github.awakelol.rex.core

data class PackageDiff(
    val added: Set<String>,
    val removedPending: Set<String>,
    val removedApproved: Set<String>,
) {
    companion object {
        /**
         * Anything installed that isn't approved or already pending is new.
         * Updates keep the same package name, so an updated approved app never shows up here.
         */
        fun between(installed: Set<String>, approved: Set<String>, pending: Set<String>) = PackageDiff(
            added = installed - approved - pending,
            removedPending = pending - installed,
            removedApproved = approved - installed,
        )
    }
}
