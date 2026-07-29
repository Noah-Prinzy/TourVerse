package com.tourverse.repositories

import com.tourverse.database.tables.*
import com.tourverse.exceptions.ConflictException
import com.tourverse.models.*
import com.tourverse.services.DestinationDuplicateDetector
import org.jetbrains.exposed.v1.core.*
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.*
import org.jetbrains.exposed.v1.jdbc.transactions.suspendTransaction
import java.math.BigDecimal
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.UUID

class PostgresDestinationImportRepository : DestinationImportRepository {
    override suspend fun createBatch(adminId: UUID, query: DestinationImportQuery) = suspendTransaction {
        val id = UUID.randomUUID()
        val now = now()
        DestinationImportBatchesTable.insert { row ->
            row[DestinationImportBatchesTable.id] = id
            row[provider] = query.provider.uppercase()
            row[requestedBy] = adminId
            row[countryCode] = query.countryCode
            row[city] = query.city?.clean()
            row[queryText] = query.search?.clean()
            row[requestedLimit] = query.limit
            row[status] = DestinationImportBatchStatus.RUNNING.name
            row[retrievedCount] = 0
            row[createdAt] = now
            row[updatedAt] = now
        }
        batch(id)!!
    }

    override suspend fun saveCandidates(batchId: UUID, candidates: List<DestinationCandidate>) = suspendTransaction {
        candidates.mapNotNull { candidate ->
            val exists = DestinationImportCandidatesTable.selectAll().where {
                (DestinationImportCandidatesTable.sourceProvider eq candidate.sourceProvider) and
                    (DestinationImportCandidatesTable.externalId eq candidate.externalId)
            }.any()
            if (exists) null else {
                val id = UUID.randomUUID()
                val timestamp = now()
                DestinationImportCandidatesTable.insert { row ->
                    row[DestinationImportCandidatesTable.id] = id
                    row[DestinationImportCandidatesTable.batchId] = batchId
                    row[sourceProvider] = candidate.sourceProvider
                    row[externalId] = candidate.externalId
                    row[sourceUrl] = candidate.sourceUrl
                    row[name] = candidate.name.clean()
                    row[countryCode] = candidate.countryCode
                    row[country] = candidate.country.clean()
                    row[region] = candidate.region?.clean()
                    row[city] = candidate.city?.clean()
                    row[latitude] = candidate.latitude?.let(BigDecimal::valueOf)
                    row[longitude] = candidate.longitude?.let(BigDecimal::valueOf)
                    row[categoryHint] = candidate.categoryHint?.clean()
                    row[mappedCategory] = candidate.mappedCategory?.clean()
                    row[descriptionHint] = candidate.descriptionHint?.clean()
                    row[officialWebsite] = candidate.officialWebsite
                    row[imageReference] = candidate.imageReference
                    row[imageLicence] = candidate.imageLicence?.clean()
                    row[imageAttribution] = candidate.imageAttribution?.clean()
                    row[imageLicenceUrl] = candidate.imageLicenceUrl
                    row[sourceClassifications] = candidate.sourceClassifications.joinToString("\n")
                    row[retrievedAt] = OffsetDateTime.ofInstant(candidate.retrievedAt, ZoneOffset.UTC)
                    row[reviewStatus] = candidate.reviewStatus.name
                    row[createdAt] = timestamp
                    row[updatedAt] = timestamp
                }
                candidate(id)!!
            }
        }
    }

    override suspend fun completeBatch(batchId: UUID, retrievedCount: Int, error: String?) = suspendTransaction {
        DestinationImportBatchesTable.update({ DestinationImportBatchesTable.id eq batchId }) { row ->
            row[DestinationImportBatchesTable.retrievedCount] = retrievedCount
            row[status] = if (error == null) DestinationImportBatchStatus.COMPLETED.name
                else DestinationImportBatchStatus.FAILED.name
            row[errorMessage] = error?.take(500)
            row[updatedAt] = now()
        }
        batch(batchId)!!
    }

    override suspend fun listBatches() = suspendTransaction {
        DestinationImportBatchesTable.selectAll()
            .orderBy(DestinationImportBatchesTable.createdAt to SortOrder.DESC)
            .map { it.toBatch() }
    }

    override suspend fun getBatch(id: UUID) = suspendTransaction { batch(id) }

