package com.tourverse.services

import com.tourverse.database.tables.*
import com.tourverse.exceptions.ConflictException
import com.tourverse.exceptions.ForbiddenException
import com.tourverse.exceptions.NotFoundException
import com.tourverse.models.*
import com.tourverse.utils.ValidationException
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.*
import org.jetbrains.exposed.v1.jdbc.transactions.suspendTransaction
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.UUID

class CommunityService {
    // Coordinates the destination reviews business workflow for callers.
    suspend fun destinationReviews(destinationId: UUID): DestinationReviewSummary = suspendTransaction {
        ensureDestination(destinationId)
        val rows = ReviewsTable.selectAll().where { ReviewsTable.destinationId eq destinationId }
            .orderBy(ReviewsTable.createdAt to SortOrder.DESC).map { it.toReview() }
        DestinationReviewSummary(
            averageRating = if (rows.isEmpty()) 0.0 else rows.map { it.rating }.average(),
            reviewCount = rows.size.toLong(), reviews = rows
        )
    }

    // Creates review after applying validation and business rules.
    suspend fun createReview(userId: UUID, destinationId: UUID, request: CreateReviewRequest): ReviewResponse = suspendTransaction {
        validateReview(request.rating, request.comment)
        ensureDestination(destinationId)
        if (ReviewsTable.selectAll().where { (ReviewsTable.userId eq userId) and (ReviewsTable.destinationId eq destinationId) }.any())
            throw ConflictException("You have already reviewed this destination")
        val id = UUID.randomUUID(); val now = now()
        ReviewsTable.insert { r ->
            r[ReviewsTable.id] = id; r[ReviewsTable.userId] = userId; r[ReviewsTable.destinationId] = destinationId
            r[rating] = request.rating; r[comment] = clean(request.comment); r[createdAt] = now; r[updatedAt] = now
        }
        ReviewsTable.selectAll().where { ReviewsTable.id eq id }.single().toReview()
    }

    // Updates review while keeping related state consistent.
    suspend fun updateReview(userId: UUID, role: String, id: UUID, request: UpdateReviewRequest): ReviewResponse = suspendTransaction {
        val old = ReviewsTable.selectAll().where { ReviewsTable.id eq id }.singleOrNull() ?: throw NotFoundException("Review not found")
        if (old[ReviewsTable.userId] != userId && role != "ADMIN") throw ForbiddenException("You can only edit your own review")
        val rating = request.rating ?: old[ReviewsTable.rating]; val comment = request.comment ?: old[ReviewsTable.comment]
        validateReview(rating, comment)
        ReviewsTable.update({ ReviewsTable.id eq id }) { r -> r[ReviewsTable.rating] = rating; r[ReviewsTable.comment] = clean(comment); r[updatedAt] = now() }
        ReviewsTable.selectAll().where { ReviewsTable.id eq id }.single().toReview()
    }

    // Removes or invalidates review after enforcing ownership and authorization rules.
    suspend fun deleteReview(userId: UUID, role: String, id: UUID) = suspendTransaction {
        val row = ReviewsTable.selectAll().where { ReviewsTable.id eq id }.singleOrNull() ?: throw NotFoundException("Review not found")
        if (row[ReviewsTable.userId] != userId && role != "ADMIN") throw ForbiddenException("You can only delete your own review")
        ReviewsTable.deleteWhere { ReviewsTable.id eq id }; Unit
    }

    // Coordinates the favorites business workflow for callers.
    suspend fun favorites(userId: UUID): List<FavoriteResponse> = suspendTransaction {
        (FavoritesTable innerJoin DestinationsTable).selectAll().where { FavoritesTable.userId eq userId }
            .orderBy(FavoritesTable.createdAt to SortOrder.DESC).map { FavoriteResponse(it[FavoritesTable.id], it.toDestination(), it[FavoritesTable.createdAt].toInstant()) }
    }

