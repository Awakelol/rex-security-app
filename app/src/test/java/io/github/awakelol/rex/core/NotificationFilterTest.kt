package io.github.awakelol.rex.core

import org.junit.Assert.assertEquals
import org.junit.Test

class NotificationFilterTest {

    private val own = "io.github.awakelol.rex"
    private val pending = setOf("com.shady.cleaner")

    private fun decide(pkg: String, title: String?, text: String?) =
        NotificationFilter.decide(pkg, title, text, pending, own)

    @Test
    fun `pending app is always cancelled`() {
        assertEquals(Decision.CANCEL, decide("com.shady.cleaner", "Hello", "Nothing scary here"))
        assertEquals(Decision.CANCEL, decide("com.shady.cleaner", null, null))
    }

    @Test
    fun `chat message mentioning a virus from an approved app is kept`() {
        val result = decide("com.whatsapp", "Mum", "Stay home, there's a nasty virus going around")
        assertEquals(Decision.KEEP, result)
    }

    @Test
    fun `scareware from a browser is cancelled`() {
        assertEquals(
            Decision.CANCEL,
            decide("com.android.chrome", "WARNING", "Your phone is INFECTED with 4 viruses! Clean now"),
        )
        assertEquals(
            Decision.CANCEL,
            decide("com.sec.android.app.sbrowser", "Battery is   damaged", null),
        )
        assertEquals(
            Decision.CANCEL,
            decide("org.mozilla.firefox", null, "Storage full. Tap to clean"),
        )
    }

    @Test
    fun `normal browser notification is kept`() {
        assertEquals(Decision.KEEP, decide("com.android.chrome", "BBC News", "Rain expected this weekend"))
    }

    @Test
    fun `own notifications are kept even if they look scary`() {
        assertEquals(Decision.KEEP, decide(own, "Harmful app", "Possible malware installed"))
    }

    @Test
    fun `system app notification is kept`() {
        assertEquals(Decision.KEEP, decide("com.android.systemui", "Storage full", "Free up space"))
    }
}
