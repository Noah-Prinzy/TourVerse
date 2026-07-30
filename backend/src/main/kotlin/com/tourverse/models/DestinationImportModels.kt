package com.tourverse.models

import com.tourverse.utils.InstantSerializer
import com.tourverse.utils.UUIDSerializer
import kotlinx.serialization.Serializable
import java.time.Instant
import java.util.UUID

@Serializable
// Lists the supported destination import status values used by validation and persistence.
enum class DestinationImportStatus {
    PENDING_REVIEW, APPROVED, REJECTED, POSSIBLE_DUPLICATE, IMPORT_FAILED
}

@Serializable
// Lists the supported destination import batch status values used by validation and persistence.
enum class DestinationImportBatchStatus {
    RUNNING, COMPLETED, FAILED
}

@Serializable
// Lists the supported destination provider values used by validation and persistence.
enum class DestinationProvider {
    WIKIDATA, OPENTRIPMAP, GOOGLE_PLACES, TOURVERSE, DEVELOPMENT_SEED
}

@Serializable
// Carries destination import query values between application layers.
data class DestinationImportQuery(
    val provider: String,
    val countryCode: String,
    val city: String? = null,
    val search: String? = null,
    val category: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val radiusKm: Double? = null,
    val limit: Int = 20
)

@Serializable
// Carries destination candidate values between application layers.
data class DestinationCandidate(
    @Serializable(with = UUIDSerializer::class) val id: UUID = UUID.randomUUID(),
    @Serializable(with = UUIDSerializer::class) val batchId: UUID? = null,
    val sourceProvider: String,
    val externalId: String,
    val sourceUrl: String? = null,
    val name: String,
    val countryCode: String? = null,
    val country: String,
    val region: String? = null,
    val city: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val categoryHint: String? = null,
    val mappedCategory: String? = null,
    val descriptionHint: String? = null,
    val officialWebsite: String? = null,
    val imageReference: String? = null,
    val imageLicence: String? = null,
    val imageAttribution: String? = null,
    val imageLicenceUrl: String? = null,
    val sourceClassifications: List<String> = emptyList(),
    @Serializable(with = InstantSerializer::class) val retrievedAt: Instant = Instant.now(),
    val reviewStatus: DestinationImportStatus = DestinationImportStatus.PENDING_REVIEW,
    val rejectionReason: String? = null,
    @Serializable(with = UUIDSerializer::class) val duplicateOfDestinationId: UUID? = null,
    @Serializable(with = UUIDSerializer::class) val approvedDestinationId: UUID? = null,
    @Serializable(with = UUIDSerializer::class) val reviewedBy: UUID? = null,
    @Serializable(with = InstantSerializer::class) val createdAt: Instant = Instant.now(),
    @Serializable(with = InstantSerializer::class) val updatedAt: Instant = Instant.now()
)

@Serializable
// Carries destination import batch values between application layers.
data class DestinationImportBatch(
    @Serializable(with = UUIDSerializer::class) val id: UUID,
    val provider: String,
    @Serializable(with = UUIDSerializer::class) val requestedBy: UUID,
    val countryCode: String?,
    val city: String?,
    val queryText: String?,
    val requestedLimit: Int,
    val status: DestinationImportBatchStatus,
    val retrievedCount: Int,
    val errorMessage: String?,
    @Serializable(with = InstantSerializer::class) val createdAt: Instant,
    @Serializable(with = InstantSerializer::class) val updatedAt: Instant
)

@Serializable
data class UpdateDestinationCandidateRequest(
    val name: String? = null,
    val countryCode: String? = null,
    val country: String? = null,
    val region: String? = null,
    val city: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val mappedCategory: String? = null,
    val descriptionHint: String? = null,
    val officialWebsite: String? = null,
    val imageReference: String? = null,
    val imageLicence: String? = null,
    val imageAttribution: String? = null,
    val imageLicenceUrl: String? = null
)

@Serializable
data class RejectDestinationCandidateRequest(val reason: String)

@Serializable
data class LinkDestinationCandidateRequest(
    @Serializable(with = UUIDSerializer::class) val destinationId: UUID
)

@Serializable
data class DuplicateReason(val code: String, val explanation: String)

@Serializable
data class DuplicateAssessment(
    val outcome: String,
    val reasons: List<DuplicateReason>
)

@Serializable
data class DestinationSourceReference(
    @Serializable(with = UUIDSerializer::class) val id: UUID,
    @Serializable(with = UUIDSerializer::class) val destinationId: UUID,
    val provider: DestinationProvider,
    val externalId: String,
    val sourceUrl: String? = null,
    @Serializable(with = InstantSerializer::class) val retrievedAt: Instant,
    @Serializable(with = InstantSerializer::class) val lastVerifiedAt: Instant? = null,
    @Serializable(with = InstantSerializer::class) val providerContentUpdatedAt: Instant? = null,
    val attribution: String? = null,
    val licence: String? = null,
    val providerPlaceId: String? = null,
    val metadataHash: String? = null,
    val active: Boolean = true
)

@Serializable
data class CatalogueSyncRequest(
    val countryCode: String,
    val providers: List<DestinationProvider> = listOf(DestinationProvider.WIKIDATA),
    val maximumResults: Int = 5,
    val publishMode: String = "REVIEW_REQUIRED"
)

@Serializable
data class GooglePlaceSearchRequest(val textQuery: String? = null, val limit: Int = 5)

@Serializable
data class GooglePlaceSearchResult(
    val placeId: String,
    val displayName: String,
    val formattedAddress: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val googleMapsUri: String? = null,
    val attribution: String = "Google"
)

@Serializable
data class LinkGooglePlaceRequest(
    val placeId: String,
    val googleMapsUri: String? = null,
    val attribution: String = "Google"
)