    // Creates favorite after applying validation and business rules.
    suspend fun addFavorite(userId: UUID, destinationId: UUID): FavoriteResponse = suspendTransaction {
        ensureDestination(destinationId)
        val existing = FavoritesTable.selectAll().where { (FavoritesTable.userId eq userId) and (FavoritesTable.destinationId eq destinationId) }.singleOrNull()
        if (existing != null) throw ConflictException("Destination is already in your favorites")
        val id = UUID.randomUUID(); val now = now()
        FavoritesTable.insert { r -> r[FavoritesTable.id] = id; r[FavoritesTable.userId] = userId; r[FavoritesTable.destinationId] = destinationId; r[createdAt] = now }
        val d = DestinationsTable.selectAll().where { DestinationsTable.id eq destinationId }.single().toDestination()
        FavoriteResponse(id, d, now.toInstant())
    }

    // Removes or invalidates favorite after enforcing ownership and authorization rules.
    suspend fun removeFavorite(userId: UUID, destinationId: UUID) = suspendTransaction {
        val deleted = FavoritesTable.deleteWhere { (FavoritesTable.userId eq userId) and (FavoritesTable.destinationId eq destinationId) }
        if (deleted == 0) throw NotFoundException("Favorite not found")
    }

    // Coordinates the trips business workflow for callers.
    suspend fun trips(userId: UUID): List<TripResponse> = suspendTransaction {
        TripsTable.selectAll().where { TripsTable.userId eq userId }.orderBy(TripsTable.createdAt to SortOrder.DESC).map { trip(it) }
    }

    // Coordinates the trip business workflow for callers.
    suspend fun trip(userId: UUID, id: UUID): TripResponse = suspendTransaction { ownedTrip(userId, id).let { trip(it) } }

    // Creates trip after applying validation and business rules.
    suspend fun createTrip(userId: UUID, request: CreateTripRequest): TripResponse = suspendTransaction {
        validateTrip(request.title, request.startDate, request.endDate)
        val id = UUID.randomUUID(); val now = now()
        TripsTable.insert { r ->
            r[TripsTable.id] = id; r[TripsTable.userId] = userId; r[title] = request.title.trim(); r[description] = clean(request.description)
            r[startDate] = request.startDate; r[endDate] = request.endDate; r[createdAt] = now; r[updatedAt] = now
        }
        trip(TripsTable.selectAll().where { TripsTable.id eq id }.single())
    }

    // Updates trip while keeping related state consistent.
    suspend fun updateTrip(userId: UUID, id: UUID, request: UpdateTripRequest): TripResponse = suspendTransaction {
        val old = ownedTrip(userId, id)
        val title = request.title ?: old[TripsTable.title]; val start = request.startDate ?: old[TripsTable.startDate]; val end = request.endDate ?: old[TripsTable.endDate]
        validateTrip(title, start, end)
        TripsTable.update({ TripsTable.id eq id }) { r ->
            request.title?.let { r[TripsTable.title] = it.trim() }; request.description?.let { r[description] = clean(it) }
            request.startDate?.let { r[startDate] = it }; request.endDate?.let { r[endDate] = it }; r[updatedAt] = now()
        }
        trip(TripsTable.selectAll().where { TripsTable.id eq id }.single())
    }

    // Removes or invalidates trip after enforcing ownership and authorization rules.
    suspend fun deleteTrip(userId: UUID, id: UUID) = suspendTransaction { ownedTrip(userId, id); TripsTable.deleteWhere { TripsTable.id eq id }; Unit }

    // Creates trip destination after applying validation and business rules.
    suspend fun addTripDestination(userId: UUID, tripId: UUID, request: AddTripDestinationRequest): TripResponse = suspendTransaction {
        ownedTrip(userId, tripId); ensureDestination(request.destinationId)
        if (request.displayOrder < 0) throw ValidationException("Display order cannot be negative")
        if (TripDestinationsTable.selectAll().where { (TripDestinationsTable.tripId eq tripId) and (TripDestinationsTable.destinationId eq request.destinationId) }.any())
            throw ConflictException("Destination is already in this trip")
        TripDestinationsTable.insert { r ->
            r[TripDestinationsTable.id] = UUID.randomUUID(); r[TripDestinationsTable.tripId] = tripId; r[TripDestinationsTable.destinationId] = request.destinationId
            r[visitDate] = request.visitDate; r[notes] = clean(request.notes); r[displayOrder] = request.displayOrder; r[createdAt] = now()
        }
        trip(TripsTable.selectAll().where { TripsTable.id eq tripId }.single())
    }

