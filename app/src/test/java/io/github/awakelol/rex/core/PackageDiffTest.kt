package io.github.awakelol.rex.core

import org.junit.Assert.assertEquals
import org.junit.Test

class PackageDiffTest {

    @Test
    fun `new package is added, updated approved package is not`() {
        val diff = PackageDiff.between(
            installed = setOf("a", "b", "new"),
            approved = setOf("a", "b"),
            pending = emptySet(),
        )
        assertEquals(setOf("new"), diff.added)
        assertEquals(emptySet<String>(), diff.removedPending)
    }

    @Test
    fun `pending package is not re-added`() {
        val diff = PackageDiff.between(setOf("a", "p"), setOf("a"), setOf("p"))
        assertEquals(emptySet<String>(), diff.added)
    }

    @Test
    fun `uninstalled packages are reported`() {
        val diff = PackageDiff.between(setOf("a"), setOf("a", "gone"), setOf("p"))
        assertEquals(setOf("p"), diff.removedPending)
        assertEquals(setOf("gone"), diff.removedApproved)
    }
}
