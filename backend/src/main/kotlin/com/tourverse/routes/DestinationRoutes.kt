package com.tourverse.routes

import com.tourverse.dto.ApiMessage
import com.tourverse.models.CreateDestinationRequest
import com.tourverse.models.DestinationQuery
import com.tourverse.models.DestinationSortField
import com.tourverse.models.SortDirection
import com.tourverse.models.UpdateDestinationRequest
import com.tourverse.security.authenticatedUser
import com.tourverse.services.DestinationService
import com.tourverse.utils.ValidationException
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.ktor.server.routing.route
import java.util.UUID

fun Route.destinationRoutes(service: DestinationService) {
    route("/api/destinations") {
        get {
            val query = parseDestinationQuery(
                search = call.request.queryParameters["search"],
                country = call.request.queryParameters["country"],
                city = call.request.queryParameters["city"],
                category = call.request.queryParameters["category"],
                pageValue = call.request.queryParameters["page"],
                sizeValue = call.request.queryParameters["size"],
                sortByValue = call.request.queryParameters["sortBy"],
                sortDirectionValue = call.request.queryParameters["sortDirection"]
            )
            call.respond(service.getAllDestinations(query))
        }

        get("/{id}") {
            val id = parseUuid(call.parameters["id"])
            val destination = service.getDestinationById(id)

            if (destination == null) {
                call.respond(
                    HttpStatusCode.NotFound,
                    ApiMessage("error", "Destination not found.")
                )
                return@get
            }

            call.respond(destination)
        }

        post {
            call.authenticatedUser("ADMIN")
            val request = call.receive<CreateDestinationRequest>()
            val createdDestination = service.createDestination(request)
            call.respond(HttpStatusCode.Created, createdDestination)
        }

        put("/{id}") {
            call.authenticatedUser("ADMIN")
            val id = parseUuid(call.parameters["id"])
            val request = call.receive<UpdateDestinationRequest>()
            val updatedDestination = service.updateDestination(id, request)

            if (updatedDestination == null) {
                call.respond(
                    HttpStatusCode.NotFound,
                    ApiMessage("error", "Destination not found.")
                )
                return@put
            }

            call.respond(updatedDestination)
        }

        delete("/{id}") {
            call.authenticatedUser("ADMIN")
            val id = parseUuid(call.parameters["id"])
            val deleted = service.deleteDestination(id)

            if (!deleted) {
                call.respond(
                    HttpStatusCode.NotFound,
                    ApiMessage("error", "Destination not found.")
                )
                return@delete
            }

            call.respond(
                HttpStatusCode.OK,
                ApiMessage("success", "Destination deleted successfully.")
            )
        }
    }
}

internal fun parseUuid(value: String?): UUID {
    return value
        ?.let { runCatching { UUID.fromString(it) }.getOrNull() }
        ?: throw ValidationException("Destination ID must be a valid UUID.")
}

internal fun parseDestinationQuery(
    search: String?,
    country: String?,
    city: String?,
    category: String?,
    pageValue: String?,
    sizeValue: String?,
    sortByValue: String?,
    sortDirectionValue: String?
): DestinationQuery {
    val page = pageValue?.toIntOrNull() ?: 1
    val size = sizeValue?.toIntOrNull() ?: 20

    if (page < 1) {
        throw ValidationException("Page must be at least 1.")
    }
    if (size !in 1..100) {
        throw ValidationException("Size must be between 1 and 100.")
    }

    val sortBy = when (sortByValue?.trim()?.lowercase()) {
        null, "", "createdat", "created_at" -> DestinationSortField.CREATED_AT
        "name" -> DestinationSortField.NAME
        "country" -> DestinationSortField.COUNTRY
        "city" -> DestinationSortField.CITY
        "category" -> DestinationSortField.CATEGORY
        "updatedat", "updated_at" -> DestinationSortField.UPDATED_AT
        else -> throw ValidationException(
            "sortBy must be one of: name, country, city, category, createdAt, updatedAt."
        )
    }

    val sortDirection = when (sortDirectionValue?.trim()?.lowercase()) {
        null, "", "desc" -> SortDirection.DESC
        "asc" -> SortDirection.ASC
        else -> throw ValidationException("sortDirection must be asc or desc.")
    }

    return DestinationQuery(
        search = search,
        country = country,
        city = city,
        category = category,
        page = page,
        size = size,
        sortBy = sortBy,
        sortDirection = sortDirection
    )
}
