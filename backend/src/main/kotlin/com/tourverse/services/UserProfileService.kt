package com.tourverse.services

import com.tourverse.database.tables.RefreshTokensTable
import com.tourverse.database.tables.UsersTable
import com.tourverse.exceptions.NotFoundException
import com.tourverse.exceptions.UnauthorizedException
import com.tourverse.models.*
import com.tourverse.security.PasswordHasher
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.suspendTransaction
import org.jetbrains.exposed.v1.jdbc.update
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.UUID

class UserProfileService {
    suspend fun get(userId: UUID): UserProfileResponse = suspendTransaction { load(userId) }

    suspend fun update(userId: UUID, request: UpdateUserProfileRequest): UserProfileResponse = suspendTransaction {
        ProfileValidator.validate(request)
        val changed = UsersTable.update({ UsersTable.id eq userId }) { row ->
            request.firstName?.let { row[firstName] = it.trim() }
            request.lastName?.let { row[lastName] = it.trim() }
            request.bio?.let { row[bio] = it.trim().takeIf(String::isNotEmpty) }
            request.nationality?.let { row[nationality] = it.trim().takeIf(String::isNotEmpty) }
            request.travelInterests?.let { row[travelInterests] = encodeInterests(normalizeInterests(it)) }
            request.profilePublic?.let { row[profilePublic] = it }
            row[updatedAt] = OffsetDateTime.now(ZoneOffset.UTC)
        }
        if (changed == 0) throw NotFoundException("User not found")
        load(userId)
    }

    suspend fun updateImage(userId: UUID, request: UpdateProfileImageRequest): UserProfileResponse = suspendTransaction {
        ProfileValidator.validateImage(request)
        val changed = UsersTable.update({ UsersTable.id eq userId }) { row ->
            row[profileImageUrl] = request.profileImageUrl?.trim()?.takeIf(String::isNotEmpty)
            row[updatedAt] = OffsetDateTime.now(ZoneOffset.UTC)
        }
        if (changed == 0) throw NotFoundException("User not found")
        load(userId)
    }

    suspend fun deleteAccount(userId: UUID, request: DeleteAccountRequest) = suspendTransaction {
        val user = UsersTable.selectAll().where { UsersTable.id eq userId }.singleOrNull()
            ?: throw NotFoundException("User not found")
        if (!PasswordHasher.verify(request.password, user[UsersTable.passwordHash])) {
            throw UnauthorizedException("Password is incorrect")
        }
        RefreshTokensTable.deleteWhere { RefreshTokensTable.userId eq userId }
        UsersTable.deleteWhere { UsersTable.id eq userId }
        Unit
    }

    private fun load(id: UUID): UserProfileResponse = UsersTable.selectAll().where { UsersTable.id eq id }
        .singleOrNull()?.toProfile() ?: throw NotFoundException("User not found")

    private fun normalizeInterests(values: List<String>) = values.map(String::trim).filter(String::isNotEmpty).distinctBy(String::lowercase)
    private fun encodeInterests(values: List<String>) = values.joinToString("\n")
    private fun decodeInterests(value: String) = value.lines().map(String::trim).filter(String::isNotEmpty)

    private fun ResultRow.toProfile() = UserProfileResponse(
        id = this[UsersTable.id], firstName = this[UsersTable.firstName], lastName = this[UsersTable.lastName],
        email = this[UsersTable.email], profileImageUrl = this[UsersTable.profileImageUrl], bio = this[UsersTable.bio],
        nationality = this[UsersTable.nationality], travelInterests = decodeInterests(this[UsersTable.travelInterests]),
        profilePublic = this[UsersTable.profilePublic], role = this[UsersTable.role],
        createdAt = this[UsersTable.createdAt].toInstant(), updatedAt = this[UsersTable.updatedAt].toInstant()
    )
}
