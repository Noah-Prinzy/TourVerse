package com.tourverse.models

import com.tourverse.utils.InstantSerializer
import com.tourverse.utils.LocalDateSerializer
import com.tourverse.utils.UUIDSerializer
import kotlinx.serialization.Serializable
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

@Serializable data class CreateReviewRequest(val rating: Int, val comment: String? = null)
@Serializable data class UpdateReviewRequest(val rating: Int? = null, val comment: String? = null)
@Serializable data class ReviewResponse(
    @Serializable(with = UUIDSerializer::class) val id: UUID,
    @Serializable(with = UUIDSerializer::class) val userId: UUID,
    @Serializable(with = UUIDSerializer::class) val destinationId: UUID,
    val rating: Int,
    val comment: String?,
    @Serializable(with = InstantSerializer::class) val createdAt: Instant,
    @Serializable(with = InstantSerializer::class) val updatedAt: Instant
)
@Serializable data class DestinationReviewSummary(val averageRating: Double, val reviewCount: Long, val reviews: List<ReviewResponse>)

@Serializable data class FavoriteResponse(
    @Serializable(with = UUIDSerializer::class) val id: UUID,
    val destination: Destination,
    @Serializable(with = InstantSerializer::class) val createdAt: Instant
)

@Serializable data class CreateTripRequest(
    val title: String,
    val description: String? = null,
    @Serializable(with = LocalDateSerializer::class) val startDate: LocalDate? = null,
    @Serializable(with = LocalDateSerializer::class) val endDate: LocalDate? = null
)
@Serializable data class UpdateTripRequest(
    val title: String? = null,
    val description: String? = null,
    @Serializable(with = LocalDateSerializer::class) val startDate: LocalDate? = null,
    @Serializable(with = LocalDateSerializer::class) val endDate: LocalDate? = null
)
@Serializable data class AddTripDestinationRequest(
    @Serializable(with = UUIDSerializer::class) val destinationId: UUID,
    @Serializable(with = LocalDateSerializer::class) val visitDate: LocalDate? = null,
    val notes: String? = null,
    val displayOrder: Int = 0
)
@Serializable data class TripDestinationResponse(
    @Serializable(with = UUIDSerializer::class) val id: UUID,
    val destination: Destination,
    @Serializable(with = LocalDateSerializer::class) val visitDate: LocalDate?,
    val notes: String?,
    val displayOrder: Int
)
@Serializable data class TripResponse(
    @Serializable(with = UUIDSerializer::class) val id: UUID,
    val title: String,
    val description: String?,
    @Serializable(with = LocalDateSerializer::class) val startDate: LocalDate?,
    @Serializable(with = LocalDateSerializer::class) val endDate: LocalDate?,
    val destinations: List<TripDestinationResponse>,
    @Serializable(with = InstantSerializer::class) val createdAt: Instant,
    @Serializable(with = InstantSerializer::class) val updatedAt: Instant
)
