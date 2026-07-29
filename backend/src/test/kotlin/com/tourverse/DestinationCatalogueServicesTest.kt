package com.tourverse

import com.tourverse.exceptions.ProviderNotConfiguredException
import com.tourverse.models.*
import com.tourverse.repositories.DestinationImportRepository
import com.tourverse.repositories.DestinationRepository
import com.tourverse.services.*
import com.tourverse.utils.ValidationException
import kotlinx.coroutines.runBlocking
import java.time.Instant
import java.util.UUID
import kotlin.test.*

class DestinationCatalogueServicesTest {
    private val now = Instant.parse("2026-01-01T00:00:00Z")

    @Test fun `freshness rules keep curated and seed rows outside provider expiry`() {
        assertNull(DestinationFreshnessPolicy.expiresAt(DataOrigin.DEVELOPMENT_SEED, now))
        assertEquals(CacheStatus.NOT_APPLICABLE, DestinationFreshnessPolicy.cacheStatus(
            DataOrigin.TOURVERSE_CURATED, null, now
        ))
        val expiry = DestinationFreshnessPolicy.expiresAt(DataOrigin.EXTERNAL, now)
        assertEquals(now.plusSeconds(30L * 24 * 60 * 60), expiry)
        assertEquals(CacheStatus.FRESH, DestinationFreshnessPolicy.cacheStatus(
            DataOrigin.EXTERNAL, expiry, now
        ))
        assertEquals(CacheStatus.STALE, DestinationFreshnessPolicy.cacheStatus(
            DataOrigin.EXTERNAL, now.minusSeconds(1), now
        ))
    }

    @Test fun `content hash is stable and sensitive to normalized content`() {
        val first = DestinationContentHasher.hash(" Name ", "UG", null)
        assertEquals(first, DestinationContentHasher.hash("Name", "UG", null))
        assertNotEquals(first, DestinationContentHasher.hash("Different", "UG", null))
        assertTrue(Regex("[0-9a-f]{64}").matches(first))
    }

    @Test fun `merge preserves curated fields and fills unlocked external gaps`() {
        val curated = destination(DataOrigin.TOURVERSE_CURATED)
        val external = external()
        assertSame(curated, DestinationMergeService.merge(curated, external))

        val unlocked = destination(DataOrigin.EXTERNAL).copy(city = null, latitude = null, longitude = null)
        val merged = DestinationMergeService.merge(unlocked, external)
        assertEquals("Kampala", merged.city)
        assertEquals(0.31, merged.latitude)
        assertEquals(DataOrigin.HYBRID, merged.dataOrigin)
        assertEquals(unlocked.name, merged.name)
    }

    @Test fun `provider validation rejects bad coordinates ids and unlicensed images`() {
        assertFailsWith<ValidationException> {
            DestinationProviderValidator.validate(external().copy(latitude = 91.0))
        }
        assertFailsWith<ValidationException> {
            DestinationProviderValidator.validate(external().copy(externalId = "bad id"))
        }
        assertFailsWith<ValidationException> {
            DestinationProviderValidator.validate(external().copy(imageReference = "https://example.com/a.jpg"))
        }
        DestinationProviderValidator.validate(external())
    }

    @Test fun `google place search fails safely when backend key is absent`() = runBlocking {
        val service = GooglePlaceLinkService(
            destinations = CatalogueDestinationRepository(destination(DataOrigin.DEVELOPMENT_SEED)),
            catalogue = object : DestinationImportRepository by EmptyImportRepository() {},
            google = object : GooglePlacesSearchClient {
                override val configured = false
                override suspend fun search(
                    name: String, latitude: Double, longitude: Double, limit: Int
                ) = emptyList<GooglePlaceSearchResult>()
            }
        )
        assertFailsWith<ProviderNotConfiguredException> {
            service.search(UUID.randomUUID(), GooglePlaceSearchRequest())
        }
        Unit
    }

    private fun destination(origin: DataOrigin) = Destination(
        id = UUID.randomUUID(),
        name = "TourVerse name",
        country = "Uganda",
        city = "Existing city",
        description = "Editorial description",
        category = "Nature",
        latitude = 0.1,
        longitude = 32.1,
        coverImageUrl = null,
        createdAt = now,
        updatedAt = now,
        countryCode = "UG",
        dataOrigin = origin
    )

    private fun external() = ExternalDestination(
        provider = DestinationProvider.WIKIDATA,
        externalId = "Q123",
        sourceUrl = "https://www.wikidata.org/wiki/Q123",
        name = "Provider name",
        countryCode = "UG",
        country = "Uganda",
        city = "Kampala",
        latitude = 0.31,
        longitude = 32.58,
        description = null,
        officialWebsite = null,
        providerTypes = emptyList(),
        categoryHint = null,
        imageReference = null,
        imageLicence = null,
        imageAttribution = null,
        retrievedAt = now
    )
}

internal class CatalogueDestinationRepository(private val item: Destination) : DestinationRepository {
    override suspend fun getAll(query: DestinationQuery) =
        com.tourverse.dto.PagedDestinationResponse(listOf(item), 1, 20, 1, 1)
    override suspend fun getById(id: UUID) = item
    override suspend fun getCountries() = emptyList<DestinationCountry>()
    override suspend fun create(request: CreateDestinationRequest) = item
    override suspend fun update(id: UUID, request: UpdateDestinationRequest) = item
    override suspend fun delete(id: UUID) = false
}

internal open class EmptyImportRepository : DestinationImportRepository {
    override suspend fun createBatch(adminId: UUID, query: DestinationImportQuery) = error("unused")
    override suspend fun saveCandidates(batchId: UUID, candidates: List<DestinationCandidate>) = emptyList<DestinationCandidate>()
    override suspend fun completeBatch(batchId: UUID, retrievedCount: Int, error: String?) = error("unused")
    override suspend fun listBatches() = emptyList<DestinationImportBatch>()
    override suspend fun getBatch(id: UUID) = null
    override suspend fun listCandidates(batchId: UUID?, status: DestinationImportStatus?) = emptyList<DestinationCandidate>()
    override suspend fun getCandidate(id: UUID) = null
    override suspend fun updateCandidate(id: UUID, request: UpdateDestinationCandidateRequest) = null
    override suspend fun rejectCandidate(id: UUID, adminId: UUID, reason: String) = null
    override suspend fun linkCandidate(id: UUID, adminId: UUID, destinationId: UUID) = null
    override suspend fun approveCandidate(id: UUID, adminId: UUID) = null
}
