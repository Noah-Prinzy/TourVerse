package com.tourverse.database.tables

import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.java.javaUUID
import org.jetbrains.exposed.v1.javatime.date
import org.jetbrains.exposed.v1.javatime.timestampWithTimeZone

object ReviewsTable : Table("reviews") {
    val id = javaUUID("id")
    val userId = javaUUID("user_id").references(UsersTable.id)
    val destinationId = javaUUID("destination_id").references(DestinationsTable.id)
    val rating = integer("rating")
    val comment = text("comment").nullable()
    val createdAt = timestampWithTimeZone("created_at")
    val updatedAt = timestampWithTimeZone("updated_at")
    override val primaryKey = PrimaryKey(id)
}

object FavoritesTable : Table("favorites") {
    val id = javaUUID("id")
    val userId = javaUUID("user_id").references(UsersTable.id)
    val destinationId = javaUUID("destination_id").references(DestinationsTable.id)
    val createdAt = timestampWithTimeZone("created_at")
    override val primaryKey = PrimaryKey(id)
}

object TripsTable : Table("trips") {
    val id = javaUUID("id")
    val userId = javaUUID("user_id").references(UsersTable.id)
    val title = varchar("title", 150)
    val description = text("description").nullable()
    val startDate = date("start_date").nullable()
    val endDate = date("end_date").nullable()
    val createdAt = timestampWithTimeZone("created_at")
    val updatedAt = timestampWithTimeZone("updated_at")
    override val primaryKey = PrimaryKey(id)
}

object TripDestinationsTable : Table("trip_destinations") {
    val id = javaUUID("id")
    val tripId = javaUUID("trip_id").references(TripsTable.id)
    val destinationId = javaUUID("destination_id").references(DestinationsTable.id)
    val visitDate = date("visit_date").nullable()
    val notes = text("notes").nullable()
    val displayOrder = integer("display_order")
    val createdAt = timestampWithTimeZone("created_at")
    override val primaryKey = PrimaryKey(id)
}
