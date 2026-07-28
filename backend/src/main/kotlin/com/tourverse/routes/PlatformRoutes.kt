package com.tourverse.routes

import com.tourverse.dto.ApiMessage
import com.tourverse.models.*
import com.tourverse.security.authenticatedUser
import com.tourverse.services.PlatformService
import com.tourverse.utils.ValidationException
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.*
import java.util.UUID

fun Route.platformRoutes(service: PlatformService) {
    route("/api/services") {
        get {
            val type = call.request.queryParameters["type"]
            val destinationId = call.request.queryParameters["destinationId"]?.let(::parseUuid)
            call.respond(service.services(type, destinationId))
        }
        get("/{id}") { call.respond(service.service(call.platformUuid("id"))) }
        post {
            val user = call.authenticatedUser("ADMIN", "BUSINESS_OWNER", "TOUR_GUIDE")
            call.respond(HttpStatusCode.Created, service.createService(user.id, call.receive<CreateTourismServiceRequest>()))
        }
        put("/{id}") {
            val user = call.authenticatedUser("ADMIN", "BUSINESS_OWNER", "TOUR_GUIDE")
            call.respond(service.updateService(user.id, user.role, call.platformUuid("id"), call.receive<UpdateTourismServiceRequest>()))
        }
        delete("/{id}") {
            val user = call.authenticatedUser("ADMIN", "BUSINESS_OWNER", "TOUR_GUIDE")
            service.deleteService(user.id, user.role, call.platformUuid("id"))
            call.respond(ApiMessage("success", "Tourism service removed successfully."))
        }
    }

    route("/api/bookings") {
        get {
            val user = call.authenticatedUser()
            call.respond(service.bookings(user.id, user.role))
        }
        post {
            val user = call.authenticatedUser()
            call.respond(HttpStatusCode.Created, service.createBooking(user.id, call.receive<CreateBookingRequest>()))
        }
        put("/{id}/cancel") {
            val user = call.authenticatedUser()
            call.respond(service.cancelBooking(user.id, call.platformUuid("id")))
        }
    }

    route("/api/notifications") {
        get {
            val user = call.authenticatedUser()
            call.respond(service.notifications(user.id))
        }
        put("/{id}/read") {
            val user = call.authenticatedUser()
            call.respond(service.markNotificationRead(user.id, call.platformUuid("id")))
        }
        put("/read-all") {
            val user = call.authenticatedUser()
            service.markAllNotificationsRead(user.id)
            call.respond(ApiMessage("success", "All notifications marked as read."))
        }
    }

    route("/api/admin") {
        get("/statistics") {
            call.authenticatedUser("ADMIN")
            call.respond(service.adminStatistics())
        }
        get("/users") {
            call.authenticatedUser("ADMIN")
            call.respond(service.adminUsers())
        }
        put("/users/{id}/role") {
            call.authenticatedUser("ADMIN")
            call.respond(service.updateUserRole(call.platformUuid("id"), call.receive<AdminRoleUpdateRequest>()))
        }
        get("/services") {
            call.authenticatedUser("ADMIN")
            call.respond(service.services(null, null, includeInactive = true))
        }
        get("/bookings") {
            val admin = call.authenticatedUser("ADMIN")
            call.respond(service.bookings(admin.id, admin.role))
        }
        put("/bookings/{id}/status") {
            val admin = call.authenticatedUser("ADMIN")
            call.respond(service.updateBookingStatus(admin.id, call.platformUuid("id"), call.receive<UpdateBookingStatusRequest>()))
        }
    }
}

private fun io.ktor.server.application.ApplicationCall.platformUuid(name: String): UUID =
    parameters[name]?.let(::parseUuid) ?: throw ValidationException("$name must be a valid UUID")

private fun parseUuid(value: String): UUID =
    runCatching { UUID.fromString(value) }.getOrElse { throw ValidationException("Value must be a valid UUID") }
