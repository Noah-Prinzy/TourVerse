package com.tourverse.models

import com.tourverse.utils.InstantSerializer
import com.tourverse.utils.UUIDSerializer
import kotlinx.serialization.Serializable
import java.time.Instant
import java.util.UUID

@Serializable
// Carries user profile response values between application layers.
data class UserProfileResponse(
    @Serializable(with = UUIDSerializer::class) val id: UUID,
    val firstName: String,
    val lastName: String,
    val email: String,
    val profileImageUrl: String?,
    val bio: String?,
    val nationality: String?,
    val travelInterests: List<String>,
    val profilePublic: Boolean,
    val role: String,
    @Serializable(with = InstantSerializer::class) val createdAt: Instant,
    @Serializable(with = InstantSerializer::class) val updatedAt: Instant
)

@Serializable
// Carries update user profile request values between application layers.
data class UpdateUserProfileRequest(
    val firstName: String? = null,
    val lastName: String? = null,
    val bio: String? = null,
    val nationality: String? = null,
    val travelInterests: List<String>? = null,
    val profilePublic: Boolean? = null
)

@Serializable
// Carries update profile image request values between application layers.
data class UpdateProfileImageRequest(val profileImageUrl: String?)

@Serializable
// Carries delete account request values between application layers.
data class DeleteAccountRequest(val password: String)
