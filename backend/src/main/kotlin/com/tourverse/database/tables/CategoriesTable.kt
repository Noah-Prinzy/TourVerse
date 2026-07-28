package com.tourverse.database.tables

import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.java.javaUUID
import org.jetbrains.exposed.v1.javatime.timestampWithTimeZone

object CategoriesTable : Table("categories") {
    val id = javaUUID("id")
    val name = varchar("name", 80)
    val slug = varchar("slug", 90)
    val description = text("description").nullable()
    val iconUrl = text("icon_url").nullable()
    val active = bool("active")
    val createdAt = timestampWithTimeZone("created_at")
    val updatedAt = timestampWithTimeZone("updated_at")
    override val primaryKey = PrimaryKey(id)
}
