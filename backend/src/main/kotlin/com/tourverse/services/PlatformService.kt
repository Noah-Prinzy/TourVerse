package com.tourverse.services

import com.tourverse.database.tables.*
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
import java.math.BigDecimal
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.UUID

class PlatformService {
    // Coordinates the services business workflow for callers.
    suspend fun services(type: String?, destinationId: UUID?, includeInactive: Boolean = false): List<TourismServiceResponse> = suspendTransaction {
        TourismServicesTable.selectAll().filter { row ->
            (includeInactive || row[TourismServicesTable.active]) &&
                (type == null || row[TourismServicesTable.serviceType].equals(type, true)) &&
                (destinationId == null || row[TourismServicesTable.destinationId] == destinationId)
        }.sortedBy { it[TourismServicesTable.name].lowercase() }.map { it.toService() }
    }

    // Coordinates the service business workflow for callers.
    suspend fun service(id: UUID, includeInactive: Boolean = false): TourismServiceResponse = suspendTransaction {
        val row = TourismServicesTable.selectAll().where { TourismServicesTable.id eq id }.singleOrNull()
            ?: throw NotFoundException("Tourism service not found")
        if (!includeInactive && !row[TourismServicesTable.active]) throw NotFoundException("Tourism service not found")
        row.toService()
    }

    // Creates service after applying validation and business rules.
    suspend fun createService(ownerId: UUID, request: CreateTourismServiceRequest): TourismServiceResponse = suspendTransaction {
        validateService(request.name, request.serviceType, request.email, request.websiteUrl, request.priceFrom, request.currency)
        request.destinationId?.let { ensureDestination(it) }
        val id = UUID.randomUUID(); val now = now()
        TourismServicesTable.insert { r ->
            r[TourismServicesTable.id] = id; r[ownerUserId] = ownerId; r[destinationId] = request.destinationId
            r[name] = request.name.trim(); r[serviceType] = request.serviceType.trim().uppercase(); r[description] = clean(request.description)
            r[phone] = clean(request.phone); r[email] = clean(request.email)?.lowercase(); r[websiteUrl] = clean(request.websiteUrl)
            r[address] = clean(request.address); r[priceFrom] = request.priceFrom?.let(BigDecimal::valueOf)
            r[currency] = request.currency.trim().uppercase(); r[active] = true; r[createdAt] = now; r[updatedAt] = now
        }
        TourismServicesTable.selectAll().where { TourismServicesTable.id eq id }.single().toService()
    }

    // Updates service while keeping related state consistent.
    suspend fun updateService(userId: UUID, role: String, id: UUID, request: UpdateTourismServiceRequest): TourismServiceResponse = suspendTransaction {
        val old = serviceRow(id); ensureOwnerOrAdmin(userId, role, old[TourismServicesTable.ownerUserId])
        val name = request.name ?: old[TourismServicesTable.name]
        val type = request.serviceType ?: old[TourismServicesTable.serviceType]
        val email = request.email ?: old[TourismServicesTable.email]
        val website = request.websiteUrl ?: old[TourismServicesTable.websiteUrl]
        val price = request.priceFrom ?: old[TourismServicesTable.priceFrom]?.toDouble()
        val currency = request.currency ?: old[TourismServicesTable.currency]
        validateService(name, type, email, website, price, currency)
        request.destinationId?.let { ensureDestination(it) }
        TourismServicesTable.update({ TourismServicesTable.id eq id }) { r ->
            request.destinationId?.let { r[destinationId] = it }; request.name?.let { r[TourismServicesTable.name] = it.trim() }
            request.serviceType?.let { r[serviceType] = it.trim().uppercase() }; request.description?.let { r[description] = clean(it) }
            request.phone?.let { r[phone] = clean(it) }; request.email?.let { r[TourismServicesTable.email] = clean(it)?.lowercase() }
            request.websiteUrl?.let { r[websiteUrl] = clean(it) }; request.address?.let { r[address] = clean(it) }
            request.priceFrom?.let { r[priceFrom] = BigDecimal.valueOf(it) }; request.currency?.let { r[TourismServicesTable.currency] = it.trim().uppercase() }
            request.active?.let { r[active] = it }; r[updatedAt] = now()
        }
        serviceRow(id).toService()
    }

    // Removes or invalidates service after enforcing ownership and authorization rules.
    suspend fun deleteService(userId: UUID, role: String, id: UUID) = suspendTransaction {
        val row = serviceRow(id); ensureOwnerOrAdmin(userId, role, row[TourismServicesTable.ownerUserId])
        if (BookingsTable.selectAll().where { BookingsTable.serviceId eq id }.any()) {
            TourismServicesTable.update({ TourismServicesTable.id eq id }) { it[active] = false; it[updatedAt] = now() }
        } else TourismServicesTable.deleteWhere { TourismServicesTable.id eq id }
        Unit
    }

    // Coordinates the bookings business workflow for callers.
    suspend fun bookings(userId: UUID, role: String): List<BookingResponse> = suspendTransaction {
        val rows = if (role == "ADMIN") BookingsTable.selectAll() else BookingsTable.selectAll().where { BookingsTable.userId eq userId }
        rows.orderBy(BookingsTable.createdAt to SortOrder.DESC).map { booking(it) }
    }

