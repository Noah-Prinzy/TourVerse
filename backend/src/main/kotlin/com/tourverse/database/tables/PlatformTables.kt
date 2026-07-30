package com.tourverse.database.tables

import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.java.javaUUID
import org.jetbrains.exposed.v1.javatime.date
import org.jetbrains.exposed.v1.javatime.timestampWithTimeZone

// Provides shared tourism services table behavior without requiring callers to create an instance.
object TourismServicesTable : Table("tourism_services") {
    val id = javaUUID("id")
    val ownerUserId = javaUUID("owner_user_id").references(UsersTable.id).nullable()
    val destinationId = javaUUID("destination_id").references(DestinationsTable.id).nullable()
    val name = varchar("name", 160)
    val serviceType = varchar("service_type", 40)
    val description = text("description").nullable()
    val phone = varchar("phone", 40).nullable()
    val email = varchar("email", 255).nullable()
    val websiteUrl = text("website_url").nullable()
    val address = text("address").nullable()
    val priceFrom = decimal("price_from", 12, 2).nullable()
    val currency = varchar("currency", 3)
    val active = bool("active")
    val createdAt = timestampWithTimeZone("created_at")
    val updatedAt = timestampWithTimeZone("updated_at")
    override val primaryKey = PrimaryKey(id)
}

// Provides shared bookings table behavior without requiring callers to create an instance.
object BookingsTable : Table("bookings") {
    val id = javaUUID("id")
    val userId = javaUUID("user_id").references(UsersTable.id)
    val serviceId = javaUUID("service_id").references(TourismServicesTable.id)
    val bookingDate = date("booking_date")
    val numberOfPeople = integer("number_of_people")
    val totalPrice = decimal("total_price", 12, 2).nullable()
    val currency = varchar("currency", 3)
    val status = varchar("status", 30)
    val paymentStatus = varchar("payment_status", 30)
    val notes = text("notes").nullable()
    val createdAt = timestampWithTimeZone("created_at")
    val updatedAt = timestampWithTimeZone("updated_at")
    override val primaryKey = PrimaryKey(id)
}

// Provides shared notifications table behavior without requiring callers to create an instance.
object NotificationsTable : Table("notifications") {
    val id = javaUUID("id")
    val userId = javaUUID("user_id").references(UsersTable.id)
    val title = varchar("title", 160)
    val message = text("message")
    val type = varchar("type", 40)
    val isRead = bool("is_read")
    val createdAt = timestampWithTimeZone("created_at")
    override val primaryKey = PrimaryKey(id)
}
