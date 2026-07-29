package com.tourverse.models

import com.tourverse.utils.InstantSerializer
import com.tourverse.utils.UUIDSerializer
import kotlinx.serialization.Serializable
import java.time.Instant
import java.util.UUID

@Serializable
enum class DataOrigin {
    TOURVERSE_CURATED, EXTERNAL, HYBRID, DEVELOPMENT_SEED
}

@Serializable
enum class CacheStatus {
    FRESH, STALE, REFRESH_PENDING, REFRESH_FAILED, NOT_APPLICABLE
}

@Serializable
enum class VerificationStatus {
    VERIFIED, PARTIALLY_VERIFIED, REVIEW_REQUIRED, REJECTED
}

@Serializable
data class Destination(
    @Serializable(with = UUIDSerializer::class)
    val id: UUID,
    val name: String,
    val country: String,
    val city: String?,
    val description: String,
    val category: String,
    val latitude: Double?,
    val longitude: Double?,
    val coverImageUrl: String?,
    @Serializable(with = InstantSerializer::class)
    val createdAt: Instant,
    @Serializable(with = InstantSerializer::class)
    val updatedAt: Instant,
    val countryCode: String? = null,
    val dataOrigin: DataOrigin = DataOrigin.TOURVERSE_CURATED,
    @Serializable(with = InstantSerializer::class)
    val lastVerifiedAt: Instant? = null,
    val verificationStatus: VerificationStatus = VerificationStatus.VERIFIED,
    val attributionSummary: String? = null,
    val mapAvailable: Boolean = latitude != null && longitude != null,
    val googlePlaceId: String? = null
)
