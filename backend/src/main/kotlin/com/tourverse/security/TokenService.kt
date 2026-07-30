package com.tourverse.security

import com.tourverse.utils.AppEnvironment
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.security.SecureRandom
import java.time.Instant
import java.util.Base64
import java.util.UUID
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

@Serializable
data class AccessTokenClaims(
    val userId: String,
    val role: String,
    val issuedAt: Long,
    val expiresAt: Long
)

data class IssuedAccessToken(val token: String, val expiresInSeconds: Long)

object TokenService {
    private const val DEFAULT_ACCESS_LIFETIME_SECONDS = 3600L
    private val json = Json { ignoreUnknownKeys = true }
    private val random = SecureRandom()

    private val secret: String by lazy {
        AppEnvironment.get("TOURVERSE_JWT_SECRET")
            ?: if (AppEnvironment.isProduction) {
                throw IllegalStateException("TOURVERSE_JWT_SECRET is required in production.")
            } else {
                "tourverse-development-secret-change-before-production"
            }
    }

    // Creates access token and returns the resulting domain value.
    fun createAccessToken(
        userId: UUID,
        role: String,
        lifetimeSeconds: Long = DEFAULT_ACCESS_LIFETIME_SECONDS
    ): IssuedAccessToken {
        require(secret.length >= 32) { "TOURVERSE_JWT_SECRET must contain at least 32 characters." }
        val now = Instant.now().epochSecond
        val claims = AccessTokenClaims(userId.toString(), role, now, now + lifetimeSeconds)
        val payload = encode(json.encodeToString(AccessTokenClaims.serializer(), claims).toByteArray())
        return IssuedAccessToken("$payload.${sign(payload)}", lifetimeSeconds)
    }

    // Validates access token before protected work continues.
    fun verifyAccessToken(token: String): AccessTokenClaims? {
        val parts = token.split('.')
        if (parts.size != 2 || !constantTimeEquals(sign(parts[0]), parts[1])) return null
        return runCatching {
            val decoded = String(Base64.getUrlDecoder().decode(parts[0]), StandardCharsets.UTF_8)
            json.decodeFromString(AccessTokenClaims.serializer(), decoded)
                .takeIf { it.expiresAt > Instant.now().epochSecond }
        }.getOrNull()
    }

    // Creates refresh token and returns the resulting domain value.
    fun createRefreshToken(): String {
        val bytes = ByteArray(48).also(random::nextBytes)
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
    }

    // Checks whether hash refresh token is true in the current context.
    fun hashRefreshToken(token: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(token.toByteArray(StandardCharsets.UTF_8))
        return digest.joinToString("") { "%02x".format(it) }
    }

    // Encapsulates the sign operation behind a reusable function.
    private fun sign(value: String): String {
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(secret.toByteArray(StandardCharsets.UTF_8), "HmacSHA256"))
        return encode(mac.doFinal(value.toByteArray(StandardCharsets.UTF_8)))
    }

    // Converts input into the encode representation used by the next application layer.
    private fun encode(bytes: ByteArray): String = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)

    // Encapsulates the constant time equals operation behind a reusable function.
    private fun constantTimeEquals(first: String, second: String): Boolean =
        MessageDigest.isEqual(first.toByteArray(), second.toByteArray())
}
