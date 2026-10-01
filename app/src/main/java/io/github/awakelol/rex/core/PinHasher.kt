package io.github.awakelol.rex.core

import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

object PinHasher {
    private const val ITERATIONS = 60_000
    private const val KEY_BITS = 256

    fun newSalt(): ByteArray = ByteArray(16).also { SecureRandom().nextBytes(it) }

    fun hash(pin: String, salt: ByteArray): ByteArray {
        val spec = PBEKeySpec(pin.toCharArray(), salt, ITERATIONS, KEY_BITS)
        try {
            return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded
        } finally {
            spec.clearPassword()
        }
    }

    fun matches(pin: String, salt: ByteArray, expected: ByteArray): Boolean =
        MessageDigest.isEqual(hash(pin, salt), expected)

    fun isValidPin(pin: String) = pin.length in 4..8 && pin.all { it in '0'..'9' }
}
