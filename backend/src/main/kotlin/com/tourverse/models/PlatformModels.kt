package com.tourverse.models

import com.tourverse.utils.InstantSerializer
import com.tourverse.utils.LocalDateSerializer
import com.tourverse.utils.UUIDSerializer
import kotlinx.serialization.Serializable
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

@Serializable data class CreateTourismServiceRequest(
    @Serializable(with = UUIDSerializer::class) val destinationId: UUID? = null,
    val name: String, val serviceType: String, val description: String? = null,
    val phone: String? = null, val email: String? = null, val websiteUrl: String? = null,
    val address: String? = null, val priceFrom: Double? = null, val currency: String = "USD"
)
@Serializable data class UpdateTourismServiceRequest(
    @Serializable(with = UUIDSerializer::class) val destinationId: UUID? = null,
    val name: String? = null, val serviceType: String? = null, val description: String? = null,
    val phone: String? = null, val email: String? = null, val websiteUrl: String? = null,
    val address: String? = null, val priceFrom: Double? = null, val currency: String? = null,
    val active: Boolean? = null
)
@Serializable data class TourismServiceResponse(
    @Serializable(with = UUIDSerializer::class) val id: UUID,
    @Serializable(with = UUIDSerializer::class) val ownerUserId: UUID?,
    @Serializable(with = UUIDSerializer::class) val destinationId: UUID?,
    val name: String, val serviceType: String, val description: String?, val phone: String?,
    val email: String?, val websiteUrl: String?, val address: String?, val priceFrom: Double?,
    val currency: String, val active: Boolean,
    @Serializable(with = InstantSerializer::class) val createdAt: Instant,
    @Serializable(with = InstantSerializer::class) val updatedAt: Instant
)

@Serializable data class CreateBookingRequest(
    @Serializable(with = UUIDSerializer::class) val serviceId: UUID,
    @Serializable(with = LocalDateSerializer::class) val bookingDate: LocalDate,
    val numberOfPeople: Int, val notes: String? = null
)
@Serializable data class UpdateBookingStatusRequest(val status: String)
@Serializable data class BookingResponse(
    @Serializable(with = UUIDSerializer::class) val id: UUID,
    @Serializable(with = UUIDSerializer::class) val userId: UUID,
    val service: TourismServiceResponse,
    @Serializable(with = LocalDateSerializer::class) val bookingDate: LocalDate,
    val numberOfPeople: Int, val totalPrice: Double?, val currency: String,
    val status: String, val paymentStatus: String, val notes: String?,
    @Serializable(with = InstantSerializer::class) val createdAt: Instant,
    @Serializable(with = InstantSerializer::class) val updatedAt: Instant
)

@Serializable data class NotificationResponse(
    @Serializable(with = UUIDSerializer::class) val id: UUID,
    val title: String, val message: String, val type: String, val isRead: Boolean,
    @Serializable(with = InstantSerializer::class) val createdAt: Instant
)
@Serializable data class AdminRoleUpdateRequest(val role: String)
@Serializable data class AdminStatisticsResponse(
    val users: Long, val destinations: Long, val reviews: Long, val services: Long,
    val bookings: Long, val pendingBookings: Long
)
@Serializable data class AdminUserResponse(
    @Serializable(with = UUIDSerializer::class) val id: UUID,
    val firstName: String, val lastName: String, val email: String, val role: String,
    @Serializable(with = InstantSerializer::class) val createdAt: Instant
)
