package io.github.awakelol.rex.core

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PinHasherTest {

    @Test
    fun `correct pin matches and wrong pin does not`() {
        val salt = PinHasher.newSalt()
        val stored = PinHasher.hash("4821", salt)
        assertTrue(PinHasher.matches("4821", salt, stored))
        assertFalse(PinHasher.matches("4822", salt, stored))
    }

    @Test
    fun `same pin with different salt gives different hash`() {
        val a = PinHasher.hash("1234", PinHasher.newSalt())
        val b = PinHasher.hash("1234", PinHasher.newSalt())
        assertFalse(a.contentEquals(b))
    }

    @Test
    fun `pin validation`() {
        assertTrue(PinHasher.isValidPin("0000"))
        assertTrue(PinHasher.isValidPin("12345678"))
        assertFalse(PinHasher.isValidPin("123"))
        assertFalse(PinHasher.isValidPin("123456789"))
        assertFalse(PinHasher.isValidPin("12a4"))
    }
}
