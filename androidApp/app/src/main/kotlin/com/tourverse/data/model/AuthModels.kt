package com.tourverse.data.model

import kotlinx.serialization.Serializable

@Serializable data class RegisterRequest(val firstName: String, val lastName: String, val email: String, val password: String)
@Serializable data class LoginRequest(val email: String, val password: String)
@Serializable data class RefreshTokenRequest(val refreshToken: String)
@Serializable data class LogoutRequest(val refreshToken: String)
@Serializable data class DeleteAccountRequest(val password: String)
@Serializable data class UpdateProfileImageRequest(val profileImageUrl: String?)
@Serializable data class UpdateUserProfileRequest(
    val firstName: String? = null,
    val lastName: String? = null,
    val bio: String? = null,
    val nationality: String? = null,
    val travelInterests: List<String>? = null,
    val profilePublic: Boolean? = null
)
@Serializable data class User(
    val id: String, val firstName: String, val lastName: String, val email: String,
    val profileImageUrl: String?, val bio: String?, val role: String, val createdAt: String
)
@Serializable data class AuthResponse(
    val accessToken: String, val refreshToken: String, val tokenType: String = "Bearer",
    val expiresInSeconds: Long, val user: User
)
@Serializable data class UserProfile(
    val id: String, val firstName: String, val lastName: String, val email: String,
    val profileImageUrl: String?, val bio: String?, val nationality: String?,
    val travelInterests: List<String>, val profilePublic: Boolean, val role: String,
    val createdAt: String, val updatedAt: String
)
