package com.tourverse.security

import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

object PasswordHasher {
    private const val ITERATIONS = 120_000
    private const val KEY_LENGTH = 256
    private val random = SecureRandom()

    fun hash(password: String): String {
        val salt = ByteArray(16).also(random::nextBytes)
        val hash = derive(password, salt, ITERATIONS)
        return listOf("pbkdf2", ITERATIONS.toString(), encode(salt), encode(hash)).joinToString("$")
    }

    fun verify(password: String, storedHash: String): Boolean = runCatching {
        val parts = storedHash.split('$')
        if (parts.size != 4 || parts[0] != "pbkdf2") return false
        val iterations = parts[1].toInt()
        val salt = Base64.getDecoder().decode(parts[2])
        val expected = Base64.getDecoder().decode(parts[3])
        MessageDigest.isEqual(expected, derive(password, salt, iterations))
    }.getOrDefault(false)

    private fun derive(password: String, salt: ByteArray, iterations: Int): ByteArray {
        val specification = PBEKeySpec(password.toCharArray(), salt, iterations, KEY_LENGTH)
        return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
            .generateSecret(specification)
            .encoded
    }

    private fun encode(value: ByteArray): String =
        Base64.getEncoder().withoutPadding().encodeToString(value)
}
