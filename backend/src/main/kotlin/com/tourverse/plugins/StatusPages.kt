package com.tourverse.plugins

import com.tourverse.dto.ApiMessage
import com.tourverse.exceptions.*
import com.tourverse.utils.ValidationException
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.*
import io.ktor.server.plugins.BadRequestException
import io.ktor.server.plugins.ContentTransformationException
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.response.respond

fun Application.configureStatusPages() {
    install(StatusPages) {
        exception<ValidationException> { call, cause ->
            call.respond(HttpStatusCode.BadRequest, ApiMessage("error", cause.message ?: "Invalid request."))
        }
        exception<ContentTransformationException> { call, _ ->
            call.respond(HttpStatusCode.BadRequest, ApiMessage("error", "Request body contains invalid or missing JSON fields."))
        }
        exception<BadRequestException> { call, cause ->
            call.respond(HttpStatusCode.BadRequest, ApiMessage("error", cause.message ?: "Invalid request."))
        }
        exception<UnauthorizedException> { call, cause ->
            call.respond(HttpStatusCode.Unauthorized, ApiMessage("error", cause.message ?: "Unauthorized"))
        }
        exception<ForbiddenException> { call, cause ->
            call.respond(HttpStatusCode.Forbidden, ApiMessage("error", cause.message ?: "Forbidden"))
        }
        exception<NotFoundException> { call, cause ->
            call.respond(HttpStatusCode.NotFound, ApiMessage("error", cause.message ?: "Not found"))
        }
        exception<ConflictException> { call, cause ->
            call.respond(HttpStatusCode.Conflict, ApiMessage("error", cause.message ?: "Conflict"))
        }
        exception<ProviderNotConfiguredException> { call, cause ->
            call.respond(HttpStatusCode.ServiceUnavailable, ApiMessage(
                "PROVIDER_NOT_CONFIGURED",
                cause.message ?: "Provider is not configured."
            ))
        }
        exception<Throwable> { call, cause ->
            this@configureStatusPages.log.error("Unhandled server error", cause)
            call.respond(HttpStatusCode.InternalServerError, ApiMessage("error", "An unexpected server error occurred."))
        }
    }
}
