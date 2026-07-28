package com.tourverse.data.remote

import com.tourverse.data.model.DestinationQuery
import com.tourverse.data.model.DestinationSortField
import com.tourverse.data.model.SortDirection
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class TourismApiTest {
    @Test
    fun serializesQueryAndParsesPagedResponse() = runBlocking {
        var requestedUrl = ""
        val client = client(MockEngine { request ->
            requestedUrl = request.url.toString()
            respond(
                content = """
                    {
                      "items": [{
                        "id": "c785d75a-8824-45de-8a93-e9a27a488a45",
                        "name": "Maasai Mara",
                        "country": "Kenya",
                        "city": null,
                        "description": "Wildlife reserve",
                        "category": "Wildlife",
                        "latitude": null,
                        "longitude": null,
                        "coverImageUrl": null,
                        "createdAt": "2026-07-28T00:00:00Z",
                        "updatedAt": "2026-07-28T00:00:00Z"
                      }],
                      "page": 2,
                      "size": 10,
                      "totalItems": 11,
                      "totalPages": 2
                    }
                """.trimIndent(),
                status = HttpStatusCode.OK,
                headers = jsonHeaders
            )
        })

        val response = TourismApi(client, "http://localhost:8080").getDestinations(
            DestinationQuery(
                search = "mara & wildlife",
                country = "Kenya",
                page = 2,
                size = 10,
                sortBy = DestinationSortField.NAME,
                sortDirection = SortDirection.ASC
            )
        )

        assertEquals(1, response.items.size)
        assertEquals(2, response.page)
        check("search=mara+%26+wildlife" in requestedUrl || "search=mara%20%26%20wildlife" in requestedUrl)
        check("country=Kenya" in requestedUrl)
        check("page=2" in requestedUrl)
        check("size=10" in requestedUrl)
        check("sortBy=name" in requestedUrl)
        check("sortDirection=asc" in requestedUrl)
    }

    @Test
    fun usesBackendErrorMessage() = runBlocking {
        val client = client(MockEngine {
            respond(
                """{"status":"error","message":"Size must be between 1 and 100."}""",
                HttpStatusCode.BadRequest,
                jsonHeaders
            )
        })

        val exception = assertFailsWith<DestinationApiException> {
            TourismApi(client, "http://localhost:8080")
                .getDestinations(DestinationQuery())
        }

        assertEquals("Size must be between 1 and 100.", exception.message)
    }

    @Test
    fun malformedErrorUsesStableFallback() = runBlocking {
        val client = client(MockEngine {
            respond("not-json", HttpStatusCode.InternalServerError)
        })

        val exception = assertFailsWith<DestinationApiException> {
            TourismApi(client, "http://localhost:8080")
                .getDestinations(DestinationQuery())
        }

        assertEquals("Request failed (HTTP 500).", exception.message)
    }

    private fun client(engine: MockEngine): HttpClient {
        return HttpClient(engine) {
            install(ContentNegotiation) {
                json(ApiClient.json)
            }
        }
    }

    private companion object {
        val jsonHeaders = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
    }
}
