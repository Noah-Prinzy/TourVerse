package com.tourverse.data.remote

import com.tourverse.BuildConfig
import com.tourverse.data.model.ApiMessage
import com.tourverse.data.model.DestinationQuery
import com.tourverse.data.model.Destination
import com.tourverse.data.model.Category
import com.tourverse.data.model.ReviewSummary
import com.tourverse.data.model.PagedDestinationResponse
import io.ktor.client.call.body
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess
import kotlinx.serialization.SerializationException
import kotlinx.serialization.decodeFromString
import kotlinx.coroutines.CancellationException

class TourismApi(
    private val client: HttpClient = ApiClient.client,
    apiBaseUrl: String = BuildConfig.API_BASE_URL
) {

    private val baseUrl = apiBaseUrl
        .trim()
        .also {
            require(it.isNotEmpty()) {
                "API base URL is not configured for the ${BuildConfig.FLAVOR} build."
            }
        }
        .let { if (it.endsWith('/')) it else "$it/" }

    suspend fun getDestinations(query: DestinationQuery): PagedDestinationResponse {
        val response = try {
            client.get("${baseUrl}api/destinations") {
                query.search.trim().takeIf(String::isNotEmpty)?.let { parameter("search", it) }
                query.country.trim().takeIf(String::isNotEmpty)?.let { parameter("country", it) }
                query.city.trim().takeIf(String::isNotEmpty)?.let { parameter("city", it) }
                query.category.trim().takeIf(String::isNotEmpty)?.let { parameter("category", it) }
                parameter("page", query.page)
                parameter("size", query.size)
                parameter("sortBy", query.sortBy.apiValue)
                parameter("sortDirection", query.sortDirection.apiValue)
            }
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: Exception) {
            throw DestinationApiException(
                "Unable to connect to TourVerse. Check your connection and try again.",
                exception
            )
        }

        if (response.status.isSuccess()) {
            return try {
                response.body()
            } catch (exception: Exception) {
                throw DestinationApiException(
                    "TourVerse returned an invalid destination response.",
                    exception
                )
            }
        }

        val fallback = "Request failed (HTTP ${response.status.value})."
        val message = try {
            val body = response.bodyAsText()
            if (body.isBlank()) fallback
            else ApiClient.json.decodeFromString<ApiMessage>(body).message.ifBlank { fallback }
        } catch (_: SerializationException) {
            fallback
        } catch (_: IllegalArgumentException) {
            fallback
        }
        throw DestinationApiException(message)
    }

    suspend fun getDestination(id: String): Destination =
        getPublic("api/destinations/$id")

    suspend fun getCategories(): List<Category> =
        getPublic("api/categories")

    suspend fun getReviews(destinationId: String): ReviewSummary =
        getPublic("api/destinations/$destinationId/reviews")

    private suspend inline fun <reified T> getPublic(path: String): T {
        val response = try {
            client.get("$baseUrl$path")
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: Exception) {
            throw DestinationApiException("Unable to connect to TourVerse. Check your connection and try again.", exception)
        }
        if (response.status.isSuccess()) return try { response.body() }
        catch (exception: Exception) { throw DestinationApiException("TourVerse returned an invalid response.", exception) }
        val fallback = "Request failed (HTTP ${response.status.value})."
        val message = runCatching { ApiClient.json.decodeFromString<ApiMessage>(response.bodyAsText()).message }.getOrDefault(fallback)
        throw DestinationApiException(message)
    }
}

class DestinationApiException(
    override val message: String,
    cause: Throwable? = null
) : Exception(message, cause)