    override suspend fun listCandidates(batchId: UUID?, status: DestinationImportStatus?) = suspendTransaction {
        val query = DestinationImportCandidatesTable.selectAll()
        batchId?.let { query.andWhere { DestinationImportCandidatesTable.batchId eq it } }
        status?.let { value -> query.andWhere { DestinationImportCandidatesTable.reviewStatus eq value.name } }
        query.orderBy(DestinationImportCandidatesTable.createdAt to SortOrder.DESC).map { it.toCandidate() }
    }

    override suspend fun getCandidate(id: UUID) = suspendTransaction { candidate(id) }

    override suspend fun updateCandidate(id: UUID, request: UpdateDestinationCandidateRequest) = suspendTransaction {
        val current = candidate(id) ?: return@suspendTransaction null
        if (current.reviewStatus == DestinationImportStatus.APPROVED) {
            throw ConflictException("An approved candidate cannot be edited.")
        }
        DestinationImportCandidatesTable.update({ DestinationImportCandidatesTable.id eq id }) { row ->
            request.name?.let { row[name] = it.clean() }
            request.countryCode?.let { row[countryCode] = it }
            request.country?.let { row[country] = it.clean() }
            request.region?.let { row[region] = it.clean().ifBlank { null } }
            request.city?.let { row[city] = it.clean().ifBlank { null } }
            request.latitude?.let { row[latitude] = BigDecimal.valueOf(it) }
            request.longitude?.let { row[longitude] = BigDecimal.valueOf(it) }
            request.mappedCategory?.let { row[mappedCategory] = it.clean().ifBlank { null } }
            request.descriptionHint?.let { row[descriptionHint] = it.clean().ifBlank { null } }
            request.officialWebsite?.let { row[officialWebsite] = it.clean().ifBlank { null } }
            request.imageReference?.let { row[imageReference] = it.clean().ifBlank { null } }
            request.imageLicence?.let { row[imageLicence] = it.clean().ifBlank { null } }
            request.imageAttribution?.let { row[imageAttribution] = it.clean().ifBlank { null } }
            request.imageLicenceUrl?.let { row[imageLicenceUrl] = it.clean().ifBlank { null } }
            row[updatedAt] = now()
        }
        candidate(id)
    }

    override suspend fun rejectCandidate(id: UUID, adminId: UUID, reason: String) = suspendTransaction {
        val current = candidate(id) ?: return@suspendTransaction null
        if (current.reviewStatus == DestinationImportStatus.APPROVED) {
            throw ConflictException("An approved candidate cannot be rejected.")
        }
        DestinationImportCandidatesTable.update({ DestinationImportCandidatesTable.id eq id }) { row ->
            row[reviewStatus] = DestinationImportStatus.REJECTED.name
            row[rejectionReason] = reason.take(500)
            row[reviewedBy] = adminId
            row[updatedAt] = now()
        }
        candidate(id)
    }

    override suspend fun linkCandidate(id: UUID, adminId: UUID, destinationId: UUID) = suspendTransaction {
        val current = candidate(id) ?: return@suspendTransaction null
        if (!DestinationsTable.selectAll().where { DestinationsTable.id eq destinationId }.any()) return@suspendTransaction null
        linkSource(current, destinationId)
        DestinationImportCandidatesTable.update({ DestinationImportCandidatesTable.id eq id }) { row ->
            row[reviewStatus] = DestinationImportStatus.APPROVED.name
            row[approvedDestinationId] = destinationId
            row[duplicateOfDestinationId] = destinationId
            row[reviewedBy] = adminId
            row[updatedAt] = now()
        }
        candidate(id)
    }

