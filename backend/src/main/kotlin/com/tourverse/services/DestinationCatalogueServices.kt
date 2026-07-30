package com.tourverse.services

import com.tourverse.models.*
import com.tourverse.repositories.DestinationImportRepository
import com.tourverse.utils.ValidationException
import java.net.URI
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.time.Duration
import java.time.Instant
import java.util.UUID

object DestinationFreshnessPolicy {
    val externalCore: Duration = Duration.ofDays(30)
    val coordinates: Duration = Duration.ofDays(90)

    // Coordinates the expires at business workflow for callers.
    fun expiresAt(origin: DataOrigin, verifiedAt: Instant): Instant? =
        if (origin == DataOrigin.EXTERNAL || origin == DataOrigin.HYBRID) {
            verifiedAt.plus(externalCore)
        } else null

    // Coordinates the cache status business workflow for callers.
    fun cacheStatus(origin: DataOrigin, expiresAt: Instant?, now: Instant = Instant.now()): CacheStatus =
        when {
            origin == DataOrigin.TOURVERSE_CURATED || origin == DataOrigin.DEVELOPMENT_SEED ->
                CacheStatus.NOT_APPLICABLE
            expiresAt == null || !expiresAt.isAfter(now) -> CacheStatus.STALE
            else -> CacheStatus.FRESH
        }
}

object DestinationContentHasher {
    // Checks whether hash applies to the current data.
    fun hash(vararg values: String?): String {
        val normalized = values.joinToString("\u001f") { it?.trim().orEmpty() }
        return MessageDigest.getInstance("SHA-256")
            .digest(normalized.toByteArray(StandardCharsets.UTF_8))
            .joinToString("") { "%02x".format(it) }
    }
}

object DestinationNormalizationService {
    // Converts the supplied values into the normalize name form required by the domain model.
    fun normalizeName(value: String): String =
        value.trim().replace(Regex("\\s+"), " ")
}

object DestinationMergeService {
    // Updates merge while keeping related state consistent.
    fun merge(current: Destination, external: ExternalDestination): Destination {
        if (current.dataOrigin == DataOrigin.TOURVERSE_CURATED) return current
        return current.copy(
            name = current.name.ifBlank { DestinationNormalizationService.normalizeName(external.name) },
            countryCode = current.countryCode ?: external.countryCode,
            city = current.city ?: external.city,
            latitude = current.latitude ?: external.latitude,
            longitude = current.longitude ?: external.longitude,
            dataOrigin = DataOrigin.HYBRID
        )
    }
}

object DestinationProviderValidator {
    private val externalId = Regex("[A-Za-z0-9._:-]{1,255}")

    // Validates validate and stops the workflow when input is invalid.
    fun validate(destination: ExternalDestination) {
        if (destination.name.isBlank()) throw ValidationException("Provider destination name must not be blank.")
        CountryCodeService.normalizeCode(destination.countryCode)
        destination.latitude?.let {
            if (it !in -90.0..90.0) throw ValidationException("Provider latitude must be between -90 and 90.")
        }
        destination.longitude?.let {
            if (it !in -180.0..180.0) throw ValidationException("Provider longitude must be between -180 and 180.")
        }
        if (!externalId.matches(destination.externalId)) {
            throw ValidationException("Provider external ID is invalid.")
        }
        destination.sourceUrl?.let {
            val uri = runCatching { URI(it) }.getOrNull()
            if (uri == null || uri.scheme !in setOf("http", "https") || uri.host.isNullOrBlank()) {
                throw ValidationException("Provider source URL must use HTTP or HTTPS.")
            }
        }
        if (destination.imageReference != null &&
            (destination.imageLicence.isNullOrBlank() || destination.imageAttribution.isNullOrBlank())
        ) {
            throw ValidationException("Provider images require licence and attribution.")
        }
    }
}

class DestinationCatalogueService(
    private val repository: DestinationImportRepository,
    private val importService: DestinationImportService
) {
    // Updates sync while keeping related state consistent.
    suspend fun sync(adminId: UUID, request: CatalogueSyncRequest): List<DestinationImportBatch> {
        if (request.publishMode != "REVIEW_REQUIRED") {
            throw ValidationException("publishMode must be REVIEW_REQUIRED.")
        }
        if (request.maximumResults !in 1..20) {
            throw ValidationException("maximumResults must be between 1 and 20.")
        }
        val code = CountryCodeService.normalizeCode(request.countryCode)!!
        if (request.providers.isEmpty()) throw ValidationException("At least one provider is required.")
        return request.providers.map { provider ->
            if (provider !in setOf(DestinationProvider.WIKIDATA, DestinationProvider.OPENTRIPMAP)) {
                throw ValidationException("$provider is not a catalogue discovery provider.")
            }
            importService.search(
                adminId,
                DestinationImportQuery(provider.name, code, limit = request.maximumResults)
            )
        }
    }

    // Coordinates the jobs business workflow for callers.
    suspend fun jobs() = repository.listBatches()
    // Coordinates the stale business workflow for callers.
    suspend fun stale() = repository.listStaleDestinations()
    // Coordinates the sources business workflow for callers.
    suspend fun sources(destinationId: UUID) = repository.listSources(destinationId)
    // Coordinates the request refresh business workflow for callers.
    suspend fun requestRefresh(destinationId: UUID) =
        repository.markRefreshPending(destinationId)
            ?: throw com.tourverse.exceptions.NotFoundException("Destination not found.")
}
