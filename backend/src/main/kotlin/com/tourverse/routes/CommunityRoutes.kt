package com.tourverse.routes

import com.tourverse.dto.ApiMessage
import com.tourverse.models.*
import com.tourverse.security.authenticatedUser
import com.tourverse.services.CommunityService
import com.tourverse.utils.ValidationException
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.*
import java.util.UUID

fun Route.communityRoutes(service: CommunityService) {
    route("/api/destinations/{destinationId}/reviews") {
        get { call.respond(service.destinationReviews(call.uuid("destinationId"))) }
        post { val u = call.authenticatedUser(); call.respond(HttpStatusCode.Created, service.createReview(u.id, call.uuid("destinationId"), call.receive<CreateReviewRequest>())) }
    }
    route("/api/reviews/{id}") {
        put { val u = call.authenticatedUser(); call.respond(service.updateReview(u.id, u.role, call.uuid("id"), call.receive<UpdateReviewRequest>())) }
        delete { val u = call.authenticatedUser(); service.deleteReview(u.id, u.role, call.uuid("id")); call.respond(ApiMessage("success", "Review deleted successfully.")) }
    }
    route("/api/favorites") {
        get { val u = call.authenticatedUser(); call.respond(service.favorites(u.id)) }
        post("/{destinationId}") { val u = call.authenticatedUser(); call.respond(HttpStatusCode.Created, service.addFavorite(u.id, call.uuid("destinationId"))) }
        delete("/{destinationId}") { val u = call.authenticatedUser(); service.removeFavorite(u.id, call.uuid("destinationId")); call.respond(ApiMessage("success", "Favorite removed successfully.")) }
    }
    route("/api/trips") {
        get { val u = call.authenticatedUser(); call.respond(service.trips(u.id)) }
        post { val u = call.authenticatedUser(); call.respond(HttpStatusCode.Created, service.createTrip(u.id, call.receive<CreateTripRequest>())) }
        route("/{id}") {
            get { val u = call.authenticatedUser(); call.respond(service.trip(u.id, call.uuid("id"))) }
            put { val u = call.authenticatedUser(); call.respond(service.updateTrip(u.id, call.uuid("id"), call.receive<UpdateTripRequest>())) }
            delete { val u = call.authenticatedUser(); service.deleteTrip(u.id, call.uuid("id")); call.respond(ApiMessage("success", "Trip deleted successfully.")) }
            post("/destinations") { val u = call.authenticatedUser(); call.respond(HttpStatusCode.Created, service.addTripDestination(u.id, call.uuid("id"), call.receive<AddTripDestinationRequest>())) }
            delete("/destinations/{destinationId}") { val u = call.authenticatedUser(); call.respond(service.removeTripDestination(u.id, call.uuid("id"), call.uuid("destinationId"))) }
        }
    }
}

private fun io.ktor.server.application.ApplicationCall.uuid(name: String): UUID =
    parameters[name]?.let { runCatching { UUID.fromString(it) }.getOrNull() }
        ?: throw ValidationException("$name must be a valid UUID")
