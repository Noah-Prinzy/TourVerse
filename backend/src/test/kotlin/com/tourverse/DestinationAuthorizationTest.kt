package com.tourverse

import com.tourverse.dto.PagedDestinationResponse
import com.tourverse.models.CreateDestinationRequest
import com.tourverse.models.Destination
import com.tourverse.models.DestinationQuery
import com.tourverse.models.UpdateDestinationRequest
import com.tourverse.plugins.configureSerialization
import com.tourverse.plugins.configureStatusPages
import com.tourverse.repositories.DestinationRepository
import com.tourverse.routes.destinationRoutes
import com.tourverse.security.TokenService
import com.tourverse.services.DestinationService
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.routing.routing
import io.ktor.server.testing.testApplication
import java.time.Instant
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals

class DestinationAuthorizationTest {
    private val destinationId = UUID.fromString("11111111-1111-1111-1111-111111111111")
    private val requestJson = """
        {
          "name": "Test destination",
          "country": "Uganda",
          "city": "Kampala",
          "description": "A development destination used by route tests.",
          "category": "Nature",
          "latitude": null,
          "longitude": null,
          "coverImageUrl": null
        }
    """.trimIndent()

    @Test
    fun `anonymous reads remain public`() = withRoutes { repository ->
        assertEquals(HttpStatusCode.OK, client.get("/api/destinations").status)
        assertEquals(HttpStatusCode.OK, client.get("/api/destinations/$destinationId").status)
        assertEquals(2, repository.readCount)
    }

    @Test
    fun `anonymous writes are rejected without mutation`() = withRoutes { repository ->
        assertEquals(HttpStatusCode.Unauthorized, client.post("/api/destinations") { jsonBody() }.status)
        assertEquals(HttpStatusCode.Unauthorized, client.put("/api/destinations/$destinationId") { jsonBody() }.status)
        assertEquals(HttpStatusCode.Unauthorized, client.delete("/api/destinations/$destinationId").status)
        assertEquals(0, repository.writeCount)
    }

    @Test
    fun `non admin writes are forbidden without mutation`() = withRoutes { repository ->
        val token = TokenService.createAccessToken(UUID.randomUUID(), "USER").token
        assertEquals(HttpStatusCode.Forbidden, client.post("/api/destinations") { authorizedJson(token) }.status)
        assertEquals(HttpStatusCode.Forbidden, client.put("/api/destinations/$destinationId") { authorizedJson(token) }.status)
        assertEquals(HttpStatusCode.Forbidden, client.delete("/api/destinations/$destinationId") {
            header(HttpHeaders.Authorization, "Bearer $token")
        }.status)
        assertEquals(0, repository.writeCount)
    }

    @Test
    fun `administrator can reach destination write handlers`() = withRoutes { repository ->
        val token = TokenService.createAccessToken(UUID.randomUUID(), "ADMIN").token
        assertEquals(HttpStatusCode.Created, client.post("/api/destinations") { authorizedJson(token) }.status)
        assertEquals(HttpStatusCode.OK, client.put("/api/destinations/$destinationId") { authorizedJson(token) }.status)
        assertEquals(HttpStatusCode.OK, client.delete("/api/destinations/$destinationId") {
            header(HttpHeaders.Authorization, "Bearer $token")
        }.status)
        assertEquals(3, repository.writeCount)
    }

    private fun io.ktor.client.request.HttpRequestBuilder.jsonBody() {
        contentType(ContentType.Application.Json)
        setBody(requestJson)
    }

    private fun io.ktor.client.request.HttpRequestBuilder.authorizedJson(token: String) {
        header(HttpHeaders.Authorization, "Bearer $token")
        jsonBody()
    }

    private fun withRoutes(block: suspend io.ktor.server.testing.ApplicationTestBuilder.(FakeDestinationRepository) -> Unit) =
        testApplication {
            val repository = FakeDestinationRepository(destinationId)
            application {
                configureSerialization()
                configureStatusPages()
                routing { destinationRoutes(DestinationService(repository)) }
            }
            block(repository)
        }
}

private class FakeDestinationRepository(private val destinationId: UUID) : DestinationRepository {
    var readCount = 0
    var writeCount = 0

    private val destination = Destination(
        id = destinationId,
        name = "Test destination",
        country = "Uganda",
        city = "Kampala",
        description = "A development destination used by route tests.",
        category = "Nature",
        latitude = null,
        longitude = null,
        coverImageUrl = null,
        createdAt = Instant.parse("2026-01-01T00:00:00Z"),
        updatedAt = Instant.parse("2026-01-01T00:00:00Z")
    )

    override suspend fun getAll(query: DestinationQuery): PagedDestinationResponse {
        readCount++
        return PagedDestinationResponse(listOf(destination), 1, query.size, 1, 1)
    }

    override suspend fun getById(id: UUID): Destination? {
        readCount++
        return destination.takeIf { id == destinationId }
    }

    override suspend fun create(request: CreateDestinationRequest): Destination {
        writeCount++
        return destination
    }

    override suspend fun update(id: UUID, request: UpdateDestinationRequest): Destination? {
        writeCount++
        return destination.takeIf { id == destinationId }
    }

    override suspend fun delete(id: UUID): Boolean {
        writeCount++
        return id == destinationId
    }
}
