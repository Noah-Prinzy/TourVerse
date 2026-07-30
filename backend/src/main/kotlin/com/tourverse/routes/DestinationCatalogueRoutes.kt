package com.tourverse.routes

import com.tourverse.dto.ApiMessage
import com.tourverse.models.*
import com.tourverse.security.authenticatedUser
import com.tourverse.services.CountryCodeService
import com.tourverse.services.DestinationCatalogueService
import com.tourverse.services.GooglePlaceLinkService
import com.tourverse.utils.ValidationException
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.*
import java.util.UUID

// Registers the route endpoints and delegates each request to its service layer.
fun Route.destinationCatalogueRoutes(
    catalogue: DestinationCatalogueService,
    googlePlaces: GooglePlaceLinkService
) {
    route("/api/admin/catalogue") {
        post("/sync") {
            val admin = call.authenticatedUser("ADMIN")
            call.respond(HttpStatusCode.Created, catalogue.sync(
                admin.id,
                call.receive<CatalogueSyncRequest>()
            ))
        }
        post("/sync/country/{countryCode}") {
            val admin = call.authenticatedUser("ADMIN")
            val code = CountryCodeService.normalizeCode(call.parameters["countryCode"])!!
            call.respond(HttpStatusCode.Created, catalogue.sync(
                admin.id,
                CatalogueSyncRequest(countryCode = code)
            ))
        }
        post("/refresh/{destinationId}") {
            call.authenticatedUser("ADMIN")
            call.respond(catalogue.requestRefresh(catalogueUuid(call.parameters["destinationId"])))
        }
        get("/sync-jobs") {
            call.authenticatedUser("ADMIN")
            call.respond(catalogue.jobs())
        }
        get("/stale") {
            call.authenticatedUser("ADMIN")
            call.respond(catalogue.stale())
        }
        get("/destinations/{destinationId}/sources") {
            call.authenticatedUser("ADMIN")
            call.respond(catalogue.sources(catalogueUuid(call.parameters["destinationId"])))
        }
    }

    route("/api/admin/destinations/{destinationId}/google-place") {
        post("/search") {
            call.authenticatedUser("ADMIN")
            call.respond(googlePlaces.search(
                catalogueUuid(call.parameters["destinationId"]),
                call.receive<GooglePlaceSearchRequest>()
            ))
        }
        put {
            val admin = call.authenticatedUser("ADMIN")
            call.respond(googlePlaces.link(
                catalogueUuid(call.parameters["destinationId"]),
                admin.id,
                call.receive<LinkGooglePlaceRequest>()
            ))
        }
        delete {
            call.authenticatedUser("ADMIN")
            googlePlaces.remove(catalogueUuid(call.parameters["destinationId"]))
            call.respond(ApiMessage("success", "Google Place link removed."))
        }
    }
}

// Encapsulates the catalogue uuid operation behind a reusable function.
private fun catalogueUuid(value: String?): UUID =
    value?.let { runCatching { UUID.fromString(it) }.getOrNull() }
        ?: throw ValidationException("Destination ID must be a valid UUID.")
