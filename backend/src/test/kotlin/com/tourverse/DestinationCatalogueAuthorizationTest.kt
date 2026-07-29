package com.tourverse

import com.tourverse.models.*
import com.tourverse.plugins.configureSerialization
import com.tourverse.plugins.configureStatusPages
import com.tourverse.repositories.DestinationImportRepository
import com.tourverse.routes.destinationCatalogueRoutes
import com.tourverse.security.TokenService
import com.tourverse.services.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.server.routing.routing
import io.ktor.server.testing.testApplication
import java.util.UUID
import java.time.Instant
import kotlin.test.*

class DestinationCatalogueAuthorizationTest {
    @Test fun `anonymous and user cannot start catalogue synchronization`() = testApplication {
        val repository = EmptyImportRepository()
        val importService = DestinationImportService(repository, listOf(NoCallWikidataProvider()))
        val catalogue = DestinationCatalogueService(repository, importService)
        val google = GooglePlaceLinkService(
            CatalogueDestinationRepository(Destination(
                id = UUID.randomUUID(), name = "Fixture", country = "Uganda", city = null,
                description = "Fixture destination", category = "Nature",
                latitude = 0.1, longitude = 32.1, coverImageUrl = null,
                createdAt = Instant.EPOCH, updatedAt = Instant.EPOCH
            )),
            repository,
            object : GooglePlacesSearchClient {
                override val configured = false
                override suspend fun search(name: String, latitude: Double, longitude: Double, limit: Int) =
                    emptyList<GooglePlaceSearchResult>()
            }
        )
        application {
            configureSerialization()
            configureStatusPages()
            routing { destinationCatalogueRoutes(catalogue, google) }
        }
        val body = """{"countryCode":"UG","providers":["WIKIDATA"],"maximumResults":3}"""
        assertEquals(HttpStatusCode.Unauthorized, client.post("/api/admin/catalogue/sync") {
            contentType(ContentType.Application.Json); setBody(body)
        }.status)
        val token = TokenService.createAccessToken(UUID.randomUUID(), "USER").token
        assertEquals(HttpStatusCode.Forbidden, client.post("/api/admin/catalogue/sync") {
            header(HttpHeaders.Authorization, "Bearer $token")
            contentType(ContentType.Application.Json); setBody(body)
        }.status)
    }
}

private class NoCallWikidataProvider : DestinationImportProvider {
    override val providerName = "WIKIDATA"
    override val enabled = true
    override suspend fun search(query: DestinationImportQuery) = emptyList<DestinationCandidate>()
    override suspend fun getDetails(externalId: String) = null
}
