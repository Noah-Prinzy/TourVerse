package com.tourverse.routes

import com.tourverse.dto.ApiMessage
import com.tourverse.services.DestinationService
import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.route

fun Route.destinationRoutes(service: DestinationService) {
    route("/api/destinations") {
        get {
            call.respond(service.getAllDestinations())
        }

        get("/{id}") {
            val id = call.parameters["id"]?.toIntOrNull()

            if (id == null) {
                call.respond(
                    HttpStatusCode.BadRequest,
                    ApiMessage("error", "Destination ID must be a number")
                )
                return@get
            }

            val destination = service.getDestinationById(id)

            if (destination == null) {
                call.respond(
                    HttpStatusCode.NotFound,
                    ApiMessage("error", "Destination not found")
                )
                return@get
            }

            call.respond(destination)
        }
    }
}