    override suspend fun approveCandidate(id: UUID, adminId: UUID) = suspendTransaction {
        val current = candidate(id) ?: return@suspendTransaction null
        if (current.reviewStatus == DestinationImportStatus.APPROVED) {
            throw ConflictException("Candidate has already been approved.")
        }
        val category = current.mappedCategory
            ?: throw ConflictException("Map the candidate to a TourVerse category before approval.")
        if (!CategoriesTable.selectAll().where {
                (CategoriesTable.name.lowerCase() eq category.lowercase()) and (CategoriesTable.active eq true)
            }.any()) throw ConflictException("Mapped category is not an active TourVerse category.")
        val possibleDuplicate = DestinationsTable.selectAll().map { it.toDestination() }
            .firstOrNull { DestinationDuplicateDetector.assess(current, it).outcome != "NO_DUPLICATE_FOUND" }
        if (possibleDuplicate != null) {
            DestinationImportCandidatesTable.update({ DestinationImportCandidatesTable.id eq id }) { row ->
                row[reviewStatus] = DestinationImportStatus.POSSIBLE_DUPLICATE.name
                row[duplicateOfDestinationId] = possibleDuplicate.id
                row[updatedAt] = now()
            }
            return@suspendTransaction candidate(id)
        }
        if (current.imageReference != null &&
            (current.imageLicence.isNullOrBlank() || current.imageAttribution.isNullOrBlank())
        ) throw ConflictException("Image licence and attribution are required before approving an imported image.")
        val destinationId = UUID.randomUUID()
        val timestamp = now()
        DestinationsTable.insert { row ->
            row[DestinationsTable.id] = destinationId
            row[name] = current.name
            row[country] = current.country
            row[countryCode] = current.countryCode
            row[city] = current.city
            row[description] = current.descriptionHint?.takeIf(String::isNotBlank)
                ?: "A TourVerse destination awaiting editorial description."
            row[DestinationsTable.category] = category
            row[latitude] = current.latitude?.let(BigDecimal::valueOf)
            row[longitude] = current.longitude?.let(BigDecimal::valueOf)
            row[coverImageUrl] = current.imageReference
            row[dataOrigin] = DataOrigin.EXTERNAL.name
            row[cacheStatus] = CacheStatus.FRESH.name
            row[lastVerifiedAt] = timestamp
            row[expiresAt] = timestamp.plusDays(30)
            row[verificationStatus] = VerificationStatus.VERIFIED.name
            row[verificationConfidence] = BigDecimal.valueOf(0.75)
            row[editoriallyLocked] = false
            row[createdAt] = timestamp
            row[updatedAt] = timestamp
        }
        linkSource(current, destinationId)
        DestinationImportCandidatesTable.update({ DestinationImportCandidatesTable.id eq id }) { row ->
            row[reviewStatus] = DestinationImportStatus.APPROVED.name
            row[approvedDestinationId] = destinationId
            row[reviewedBy] = adminId
            row[updatedAt] = timestamp
        }
        candidate(id)
    }

    override suspend fun listStaleDestinations() = suspendTransaction {
        val current = now()
        DestinationsTable.selectAll().where {
            (DestinationsTable.cacheStatus eq CacheStatus.STALE.name) or
                (DestinationsTable.expiresAt lessEq current)
        }.map { it.toDestination() }
    }

    override suspend fun markRefreshPending(destinationId: UUID) = suspendTransaction {
        val changed = DestinationsTable.update({ DestinationsTable.id eq destinationId }) {
            it[cacheStatus] = CacheStatus.REFRESH_PENDING.name
            it[updatedAt] = now()
        }
        if (changed == 0) null else DestinationsTable.selectAll()
            .where { DestinationsTable.id eq destinationId }.single().toDestination()
    }

    override suspend fun listSources(destinationId: UUID) = suspendTransaction {
        DestinationSourceReferencesTable.selectAll()
            .where { DestinationSourceReferencesTable.destinationId eq destinationId }
            .orderBy(DestinationSourceReferencesTable.createdAt to SortOrder.ASC)
            .map { it.toSourceReference() }
    }

