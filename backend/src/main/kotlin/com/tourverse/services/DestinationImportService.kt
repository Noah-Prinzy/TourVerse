package com.tourverse.services

import com.tourverse.exceptions.NotFoundException
import com.tourverse.models.*
import com.tourverse.repositories.DestinationImportRepository
import com.tourverse.utils.ValidationException
import java.net.URI
import java.time.Instant
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class DestinationImportService(
    private val repository: DestinationImportRepository,
    providers: List<DestinationImportProvider>
) {
    private val providers = providers.associateBy { it.providerName.uppercase() }
    private val lastRequest = ConcurrentHashMap<String, Instant>()

    // Retrieves search from the relevant repository or external provider.
    suspend fun search(adminId: UUID, request: DestinationImportQuery): DestinationImportBatch {
        val providerName = request.provider.trim().uppercase()
        val provider = providers[providerName]
            ?: throw ValidationException("Provider must be one of: ${providers.keys.sorted().joinToString()}.")
        if (!provider.enabled) throw ValidationException("$providerName provider is disabled.")
        val code = CountryCodeService.normalizeCode(request.countryCode)!!
        if (request.limit !in 1..100) throw ValidationException("Import limit must be between 1 and 100.")
        request.city?.let { length(it, "City", 100) }
        request.search?.let { length(it, "Search", 200) }
        if ((request.latitude == null) != (request.longitude == null)) {
            throw ValidationException("Latitude and longitude must be supplied together.")
        }
        val quotaKey = "$adminId:$providerName"
        lastRequest[quotaKey]?.let {
            if (Instant.now().minusSeconds(2).isBefore(it)) {
                throw ValidationException("Wait before starting another import for this provider.")
            }
        }
        lastRequest[quotaKey] = Instant.now()
        val normalized = request.copy(provider = providerName, countryCode = code)
        val batch = repository.createBatch(adminId, normalized)
        return try {
            val candidates = provider.search(normalized).take(request.limit).map { candidate ->
                candidate.copy(
                    countryCode = candidate.countryCode?.let(CountryCodeService::normalizeCode),
                    mappedCategory = SourceCategoryMapper.map(candidate.sourceClassifications)
                )
            }
            repository.saveCandidates(batch.id, candidates)
            repository.completeBatch(batch.id, candidates.size)
        } catch (exception: Exception) {
            repository.completeBatch(batch.id, 0, exception.message ?: "Provider request failed.")
            throw exception
        }
    }

    // Retrieves batches from the relevant repository or external provider.
    suspend fun listBatches() = repository.listBatches()
    // Retrieves batch from the relevant repository or external provider.
    suspend fun getBatch(id: UUID) = repository.getBatch(id) ?: throw NotFoundException("Import batch not found.")
    // Retrieves candidates from the relevant repository or external provider.
    suspend fun listCandidates(batchId: UUID?, status: DestinationImportStatus?) = repository.listCandidates(batchId, status)
    // Retrieves candidate from the relevant repository or external provider.
    suspend fun getCandidate(id: UUID) = repository.getCandidate(id) ?: throw NotFoundException("Import candidate not found.")

    // Updates retry while keeping related state consistent.
    suspend fun retry(adminId: UUID, batchId: UUID): DestinationImportBatch {
        val batch = getBatch(batchId)
        return search(adminId, DestinationImportQuery(
            provider = batch.provider, countryCode = batch.countryCode
                ?: throw ValidationException("The original batch has no valid country code."),
            city = batch.city, search = batch.queryText, limit = batch.requestedLimit
        ))
    }

    // Updates candidate while keeping related state consistent.
    suspend fun updateCandidate(id: UUID, request: UpdateDestinationCandidateRequest): DestinationCandidate {
        request.name?.let { length(it, "Name", 150, false) }
        request.country?.let { length(it, "Country", 100, false) }
        request.countryCode?.let(CountryCodeService::normalizeCode)
        request.latitude?.let { if (it !in -90.0..90.0) throw ValidationException("Latitude must be between -90 and 90.") }
        request.longitude?.let { if (it !in -180.0..180.0) throw ValidationException("Longitude must be between -180 and 180.") }
        listOf(request.officialWebsite, request.imageReference, request.imageLicenceUrl)
            .filterNotNull().filter(String::isNotBlank).forEach(::validateUrl)
        return repository.updateCandidate(id, request) ?: throw NotFoundException("Import candidate not found.")
    }

    // Updates candidate while keeping related state consistent.
    suspend fun rejectCandidate(id: UUID, adminId: UUID, request: RejectDestinationCandidateRequest): DestinationCandidate {
        length(request.reason, "Rejection reason", 500, false)
        return repository.rejectCandidate(id, adminId, request.reason.trim())
            ?: throw NotFoundException("Import candidate not found.")
    }

    // Updates candidate while keeping related state consistent.
    suspend fun linkCandidate(id: UUID, adminId: UUID, request: LinkDestinationCandidateRequest): DestinationCandidate =
        repository.linkCandidate(id, adminId, request.destinationId)
            ?: throw NotFoundException("Import candidate or destination not found.")

    // Updates candidate while keeping related state consistent.
    suspend fun approveCandidate(id: UUID, adminId: UUID): DestinationCandidate {
        val candidate = getCandidate(id)
        DestinationValidator.validate(CreateDestinationRequest(
            name = candidate.name, country = candidate.country, countryCode = candidate.countryCode,
            city = candidate.city,
            description = candidate.descriptionHint?.takeIf(String::isNotBlank)
                ?: "A TourVerse destination awaiting editorial description.",
            category = candidate.mappedCategory ?: "", latitude = candidate.latitude,
            longitude = candidate.longitude, coverImageUrl = candidate.imageReference
        ))
        val approved = repository.approveCandidate(id, adminId)
            ?: throw NotFoundException("Import candidate not found.")
        if (approved.reviewStatus == DestinationImportStatus.POSSIBLE_DUPLICATE) {
            throw com.tourverse.exceptions.ConflictException(
                "Candidate may duplicate an existing destination and requires linking or rejection."
            )
        }
        return approved
    }

    // Coordinates the length business workflow for callers.
    private fun length(value: String, field: String, max: Int, blankAllowed: Boolean = true) {
        val clean = value.trim()
        if (!blankAllowed && clean.isBlank()) throw ValidationException("$field must not be blank.")
        if (clean.length > max) throw ValidationException("$field must not exceed $max characters.")
    }

    // Validates url and stops the workflow when input is invalid.
    private fun validateUrl(value: String) {
        val uri = runCatching { URI(value) }.getOrNull()
        if (uri == null || uri.scheme?.lowercase() !in setOf("http", "https") || uri.host.isNullOrBlank()) {
            throw ValidationException("Imported URLs must use a valid HTTP or HTTPS URL.")
        }
    }
}
