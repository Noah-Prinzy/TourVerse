package com.tourverse.services

import com.tourverse.exceptions.NotFoundException
import com.tourverse.exceptions.ProviderNotConfiguredException
import com.tourverse.models.*
import com.tourverse.repositories.DestinationImportRepository
import com.tourverse.repositories.DestinationRepository
import com.tourverse.utils.AppEnvironment
import com.tourverse.utils.ValidationException
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.*
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import kotlinx.serialization.json.*
import java.util.UUID

interface GooglePlacesSearchClient {
    val configured: Boolean
    suspend fun search(
        name: String,
        latitude: Double,
        longitude: Double,
        limit: Int
    ): List<GooglePlaceSearchResult>
}

class GooglePlacesHttpClient(
    private val apiKey: String? = AppEnvironment.get("TOURVERSE_GOOGLE_PLACES_API_KEY"),
    private val client: HttpClient = HttpClient(CIO) {
        install(HttpTimeout) {
            requestTimeoutMillis = 10_000
            connectTimeoutMillis = 5_000
            socketTimeoutMillis = 10_000
        }
    }
) : GooglePlacesSearchClient {
    override val configured: Boolean = !apiKey.isNullOrBlank()

    override suspend fun search(
        name: String,
        latitude: Double,
        longitude: Double,
        limit: Int
    ): List<GooglePlaceSearchResult> {
        val key = apiKey ?: throw ProviderNotConfiguredException(
            "Google Places is not configured on the TourVerse backend."
        )
        val body = buildJsonObject {
            put("textQuery", name)
            put("maxResultCount", limit.coerceIn(1, 5))
            putJsonObject("locationBias") {
                putJsonObject("circle") {
                    putJsonObject("center") {
                        put("latitude", latitude)
                        put("longitude", longitude)
                    }
                    put("radius", 25_000.0)
                }
            }
        }
        val response = client.post("https://places.googleapis.com/v1/places:searchText") {
            contentType(ContentType.Application.Json)
            header("X-Goog-Api-Key", key)
            header(
                "X-Goog-FieldMask",
                "places.id,places.displayName,places.formattedAddress,places.location,places.googleMapsUri"
            )
            header(HttpHeaders.UserAgent, "TourVerse/1.0 destination-linking")
            setBody(body.toString())
        }
        val payload = Json.parseToJsonElement(response.bodyAsText()).jsonObject
        return payload["places"]?.jsonArray.orEmpty().mapNotNull { value ->
            val place = value.jsonObject
            val id = place["id"]?.jsonPrimitive?.contentOrNull ?: return@mapNotNull null
            val displayName = place["displayName"]?.jsonObject?.get("text")
                ?.jsonPrimitive?.contentOrNull ?: return@mapNotNull null
            val location = place["location"]?.jsonObject
            GooglePlaceSearchResult(
                placeId = id,
                displayName = displayName,
                formattedAddress = place["formattedAddress"]?.jsonPrimitive?.contentOrNull,
                latitude = location?.get("latitude")?.jsonPrimitive?.doubleOrNull,
                longitude = location?.get("longitude")?.jsonPrimitive?.doubleOrNull,
                googleMapsUri = place["googleMapsUri"]?.jsonPrimitive?.contentOrNull
            )
        }.take(limit)
    }
}

class GooglePlaceLinkService(
    private val destinations: DestinationRepository,
    private val catalogue: DestinationImportRepository,
    private val google: GooglePlacesSearchClient
) {
    suspend fun search(
        destinationId: UUID,
        request: GooglePlaceSearchRequest
    ): List<GooglePlaceSearchResult> {
        if (!google.configured) throw ProviderNotConfiguredException(
            "Google Places is not configured on the TourVerse backend."
        )
        val destination = destinations.getById(destinationId)
            ?: throw NotFoundException("Destination not found.")
        val latitude = destination.latitude
            ?: throw ValidationException("Destination coordinates are required for Google Place linking.")
        val longitude = destination.longitude
            ?: throw ValidationException("Destination coordinates are required for Google Place linking.")
        if (request.limit !in 1..5) throw ValidationException("Google Place search limit must be between 1 and 5.")
        val text = request.textQuery?.trim()?.takeIf(String::isNotEmpty) ?: destination.name
        return google.search(text, latitude, longitude, request.limit)
    }

    suspend fun link(destinationId: UUID, adminId: UUID, request: LinkGooglePlaceRequest) =
        catalogue.linkGooglePlace(destinationId, adminId, validate(request))
            ?: throw NotFoundException("Destination not found.")

    suspend fun remove(destinationId: UUID) {
        if (!catalogue.removeGooglePlace(destinationId)) {
            throw NotFoundException("Active Google Place link not found.")
        }
    }

    private fun validate(request: LinkGooglePlaceRequest): LinkGooglePlaceRequest {
        val placeId = request.placeId.trim()
        if (!Regex("[A-Za-z0-9_-]{10,255}").matches(placeId)) {
            throw ValidationException("Google Place ID is invalid.")
        }
        val attribution = request.attribution.trim()
        if (attribution != "Google") throw ValidationException("Google attribution must be preserved.")
        return request.copy(placeId = placeId, attribution = attribution)
    }
}