    // Creates booking after applying validation and business rules.
    suspend fun createBooking(userId: UUID, request: CreateBookingRequest): BookingResponse = suspendTransaction {
        if (request.numberOfPeople !in 1..100) throw ValidationException("Number of people must be between 1 and 100")
        if (request.bookingDate.isBefore(java.time.LocalDate.now())) throw ValidationException("Booking date cannot be in the past")
        val service = serviceRow(request.serviceId)
        if (!service[TourismServicesTable.active]) throw ValidationException("This tourism service is not accepting bookings")
        val unitPrice = service[TourismServicesTable.priceFrom]
        val total = unitPrice?.multiply(BigDecimal.valueOf(request.numberOfPeople.toLong()))
        val id = UUID.randomUUID(); val now = now()
        BookingsTable.insert { r ->
            r[BookingsTable.id] = id; r[BookingsTable.userId] = userId; r[serviceId] = request.serviceId
            r[bookingDate] = request.bookingDate; r[numberOfPeople] = request.numberOfPeople; r[totalPrice] = total
            r[currency] = service[TourismServicesTable.currency]; r[status] = "PENDING"; r[paymentStatus] = "UNPAID"
            r[notes] = clean(request.notes); r[createdAt] = now; r[updatedAt] = now
        }
        createNotification(userId, "Booking received", "Your booking is pending confirmation.", "BOOKING")
        booking(BookingsTable.selectAll().where { BookingsTable.id eq id }.single())
    }

    // Removes or invalidates booking after enforcing ownership and authorization rules.
    suspend fun cancelBooking(userId: UUID, id: UUID): BookingResponse = suspendTransaction {
        val row = bookingRow(id)
        if (row[BookingsTable.userId] != userId) throw ForbiddenException("You can only cancel your own booking")
        if (row[BookingsTable.status] in setOf("CANCELLED", "COMPLETED")) throw ValidationException("This booking cannot be cancelled")
        BookingsTable.update({ BookingsTable.id eq id }) { it[status] = "CANCELLED"; it[updatedAt] = now() }
        createNotification(userId, "Booking cancelled", "Your booking has been cancelled.", "BOOKING")
        bookingRow(id).let { booking(it) }
    }

    // Updates booking status while keeping related state consistent.
    suspend fun updateBookingStatus(adminId: UUID, id: UUID, request: UpdateBookingStatusRequest): BookingResponse = suspendTransaction {
        val status = request.status.trim().uppercase()
        if (status !in setOf("PENDING", "CONFIRMED", "CANCELLED", "COMPLETED")) throw ValidationException("Invalid booking status")
        val row = bookingRow(id)
        BookingsTable.update({ BookingsTable.id eq id }) { it[BookingsTable.status] = status; it[updatedAt] = now() }
        createNotification(row[BookingsTable.userId], "Booking status updated", "Your booking is now $status.", "BOOKING")
        bookingRow(id).let { booking(it) }
    }

    // Coordinates the notifications business workflow for callers.
    suspend fun notifications(userId: UUID): List<NotificationResponse> = suspendTransaction {
        NotificationsTable.selectAll().where { NotificationsTable.userId eq userId }
            .orderBy(NotificationsTable.createdAt to SortOrder.DESC).map { it.toNotification() }
    }
    // Updates notification read while keeping related state consistent.
    suspend fun markNotificationRead(userId: UUID, id: UUID): NotificationResponse = suspendTransaction {
        val row = NotificationsTable.selectAll().where { (NotificationsTable.id eq id) and (NotificationsTable.userId eq userId) }.singleOrNull()
            ?: throw NotFoundException("Notification not found")
        NotificationsTable.update({ NotificationsTable.id eq id }) { it[isRead] = true }
        NotificationsTable.selectAll().where { NotificationsTable.id eq id }.single().toNotification()
    }
    // Updates all notifications read while keeping related state consistent.
    suspend fun markAllNotificationsRead(userId: UUID) = suspendTransaction {
        NotificationsTable.update({ NotificationsTable.userId eq userId }) { it[isRead] = true }; Unit
    }

    // Coordinates the admin statistics business workflow for callers.
    suspend fun adminStatistics(): AdminStatisticsResponse = suspendTransaction {
        AdminStatisticsResponse(
            UsersTable.selectAll().count(), DestinationsTable.selectAll().count(), ReviewsTable.selectAll().count(),
            TourismServicesTable.selectAll().count(), BookingsTable.selectAll().count(),
            BookingsTable.selectAll().where { BookingsTable.status eq "PENDING" }.count()
        )
    }
    // Coordinates the admin users business workflow for callers.
    suspend fun adminUsers(): List<AdminUserResponse> = suspendTransaction {
        UsersTable.selectAll().orderBy(UsersTable.createdAt to SortOrder.DESC).map {
            AdminUserResponse(it[UsersTable.id], it[UsersTable.firstName], it[UsersTable.lastName], it[UsersTable.email], it[UsersTable.role], it[UsersTable.createdAt].toInstant())
        }
    }
    // Updates user role while keeping related state consistent.
    suspend fun updateUserRole(id: UUID, request: AdminRoleUpdateRequest): AdminUserResponse = suspendTransaction {
        val role = request.role.trim().uppercase()
        if (role !in setOf("USER", "ADMIN", "TOUR_GUIDE", "BUSINESS_OWNER")) throw ValidationException("Invalid user role")
        if (!UsersTable.selectAll().where { UsersTable.id eq id }.any()) throw NotFoundException("User not found")
        UsersTable.update({ UsersTable.id eq id }) { it[UsersTable.role] = role; it[updatedAt] = now() }
        UsersTable.selectAll().where { UsersTable.id eq id }.single().let {
            AdminUserResponse(it[UsersTable.id], it[UsersTable.firstName], it[UsersTable.lastName], it[UsersTable.email], it[UsersTable.role], it[UsersTable.createdAt].toInstant())
        }
    }

