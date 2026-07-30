package com.tourverse.database.tables

import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.java.javaUUID
import org.jetbrains.exposed.v1.javatime.timestampWithTimeZone

// Provides shared users table behavior without requiring callers to create an instance.
object UsersTable : Table("users") {
    val id = javaUUID("id")
    val firstName = varchar("first_name", 80)
    val lastName = varchar("last_name", 80)
    val email = varchar("email", 255)
    val passwordHash = text("password_hash")
    val profileImageUrl = text("profile_image_url").nullable()
    val bio = text("bio").nullable()
    val nationality = varchar("nationality", 100).nullable()
    val travelInterests = text("travel_interests")
    val profilePublic = bool("profile_public")
    val role = varchar("role", 30)
    val createdAt = timestampWithTimeZone("created_at")
    val updatedAt = timestampWithTimeZone("updated_at")
    override val primaryKey = PrimaryKey(id)
}

// Provides shared refresh tokens table behavior without requiring callers to create an instance.
object RefreshTokensTable : Table("refresh_tokens") {
    val id = javaUUID("id")
    val userId = javaUUID("user_id").references(UsersTable.id)
    val tokenHash = varchar("token_hash", 64)
    val expiresAt = timestampWithTimeZone("expires_at")
    val revokedAt = timestampWithTimeZone("revoked_at").nullable()
    val createdAt = timestampWithTimeZone("created_at")
    override val primaryKey = PrimaryKey(id)
}
