package com.tourverse.data.model

import kotlinx.serialization.Serializable

@Serializable data class Category(
    val id: String, val name: String, val slug: String, val description: String?,
    val iconUrl: String?, val active: Boolean, val createdAt: String, val updatedAt: String
)
@Serializable data class Review(
    val id: String, val userId: String, val destinationId: String, val rating: Int,
    val comment: String?, val createdAt: String, val updatedAt: String
)
@Serializable data class ReviewSummary(val averageRating: Double, val reviewCount: Long, val reviews: List<Review>)
@Serializable data class CreateReviewRequest(val rating: Int, val comment: String? = null)
@Serializable data class UpdateReviewRequest(val rating: Int? = null, val comment: String? = null)
@Serializable data class Favorite(val id: String, val destination: Destination, val createdAt: String)
@Serializable data class CreateTripRequest(val title: String, val description: String? = null, val startDate: String? = null, val endDate: String? = null)
@Serializable data class UpdateTripRequest(val title: String? = null, val description: String? = null, val startDate: String? = null, val endDate: String? = null)
@Serializable data class AddTripDestinationRequest(val destinationId: String, val visitDate: String? = null, val notes: String? = null, val displayOrder: Int = 0)
@Serializable data class TripDestination(val id: String, val destination: Destination, val visitDate: String?, val notes: String?, val displayOrder: Int)
@Serializable data class Trip(
    val id: String, val title: String, val description: String?, val startDate: String?, val endDate: String?,
    val destinations: List<TripDestination>, val createdAt: String, val updatedAt: String
)
