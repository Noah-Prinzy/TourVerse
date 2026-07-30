package com.tourverse.models

import com.tourverse.utils.InstantSerializer
import com.tourverse.utils.UUIDSerializer
import kotlinx.serialization.Serializable
import java.time.Instant
import java.util.UUID

@Serializable
// Carries register request values between application layers.
data class RegisterRequest(
    val firstName: String,
    val lastName: String,
    val email: String,
    val password: String
)

@Serializable
// Carries login request values between application layers.
data class LoginRequest(val email: String, val password: String)

@Serializable
// Carries refresh token request values between application layers.
data class RefreshTokenRequest(val refreshToken: String)

@Serializable
// Carries logout request values between application layers.
data class LogoutRequest(val refreshToken: String)

@Serializable
// Carries change password request values between application layers.
data class ChangePasswordRequest(
    val currentPassword: String,
    val newPassword: String
)

@Serializable
// Carries update profile request values between application layers.
data class UpdateProfileRequest(
    val firstName: String? = null,
    val lastName: String? = null,
    val bio: String? = null,
    val profileImageUrl: String? = null
)

@Serializable
// Carries user response values between application layers.
data class UserResponse(
    @Serializable(with = UUIDSerializer::class) val id: UUID,
    val firstName: String,
    val lastName: String,
    val email: String,
    val profileImageUrl: String?,
    val bio: String?,
    val role: String,
    @Serializable(with = InstantSerializer::class) val createdAt: Instant
)

@Serializable
// Carries auth response values between application layers.
data class AuthResponse(
    val accessToken: String,
    val refreshToken: String,
    val tokenType: String = "Bearer",
    val expiresInSeconds: Long,
    val user: UserResponse
)
