package com.tourverse.services

import com.tourverse.models.DestinationCandidate
import com.tourverse.models.DestinationImportQuery
import com.tourverse.models.DestinationProvider
import java.time.Instant

data class ProviderDiscoveryQuery(
    val countryCode: String,
    val city: String? = null,
    val textQuery: String? = null,
    val category: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val radiusMeters: Int? = null,
    val limit: Int = 20
)

data class ExternalDestination(
    val provider: DestinationProvider,
    val externalId: String,
    val sourceUrl: String?,
    val name: String,
    val countryCode: String?,
    val country: String,
    val city: String?,
    val latitude: Double?,
    val longitude: Double?,
    val description: String?,
    val officialWebsite: String?,
    val providerTypes: List<String>,
    val categoryHint: String?,
    val imageReference: String?,
    val imageLicence: String?,
    val imageAttribution: String?,
    val retrievedAt: Instant,
    val providerUpdatedAt: Instant? = null,
    val confidence: Double? = null,
    val rawMetadataHash: String? = null
)

interface DestinationDataProvider {
    val provider: DestinationProvider
    val enabled: Boolean
    // Coordinates the discover business workflow for callers.
    suspend fun discover(query: ProviderDiscoveryQuery): List<ExternalDestination>
    // Updates refresh while keeping related state consistent.
    suspend fun refresh(externalId: String): ExternalDestination?
}

interface DestinationImportProvider : DestinationDataProvider {
    val providerName: String
    override val provider: DestinationProvider
        get() = DestinationProvider.valueOf(providerName)
    // Retrieves search from the relevant repository or external provider.
    suspend fun search(query: DestinationImportQuery): List<DestinationCandidate>
    // Retrieves details from the relevant repository or external provider.
    suspend fun getDetails(externalId: String): DestinationCandidate?

    // Coordinates the discover business workflow for callers.
    override suspend fun discover(query: ProviderDiscoveryQuery): List<ExternalDestination> =
        search(
            DestinationImportQuery(
                provider = providerName,
                countryCode = query.countryCode,
                city = query.city,
                search = query.textQuery,
                category = query.category,
                latitude = query.latitude,
                longitude = query.longitude,
                radiusKm = query.radiusMeters?.div(1000.0),
                limit = query.limit
            )
        ).map { it.toExternalDestination() }

    // Updates refresh while keeping related state consistent.
    override suspend fun refresh(externalId: String): ExternalDestination? =
        getDetails(externalId)?.toExternalDestination()
}

class DestinationImportProviderException(message: String, cause: Throwable? = null) :
    RuntimeException(message, cause)

// Coordinates the destination candidate business workflow for callers.
private fun DestinationCandidate.toExternalDestination() = ExternalDestination(
    provider = DestinationProvider.valueOf(sourceProvider),
    externalId = externalId,
    sourceUrl = sourceUrl,
    name = name,
    countryCode = countryCode,
    country = country,
    city = city,
    latitude = latitude,
    longitude = longitude,
    description = descriptionHint,
    officialWebsite = officialWebsite,
    providerTypes = sourceClassifications,
    categoryHint = categoryHint,
    imageReference = imageReference,
    imageLicence = imageLicence,
    imageAttribution = imageAttribution,
    retrievedAt = retrievedAt
)