    // Validates service and stops the workflow when input is invalid.
    private fun validateService(name: String, type: String, email: String?, website: String?, price: Double?, currency: String) {
        if (name.isBlank() || name.trim().length > 160) throw ValidationException("Service name is required and must not exceed 160 characters")
        if (type.trim().uppercase() !in setOf("HOTEL", "RESTAURANT", "TOUR", "TRANSPORT", "GUIDE", "ACTIVITY")) throw ValidationException("Invalid service type")
        if (email != null && !email.matches(Regex("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$"))) throw ValidationException("Email address is invalid")
        if (website != null && !(website.startsWith("http://") || website.startsWith("https://"))) throw ValidationException("Website URL must start with http:// or https://")
        if (price != null && price < 0) throw ValidationException("Price cannot be negative")
        if (!currency.matches(Regex("^[A-Za-z]{3}$"))) throw ValidationException("Currency must be a three-letter code")
    }
    // Validates destination and stops the workflow when input is invalid.
    private fun ensureDestination(id: UUID) { if (!DestinationsTable.selectAll().where { DestinationsTable.id eq id }.any()) throw NotFoundException("Destination not found") }
    // Coordinates the service row business workflow for callers.
    private fun serviceRow(id: UUID) = TourismServicesTable.selectAll().where { TourismServicesTable.id eq id }.singleOrNull() ?: throw NotFoundException("Tourism service not found")
    // Coordinates the booking row business workflow for callers.
    private fun bookingRow(id: UUID) = BookingsTable.selectAll().where { BookingsTable.id eq id }.singleOrNull() ?: throw NotFoundException("Booking not found")
    // Validates owner or admin and stops the workflow when input is invalid.
    private fun ensureOwnerOrAdmin(userId: UUID, role: String, ownerId: UUID?) { if (role != "ADMIN" && ownerId != userId) throw ForbiddenException("You do not manage this tourism service") }
    // Creates notification after applying validation and business rules.
    private fun createNotification(userId: UUID, title: String, message: String, type: String) { NotificationsTable.insert { r -> r[id] = UUID.randomUUID(); r[NotificationsTable.userId] = userId; r[NotificationsTable.title] = title; r[NotificationsTable.message] = message; r[NotificationsTable.type] = type; r[isRead] = false; r[createdAt] = now() } }
    // Coordinates the booking business workflow for callers.
    private fun booking(row: ResultRow) = BookingResponse(row[BookingsTable.id], row[BookingsTable.userId], serviceRow(row[BookingsTable.serviceId]).toService(), row[BookingsTable.bookingDate], row[BookingsTable.numberOfPeople], row[BookingsTable.totalPrice]?.toDouble(), row[BookingsTable.currency], row[BookingsTable.status], row[BookingsTable.paymentStatus], row[BookingsTable.notes], row[BookingsTable.createdAt].toInstant(), row[BookingsTable.updatedAt].toInstant())
    // Coordinates the result row business workflow for callers.
    private fun ResultRow.toService() = TourismServiceResponse(this[TourismServicesTable.id], this[TourismServicesTable.ownerUserId], this[TourismServicesTable.destinationId], this[TourismServicesTable.name], this[TourismServicesTable.serviceType], this[TourismServicesTable.description], this[TourismServicesTable.phone], this[TourismServicesTable.email], this[TourismServicesTable.websiteUrl], this[TourismServicesTable.address], this[TourismServicesTable.priceFrom]?.toDouble(), this[TourismServicesTable.currency], this[TourismServicesTable.active], this[TourismServicesTable.createdAt].toInstant(), this[TourismServicesTable.updatedAt].toInstant())
    // Coordinates the result row business workflow for callers.
    private fun ResultRow.toNotification() = NotificationResponse(this[NotificationsTable.id], this[NotificationsTable.title], this[NotificationsTable.message], this[NotificationsTable.type], this[NotificationsTable.isRead], this[NotificationsTable.createdAt].toInstant())
    // Converts the supplied values into the clean form required by the domain model.
    private fun clean(v: String?) = v?.trim()?.takeIf { it.isNotEmpty() }
    // Coordinates the now business workflow for callers.
    private fun now() = OffsetDateTime.now(ZoneOffset.UTC)
}
