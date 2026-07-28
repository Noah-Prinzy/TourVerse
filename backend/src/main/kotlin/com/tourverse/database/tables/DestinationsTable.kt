package com.tourverse.database.tables

import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.java.javaUUID
import org.jetbrains.exposed.v1.javatime.timestampWithTimeZone

object DestinationsTable : Table("destinations") {

    val id = javaUUID("id").databaseGenerated()

    val name = varchar("name", 150)

    val country = varchar("country", 100)

    val city = varchar("city", 100).nullable()

    val description = text("description")

    val category = varchar("category", 80)

    val latitude = decimal(
        name = "latitude",
        precision = 9,
        scale = 6
    ).nullable()

    val longitude = decimal(
        name = "longitude",
        precision = 9,
        scale = 6
    ).nullable()

    val coverImageUrl = text("cover_image_url").nullable()

    val createdAt = timestampWithTimeZone("created_at")
        .databaseGenerated()

    val updatedAt = timestampWithTimeZone("updated_at")
        .databaseGenerated()

    override val primaryKey = PrimaryKey(id)
}