    override suspend fun linkGooglePlace(
        destinationId: UUID,
        adminId: UUID,
        request: LinkGooglePlaceRequest
    ) = suspendTransaction {
        if (!DestinationsTable.selectAll().where { DestinationsTable.id eq destinationId }.any()) {
            return@suspendTransaction null
        }
        DestinationSourceReferencesTable.update({
            (DestinationSourceReferencesTable.destinationId eq destinationId) and
                (DestinationSourceReferencesTable.sourceProvider eq DestinationProvider.GOOGLE_PLACES.name)
        }) { it[active] = false }
        val timestamp = now()
        val existing = DestinationSourceReferencesTable.selectAll().where {
            (DestinationSourceReferencesTable.sourceProvider eq DestinationProvider.GOOGLE_PLACES.name) and
                (DestinationSourceReferencesTable.externalId eq request.placeId)
        }.singleOrNull()
        if (existing != null && existing[DestinationSourceReferencesTable.destinationId] != destinationId) {
            throw ConflictException("Google Place ID is already linked to another destination.")
        }
        val referenceId = existing?.get(DestinationSourceReferencesTable.id) ?: UUID.randomUUID()
        if (existing == null) {
            DestinationSourceReferencesTable.insert { row ->
                row[id] = referenceId
                row[DestinationSourceReferencesTable.destinationId] = destinationId
                row[sourceProvider] = DestinationProvider.GOOGLE_PLACES.name
                row[externalId] = request.placeId
                row[sourceUrl] = request.googleMapsUri
                row[retrievedAt] = timestamp
                row[lastVerifiedAt] = timestamp
                row[attribution] = request.attribution.take(500)
                row[providerPlaceId] = request.placeId
                row[active] = true
                row[createdAt] = timestamp
            }
        } else {
            DestinationSourceReferencesTable.update({
                DestinationSourceReferencesTable.id eq referenceId
            }) { row ->
                row[sourceUrl] = request.googleMapsUri
                row[lastVerifiedAt] = timestamp
                row[attribution] = request.attribution.take(500)
                row[providerPlaceId] = request.placeId
                row[active] = true
            }
        }
        DestinationSourceReferencesTable.selectAll()
            .where { DestinationSourceReferencesTable.id eq referenceId }
            .single().toSourceReference()
    }

    override suspend fun removeGooglePlace(destinationId: UUID) = suspendTransaction {
        DestinationSourceReferencesTable.update({
            (DestinationSourceReferencesTable.destinationId eq destinationId) and
                (DestinationSourceReferencesTable.sourceProvider eq DestinationProvider.GOOGLE_PLACES.name) and
                (DestinationSourceReferencesTable.active eq true)
        }) { it[active] = false } > 0
    }

    private fun linkSource(candidate: DestinationCandidate, destinationId: UUID) {
        val exists = DestinationSourceReferencesTable.selectAll().where {
            (DestinationSourceReferencesTable.sourceProvider eq candidate.sourceProvider) and
                (DestinationSourceReferencesTable.externalId eq candidate.externalId)
        }.any()
        if (!exists) DestinationSourceReferencesTable.insert { row ->
            row[id] = UUID.randomUUID()
            row[DestinationSourceReferencesTable.destinationId] = destinationId
            row[sourceProvider] = candidate.sourceProvider
            row[externalId] = candidate.externalId
            row[sourceUrl] = candidate.sourceUrl
            row[retrievedAt] = OffsetDateTime.ofInstant(candidate.retrievedAt, ZoneOffset.UTC)
            row[lastVerifiedAt] = OffsetDateTime.ofInstant(candidate.retrievedAt, ZoneOffset.UTC)
            row[attribution] = candidate.imageAttribution
            row[licence] = candidate.imageLicence
            row[providerPlaceId] = if (candidate.sourceProvider == DestinationProvider.GOOGLE_PLACES.name) {
                candidate.externalId
            } else null
            row[active] = true
            row[createdAt] = now()
        }
    }

    private fun batch(id: UUID) = DestinationImportBatchesTable.selectAll()
        .where { DestinationImportBatchesTable.id eq id }.singleOrNull()?.toBatch()
    private fun candidate(id: UUID) = DestinationImportCandidatesTable.selectAll()
        .where { DestinationImportCandidatesTable.id eq id }.singleOrNull()?.toCandidate()
    private fun now() = OffsetDateTime.now(ZoneOffset.UTC)
    private fun String.clean() = trim()

    private fun ResultRow.toBatch() = DestinationImportBatch(
        this[DestinationImportBatchesTable.id], this[DestinationImportBatchesTable.provider],
        this[DestinationImportBatchesTable.requestedBy], this[DestinationImportBatchesTable.countryCode],
        this[DestinationImportBatchesTable.city], this[DestinationImportBatchesTable.queryText],
        this[DestinationImportBatchesTable.requestedLimit],
        DestinationImportBatchStatus.valueOf(this[DestinationImportBatchesTable.status]),
        this[DestinationImportBatchesTable.retrievedCount], this[DestinationImportBatchesTable.errorMessage],
        this[DestinationImportBatchesTable.createdAt].toInstant(), this[DestinationImportBatchesTable.updatedAt].toInstant()
    )

