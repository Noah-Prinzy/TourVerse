package com.tourverse

import com.tourverse.models.*
import com.tourverse.plugins.configureSerialization
import com.tourverse.plugins.configureStatusPages
import com.tourverse.repositories.DestinationImportRepository
import com.tourverse.routes.destinationImportRoutes
import com.tourverse.security.TokenService
import com.tourverse.services.DestinationImportProvider
import com.tourverse.services.DestinationImportService
import io.ktor.client.request.*
import io.ktor.client.statement.bodyAsText
import io.ktor.http.*
import io.ktor.server.routing.routing
import io.ktor.server.testing.testApplication
import java.time.Instant
import java.util.UUID
import kotlinx.serialization.json.Json
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlin.test.*

class DestinationImportAuthorizationTest {
    private val requestJson = """{"provider":"FIXTURE","countryCode":"UG","limit":2}"""

    @Test fun `import request and batch are JSON serializable`() {
        val request = Json.decodeFromString<DestinationImportQuery>(requestJson)
        assertEquals("UG", request.countryCode)
        val batch = kotlinx.coroutines.runBlocking { FakeImportRepository().createBatch(UUID.randomUUID(), request) }
        assertTrue(Json.encodeToString(batch).contains("\"provider\":\"FIXTURE\""))
    }

    @Test fun `anonymous and non admin cannot start imports`() = testApplication {
        val provider = FixtureProvider()
        application {
            configureSerialization(); configureStatusPages()
            routing { destinationImportRoutes(DestinationImportService(FakeImportRepository(), listOf(provider))) }
        }
        assertEquals(HttpStatusCode.Unauthorized, client.post("/api/admin/destination-imports/search") {
            contentType(ContentType.Application.Json); setBody(requestJson)
        }.status)
        val token = TokenService.createAccessToken(UUID.randomUUID(), "USER").token
        assertEquals(HttpStatusCode.Forbidden, client.post("/api/admin/destination-imports/search") {
            header(HttpHeaders.Authorization, "Bearer $token")
            contentType(ContentType.Application.Json); setBody(requestJson)
        }.status)
        assertEquals(0, provider.calls)
    }

    @Test fun `administrator can create a fixture backed review batch`() = testApplication {
        val provider = FixtureProvider()
        application {
            configureSerialization(); configureStatusPages()
            routing { destinationImportRoutes(DestinationImportService(FakeImportRepository(), listOf(provider))) }
        }
        val token = TokenService.createAccessToken(UUID.randomUUID(), "ADMIN").token
        val response = client.post("/api/admin/destination-imports/search") {
            header(HttpHeaders.Authorization, "Bearer $token")
            contentType(ContentType.Application.Json); setBody(requestJson)
        }
        assertEquals(HttpStatusCode.Created, response.status, response.bodyAsText())
        assertEquals(1, provider.calls)
    }
}

private class FixtureProvider : DestinationImportProvider {
    override val providerName = "FIXTURE"
    override val enabled = true
    var calls = 0
    override suspend fun search(query: DestinationImportQuery): List<DestinationCandidate> {
        calls++
        return listOf(DestinationCandidate(
            sourceProvider = providerName, externalId = "fixture-1",
            name = "Fixture destination", countryCode = query.countryCode, country = "Uganda"
        ))
    }
    override suspend fun getDetails(externalId: String) = null
}

private class FakeImportRepository : DestinationImportRepository {
    private val batches = mutableListOf<DestinationImportBatch>()
    override suspend fun createBatch(adminId: UUID, query: DestinationImportQuery): DestinationImportBatch {
        val now = Instant.now()
        return DestinationImportBatch(
            UUID.randomUUID(), query.provider, adminId, query.countryCode, query.city, query.search,
            query.limit, DestinationImportBatchStatus.RUNNING, 0, null, now, now
        ).also(batches::add)
    }
    override suspend fun saveCandidates(batchId: UUID, candidates: List<DestinationCandidate>) =
        candidates.map { it.copy(batchId = batchId) }
    override suspend fun completeBatch(batchId: UUID, retrievedCount: Int, error: String?) =
        batches.first { it.id == batchId }.copy(
            status = if (error == null) DestinationImportBatchStatus.COMPLETED else DestinationImportBatchStatus.FAILED,
            retrievedCount = retrievedCount, errorMessage = error
        )
    override suspend fun listBatches() = batches
    override suspend fun getBatch(id: UUID) = batches.find { it.id == id }
    override suspend fun listCandidates(batchId: UUID?, status: DestinationImportStatus?) = emptyList<DestinationCandidate>()
    override suspend fun getCandidate(id: UUID) = null
    override suspend fun updateCandidate(id: UUID, request: UpdateDestinationCandidateRequest) = null
    override suspend fun rejectCandidate(id: UUID, adminId: UUID, reason: String) = null
    override suspend fun linkCandidate(id: UUID, adminId: UUID, destinationId: UUID) = null
    override suspend fun approveCandidate(id: UUID, adminId: UUID) = null
}
