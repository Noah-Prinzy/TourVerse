package com.tourverse.services

import com.tourverse.database.tables.RefreshTokensTable
import com.tourverse.database.tables.UsersTable
import com.tourverse.exceptions.ConflictException
import com.tourverse.exceptions.NotFoundException
import com.tourverse.exceptions.UnauthorizedException
import com.tourverse.models.*
import com.tourverse.security.PasswordHasher
import com.tourverse.security.TokenService
import com.tourverse.utils.ValidationException
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.suspendTransaction
import org.jetbrains.exposed.v1.jdbc.update
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.UUID

class AuthService {
    private val refreshLifetimeDays = 30L

    suspend fun register(request: RegisterRequest): AuthResponse = suspendTransaction {
        AuthValidator.validateRegistration(request)
        val normalizedEmail = request.email.trim().lowercase()
        if (UsersTable.selectAll().where { UsersTable.email eq normalizedEmail }.any()) {
            throw ConflictException("An account with this email already exists")
        }
        val userId = UUID.randomUUID()
        val now = OffsetDateTime.now(ZoneOffset.UTC)
        UsersTable.insert { statement ->
            statement[id] = userId
            statement[firstName] = request.firstName.trim()
            statement[lastName] = request.lastName.trim()
            statement[email] = normalizedEmail
            statement[passwordHash] = PasswordHasher.hash(request.password)
            statement[role] = "USER"
            statement[createdAt] = now
            statement[updatedAt] = now
        }
        issueTokens(loadUser(userId))
    }

    suspend fun login(request: LoginRequest): AuthResponse = suspendTransaction {
        val normalizedEmail = request.email.trim().lowercase()
        val row = UsersTable.selectAll()
            .where { UsersTable.email eq normalizedEmail }
            .singleOrNull()
            ?: throw UnauthorizedException("Incorrect email or password")
        if (!PasswordHasher.verify(request.password, row[UsersTable.passwordHash])) {
            throw UnauthorizedException("Incorrect email or password")
        }
        issueTokens(row.toUser())
    }

    suspend fun refresh(request: RefreshTokenRequest): AuthResponse = suspendTransaction {
        val now = OffsetDateTime.now(ZoneOffset.UTC)
        val tokenHash = TokenService.hashRefreshToken(request.refreshToken)
        val tokenRow = RefreshTokensTable.selectAll().where {
            (RefreshTokensTable.tokenHash eq tokenHash) and
                (RefreshTokensTable.revokedAt eq null)
        }.singleOrNull() ?: throw UnauthorizedException("Invalid refresh token")

        if (!tokenRow[RefreshTokensTable.expiresAt].isAfter(now)) {
            throw UnauthorizedException("Refresh token has expired")
        }

        RefreshTokensTable.update({ RefreshTokensTable.id eq tokenRow[RefreshTokensTable.id] }) {
            it[revokedAt] = now
        }
        issueTokens(loadUser(tokenRow[RefreshTokensTable.userId]))
    }

    suspend fun logout(request: LogoutRequest) = suspendTransaction {
        val tokenHash = TokenService.hashRefreshToken(request.refreshToken)
        RefreshTokensTable.update({ RefreshTokensTable.tokenHash eq tokenHash }) {
            it[revokedAt] = OffsetDateTime.now(ZoneOffset.UTC)
        }
        Unit
    }

    suspend fun getCurrentUser(userId: UUID): UserResponse = suspendTransaction { loadUser(userId) }

    suspend fun updateProfile(userId: UUID, request: UpdateProfileRequest): UserResponse = suspendTransaction {
        AuthValidator.validateProfile(request)
        val updatedRows = UsersTable.update({ UsersTable.id eq userId }) { statement ->
            request.firstName?.let { statement[firstName] = it.trim() }
            request.lastName?.let { statement[lastName] = it.trim() }
            request.bio?.let { statement[bio] = it.trim().takeIf(String::isNotEmpty) }
            request.profileImageUrl?.let { statement[profileImageUrl] = it.trim().takeIf(String::isNotEmpty) }
            statement[updatedAt] = OffsetDateTime.now(ZoneOffset.UTC)
        }
        if (updatedRows == 0) throw NotFoundException("User not found")
        loadUser(userId)
    }

    suspend fun changePassword(userId: UUID, request: ChangePasswordRequest) = suspendTransaction {
        val user = UsersTable.selectAll()
            .where { UsersTable.id eq userId }
            .singleOrNull()
            ?: throw NotFoundException("User not found")

        if (!PasswordHasher.verify(request.currentPassword, user[UsersTable.passwordHash])) {
            throw UnauthorizedException("Current password is incorrect")
        }
        if (request.currentPassword == request.newPassword) {
            throw ValidationException("The new password must be different from the current password")
        }
        AuthValidator.validatePassword(request.newPassword)

        UsersTable.update({ UsersTable.id eq userId }) {
            it[passwordHash] = PasswordHasher.hash(request.newPassword)
            it[updatedAt] = OffsetDateTime.now(ZoneOffset.UTC)
        }
        RefreshTokensTable.deleteWhere { RefreshTokensTable.userId eq userId }
        Unit
    }

    suspend fun revokeAllSessions(userId: UUID) = suspendTransaction {
        RefreshTokensTable.deleteWhere { RefreshTokensTable.userId eq userId }
        Unit
    }

    private fun issueTokens(user: UserResponse): AuthResponse {
        val access = TokenService.createAccessToken(user.id, user.role)
        val refreshToken = TokenService.createRefreshToken()
        val now = OffsetDateTime.now(ZoneOffset.UTC)
        RefreshTokensTable.insert { statement ->
            statement[id] = UUID.randomUUID()
            statement[userId] = user.id
            statement[tokenHash] = TokenService.hashRefreshToken(refreshToken)
            statement[expiresAt] = now.plusDays(refreshLifetimeDays)
            statement[revokedAt] = null
            statement[createdAt] = now
        }
        return AuthResponse(access.token, refreshToken, expiresInSeconds = access.expiresInSeconds, user = user)
    }

    private fun loadUser(userId: UUID): UserResponse =
        UsersTable.selectAll().where { UsersTable.id eq userId }.singleOrNull()?.toUser()
            ?: throw NotFoundException("User not found")

    private fun ResultRow.toUser() = UserResponse(
        id = this[UsersTable.id],
        firstName = this[UsersTable.firstName],
        lastName = this[UsersTable.lastName],
        email = this[UsersTable.email],
        profileImageUrl = this[UsersTable.profileImageUrl],
        bio = this[UsersTable.bio],
        role = this[UsersTable.role],
        createdAt = this[UsersTable.createdAt].toInstant()
    )
}