    private fun ResultRow.toCandidate() = DestinationCandidate(
        id = this[DestinationImportCandidatesTable.id],
        batchId = this[DestinationImportCandidatesTable.batchId],
        sourceProvider = this[DestinationImportCandidatesTable.sourceProvider],
        externalId = this[DestinationImportCandidatesTable.externalId],
        sourceUrl = this[DestinationImportCandidatesTable.sourceUrl],
        name = this[DestinationImportCandidatesTable.name],
        countryCode = this[DestinationImportCandidatesTable.countryCode],
        country = this[DestinationImportCandidatesTable.country],
        region = this[DestinationImportCandidatesTable.region],
        city = this[DestinationImportCandidatesTable.city],
        latitude = this[DestinationImportCandidatesTable.latitude]?.toDouble(),
        longitude = this[DestinationImportCandidatesTable.longitude]?.toDouble(),
        categoryHint = this[DestinationImportCandidatesTable.categoryHint],
        mappedCategory = this[DestinationImportCandidatesTable.mappedCategory],
        descriptionHint = this[DestinationImportCandidatesTable.descriptionHint],
        officialWebsite = this[DestinationImportCandidatesTable.officialWebsite],
        imageReference = this[DestinationImportCandidatesTable.imageReference],
        imageLicence = this[DestinationImportCandidatesTable.imageLicence],
        imageAttribution = this[DestinationImportCandidatesTable.imageAttribution],
        imageLicenceUrl = this[DestinationImportCandidatesTable.imageLicenceUrl],
        sourceClassifications = this[DestinationImportCandidatesTable.sourceClassifications]
            .lineSequence().filter(String::isNotBlank).toList(),
        retrievedAt = this[DestinationImportCandidatesTable.retrievedAt].toInstant(),
        reviewStatus = DestinationImportStatus.valueOf(this[DestinationImportCandidatesTable.reviewStatus]),
        rejectionReason = this[DestinationImportCandidatesTable.rejectionReason],
        duplicateOfDestinationId = this[DestinationImportCandidatesTable.duplicateOfDestinationId],
        approvedDestinationId = this[DestinationImportCandidatesTable.approvedDestinationId],
        reviewedBy = this[DestinationImportCandidatesTable.reviewedBy],
        createdAt = this[DestinationImportCandidatesTable.createdAt].toInstant(),
        updatedAt = this[DestinationImportCandidatesTable.updatedAt].toInstant()
    )

    private fun ResultRow.toSourceReference() = DestinationSourceReference(
        id = this[DestinationSourceReferencesTable.id],
        destinationId = this[DestinationSourceReferencesTable.destinationId],
        provider = DestinationProvider.valueOf(this[DestinationSourceReferencesTable.sourceProvider]),
        externalId = this[DestinationSourceReferencesTable.externalId],
        sourceUrl = this[DestinationSourceReferencesTable.sourceUrl],
        retrievedAt = this[DestinationSourceReferencesTable.retrievedAt].toInstant(),
        lastVerifiedAt = this[DestinationSourceReferencesTable.lastVerifiedAt]?.toInstant(),
        providerContentUpdatedAt = this[DestinationSourceReferencesTable.providerContentUpdatedAt]?.toInstant(),
        attribution = this[DestinationSourceReferencesTable.attribution],
        licence = this[DestinationSourceReferencesTable.licence],
        providerPlaceId = this[DestinationSourceReferencesTable.providerPlaceId],
        metadataHash = this[DestinationSourceReferencesTable.metadataHash],
        active = this[DestinationSourceReferencesTable.active]
    )

    private fun ResultRow.toDestination() = Destination(
        id = this[DestinationsTable.id], name = this[DestinationsTable.name],
        country = this[DestinationsTable.country], city = this[DestinationsTable.city],
        description = this[DestinationsTable.description], category = this[DestinationsTable.category],
        latitude = this[DestinationsTable.latitude]?.toDouble(), longitude = this[DestinationsTable.longitude]?.toDouble(),
        coverImageUrl = this[DestinationsTable.coverImageUrl], countryCode = this[DestinationsTable.countryCode],
        createdAt = this[DestinationsTable.createdAt].toInstant(), updatedAt = this[DestinationsTable.updatedAt].toInstant(),
        dataOrigin = DataOrigin.valueOf(this[DestinationsTable.dataOrigin]),
        lastVerifiedAt = this[DestinationsTable.lastVerifiedAt]?.toInstant(),
        verificationStatus = VerificationStatus.valueOf(this[DestinationsTable.verificationStatus])
    )
}