    // Removes or invalidates trip destination after enforcing ownership and authorization rules.
    suspend fun removeTripDestination(userId: UUID, tripId: UUID, destinationId: UUID): TripResponse = suspendTransaction {
        ownedTrip(userId, tripId)
        val deleted = TripDestinationsTable.deleteWhere { (TripDestinationsTable.tripId eq tripId) and (TripDestinationsTable.destinationId eq destinationId) }
        if (deleted == 0) throw NotFoundException("Destination is not part of this trip")
        trip(TripsTable.selectAll().where { TripsTable.id eq tripId }.single())
    }

    // Validates review and stops the workflow when input is invalid.
    private fun validateReview(rating: Int, comment: String?) {
        if (rating !in 1..5) throw ValidationException("Rating must be between 1 and 5")
        if ((comment?.length ?: 0) > 2000) throw ValidationException("Review comment cannot exceed 2000 characters")
    }
    // Validates trip and stops the workflow when input is invalid.
    private fun validateTrip(title: String, start: java.time.LocalDate?, end: java.time.LocalDate?) {
        if (title.isBlank()) throw ValidationException("Trip title is required")
        if (title.trim().length > 150) throw ValidationException("Trip title cannot exceed 150 characters")
        if (start != null && end != null && end.isBefore(start)) throw ValidationException("Trip end date cannot be before start date")
    }
    // Converts the supplied values into the clean form required by the domain model.
    private fun clean(value: String?) = value?.trim()?.takeIf { it.isNotEmpty() }
    // Coordinates the now business workflow for callers.
    private fun now() = OffsetDateTime.now(ZoneOffset.UTC)
    // Validates destination and stops the workflow when input is invalid.
    private fun ensureDestination(id: UUID) { if (!DestinationsTable.selectAll().where { DestinationsTable.id eq id }.any()) throw NotFoundException("Destination not found") }
    // Coordinates the owned trip business workflow for callers.
    private fun ownedTrip(userId: UUID, id: UUID): ResultRow = TripsTable.selectAll().where { (TripsTable.id eq id) and (TripsTable.userId eq userId) }.singleOrNull() ?: throw NotFoundException("Trip not found")
    // Coordinates the trip business workflow for callers.
    private fun trip(row: ResultRow): TripResponse {
        val tripId = row[TripsTable.id]
        val destinations = (TripDestinationsTable innerJoin DestinationsTable).selectAll().where { TripDestinationsTable.tripId eq tripId }
            .orderBy(TripDestinationsTable.displayOrder to SortOrder.ASC).map {
                TripDestinationResponse(it[TripDestinationsTable.id], it.toDestination(), it[TripDestinationsTable.visitDate], it[TripDestinationsTable.notes], it[TripDestinationsTable.displayOrder])
            }
        return TripResponse(tripId, row[TripsTable.title], row[TripsTable.description], row[TripsTable.startDate], row[TripsTable.endDate], destinations, row[TripsTable.createdAt].toInstant(), row[TripsTable.updatedAt].toInstant())
    }
    // Coordinates the result row business workflow for callers.
    private fun ResultRow.toReview() = ReviewResponse(this[ReviewsTable.id], this[ReviewsTable.userId], this[ReviewsTable.destinationId], this[ReviewsTable.rating], this[ReviewsTable.comment], this[ReviewsTable.createdAt].toInstant(), this[ReviewsTable.updatedAt].toInstant())
    // Coordinates the result row business workflow for callers.
    private fun ResultRow.toDestination() = Destination(
        this[DestinationsTable.id], this[DestinationsTable.name], this[DestinationsTable.country], this[DestinationsTable.city], this[DestinationsTable.description], this[DestinationsTable.category],
        this[DestinationsTable.latitude]?.toDouble(), this[DestinationsTable.longitude]?.toDouble(), this[DestinationsTable.coverImageUrl], this[DestinationsTable.createdAt].toInstant(), this[DestinationsTable.updatedAt].toInstant()
    )
}
