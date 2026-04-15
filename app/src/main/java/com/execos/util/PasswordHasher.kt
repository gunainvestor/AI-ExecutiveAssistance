package com.execos.util

import java.security.SecureRandom
import java.util.Base64
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

/**
 * PBKDF2 password hashing for local accounts.
 *
 * Stored format: pbkdf2_sha256$<iterations>$<salt_b64>$<hash_b64>
 */
object PasswordHasher {
    private const val ITERATIONS = 120_000
    private const val KEY_LENGTH_BITS = 256

    fun hashPassword(password: CharArray): String {
        val salt = ByteArray(16).also { SecureRandom().nextBytes(it) }
        val hash = pbkdf2(password, salt, ITERATIONS, KEY_LENGTH_BITS)
        return "pbkdf2_sha256$${ITERATIONS}$${b64(salt)}$${b64(hash)}"
    }

    fun verifyPassword(password: CharArray, stored: String): Boolean {
        val parts = stored.split("$")
        if (parts.size != 4) return false
        if (parts[0] != "pbkdf2_sha256") return false
        val iterations = parts[1].toIntOrNull() ?: return false
        val salt = b64d(parts[2])
        val expected = b64d(parts[3])
        val actual = pbkdf2(password, salt, iterations, expected.size * 8)
        return constantTimeEquals(actual, expected)
    }

    private fun pbkdf2(password: CharArray, salt: ByteArray, iterations: Int, keyLengthBits: Int): ByteArray {
        val spec = PBEKeySpec(password, salt, iterations, keyLengthBits)
        return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded
    }

    private fun constantTimeEquals(a: ByteArray, b: ByteArray): Boolean {
        if (a.size != b.size) return false
        var diff = 0
        for (i in a.indices) diff = diff or (a[i].toInt() xor b[i].toInt())
        return diff == 0
    }

    private fun b64(bytes: ByteArray): String = Base64.getEncoder().encodeToString(bytes)
    private fun b64d(s: String): ByteArray = Base64.getDecoder().decode(s)
}

