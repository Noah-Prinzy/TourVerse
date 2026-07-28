package com.tourverse.data.repository

import com.tourverse.data.model.*
import com.tourverse.data.remote.AuthApi
import com.tourverse.data.remote.TourismApi

class CommunityRepository(private val publicApi: TourismApi, private val authApi: AuthApi) {
    suspend fun destination(id: String) = publicApi.getDestination(id)
    suspend fun categories() = publicApi.getCategories()
    suspend fun reviews(id: String) = publicApi.getReviews(id)
    suspend fun favorites(): List<Favorite> = authApi.get("api/favorites")
    suspend fun addFavorite(id: String): Favorite = authApi.post("api/favorites/$id")
    suspend fun removeFavorite(id: String): ApiMessage = authApi.delete("api/favorites/$id")
    suspend fun createReview(destinationId: String, rating: Int, comment: String): Review =
        authApi.post("api/destinations/$destinationId/reviews", CreateReviewRequest(rating, comment.ifBlank { null }))
    suspend fun updateReview(id: String, rating: Int, comment: String): Review =
        authApi.put("api/reviews/$id", UpdateReviewRequest(rating, comment.ifBlank { null }))
    suspend fun deleteReview(id: String): ApiMessage = authApi.delete("api/reviews/$id")
}

class TripRepository(private val api: AuthApi) {
    suspend fun all(): List<Trip> = api.get("api/trips")
    suspend fun one(id: String): Trip = api.get("api/trips/$id")
    suspend fun create(request: CreateTripRequest): Trip = api.post("api/trips", request)
    suspend fun update(id: String, request: UpdateTripRequest): Trip = api.put("api/trips/$id", request)
    suspend fun delete(id: String): ApiMessage = api.delete("api/trips/$id")
    suspend fun addDestination(id: String, request: AddTripDestinationRequest): Trip = api.post("api/trips/$id/destinations", request)
    suspend fun removeDestination(id: String, destinationId: String): Trip = api.delete("api/trips/$id/destinations/$destinationId")
}
