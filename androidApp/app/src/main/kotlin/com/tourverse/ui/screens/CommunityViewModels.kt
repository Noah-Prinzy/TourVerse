package com.tourverse.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tourverse.data.model.*
import com.tourverse.data.repository.CommunityRepository
import com.tourverse.data.repository.TripRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class DestinationDetailState(
    val loading: Boolean = true, val destination: Destination? = null,
    val reviews: ReviewSummary? = null, val favorite: Boolean = false,
    val error: String? = null, val busy: Boolean = false
)

class DestinationDetailViewModel(
    private val id: String,
    private val authenticatedUserId: String?,
    private val repository: CommunityRepository
) : ViewModel() {
    private val mutableState = MutableStateFlow(DestinationDetailState())
    val state: StateFlow<DestinationDetailState> = mutableState
    init { load() }
    fun load() = viewModelScope.launch {
        mutableState.value = DestinationDetailState()
        try {
            val destination = repository.destination(id)
            val reviews = repository.reviews(id)
            val favorite = authenticatedUserId != null && repository.favorites().any { it.destination.id == id }
            mutableState.value = DestinationDetailState(false, destination, reviews, favorite)
        } catch (exception: Exception) { mutableState.value = DestinationDetailState(false, error = exception.message) }
    }
    fun toggleFavorite() = action {
        if (mutableState.value.favorite) repository.removeFavorite(id) else repository.addFavorite(id)
        mutableState.value = mutableState.value.copy(favorite = !mutableState.value.favorite)
    }
    fun review(rating: Int, comment: String) = action {
        val own = mutableState.value.reviews?.reviews?.find { it.userId == authenticatedUserId }
        if (own == null) repository.createReview(id, rating, comment) else repository.updateReview(own.id, rating, comment)
        mutableState.value = mutableState.value.copy(reviews = repository.reviews(id))
    }
    fun deleteReview(reviewId: String) = action {
        repository.deleteReview(reviewId)
        mutableState.value = mutableState.value.copy(reviews = repository.reviews(id))
    }
    private fun action(block: suspend () -> Unit) = viewModelScope.launch {
        mutableState.value = mutableState.value.copy(busy = true, error = null)
        try { block(); mutableState.value = mutableState.value.copy(busy = false) }
        catch (exception: Exception) { mutableState.value = mutableState.value.copy(busy = false, error = exception.message) }
    }
}

data class FavoritesState(val loading: Boolean = true, val items: List<Favorite> = emptyList(), val error: String? = null)
class FavoritesViewModel(private val repository: CommunityRepository) : ViewModel() {
    private val mutableState = MutableStateFlow(FavoritesState())
    val state: StateFlow<FavoritesState> = mutableState
    init { load() }
    fun load() = viewModelScope.launch {
        mutableState.value = FavoritesState()
        try { mutableState.value = FavoritesState(false, repository.favorites()) }
        catch (exception: Exception) { mutableState.value = FavoritesState(false, error = exception.message) }
    }
}

data class TripsState(val loading: Boolean = true, val trips: List<Trip> = emptyList(), val selected: Trip? = null, val error: String? = null, val busy: Boolean = false)
class TripsViewModel(private val repository: TripRepository, private val tripId: String? = null) : ViewModel() {
    private val mutableState = MutableStateFlow(TripsState())
    val state: StateFlow<TripsState> = mutableState
    init { load() }
    fun load() = viewModelScope.launch {
        mutableState.value = TripsState()
        try {
            mutableState.value = if (tripId == null) TripsState(false, trips = repository.all())
            else TripsState(false, selected = repository.one(tripId))
        } catch (exception: Exception) { mutableState.value = TripsState(false, error = exception.message) }
    }
    fun create(title: String, description: String?, start: String?, end: String?, done: (Trip) -> Unit) = action {
        done(repository.create(CreateTripRequest(title, description, start, end)))
    }
    fun update(title: String, description: String?, start: String?, end: String?) = action {
        val id = tripId ?: return@action
        mutableState.value = mutableState.value.copy(selected = repository.update(id, UpdateTripRequest(title, description, start, end)))
    }
    fun delete(done: () -> Unit) = action { repository.delete(tripId ?: return@action); done() }
    fun addDestination(destinationId: String, date: String?, notes: String?) = action {
        if (runCatching { java.util.UUID.fromString(destinationId) }.isFailure) {
            throw IllegalArgumentException("Destination ID must be a valid UUID.")
        }
        val trip = mutableState.value.selected ?: return@action
        mutableState.value = mutableState.value.copy(selected = repository.addDestination(trip.id, AddTripDestinationRequest(destinationId, date, notes, trip.destinations.size)))
    }
    fun removeDestination(destinationId: String) = action {
        val id = tripId ?: return@action
        mutableState.value = mutableState.value.copy(selected = repository.removeDestination(id, destinationId))
    }
    private fun action(block: suspend () -> Unit) = viewModelScope.launch {
        mutableState.value = mutableState.value.copy(busy = true, error = null)
        try { block(); mutableState.value = mutableState.value.copy(busy = false) }
        catch (exception: Exception) { mutableState.value = mutableState.value.copy(busy = false, error = exception.message) }
    }
}
