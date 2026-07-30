package com.tourverse.security

import com.tourverse.exceptions.ForbiddenException
import com.tourverse.exceptions.UnauthorizedException
import io.ktor.http.HttpHeaders
import io.ktor.server.application.ApplicationCall
import java.util.UUID

data class AuthenticatedUser(val id: UUID, val role: String)

// Encapsulates the application call operation behind a reusable function.
fun ApplicationCall.authenticatedUser(vararg allowedRoles: String): AuthenticatedUser {
    val authorization = request.headers[HttpHeaders.Authorization]
        ?: throw UnauthorizedException()
    val token = authorization.removePrefix("Bearer ").takeIf { it != authorization }
        ?: throw UnauthorizedException("Authorization header must use the Bearer scheme")
    val claims = TokenService.verifyAccessToken(token)
        ?: throw UnauthorizedException("Invalid or expired access token")
    if (allowedRoles.isNotEmpty() && claims.role !in allowedRoles) {
        throw ForbiddenException()
    }
    val userId = runCatching { UUID.fromString(claims.userId) }
        .getOrElse { throw UnauthorizedException("Invalid access token") }
    return AuthenticatedUser(userId, claims.role)
}
