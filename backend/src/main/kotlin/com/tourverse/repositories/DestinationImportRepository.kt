package com.tourverse.repositories

import com.tourverse.models.*
import java.util.UUID

interface DestinationImportRepository {
    // Creates batch and returns the resulting domain value.
    suspend fun createBatch(adminId: UUID, query: DestinationImportQuery): DestinationImportBatch
    // Creates candidates and returns the resulting domain value.
    suspend fun saveCandidates(batchId: UUID, candidates: List<DestinationCandidate>): List<DestinationCandidate>
    // Encapsulates the complete batch operation behind a reusable function.
    suspend fun completeBatch(batchId: UUID, retrievedCount: Int, error: String? = null): DestinationImportBatch
    // Retrieves batches from persistent or request state.
    suspend fun listBatches(): List<DestinationImportBatch>
    // Retrieves batch from persistent or request state.
    suspend fun getBatch(id: UUID): DestinationImportBatch?
    // Retrieves candidates from persistent or request state.
    suspend fun listCandidates(batchId: UUID? = null, status: DestinationImportStatus? = null): List<DestinationCandidate>
    // Retrieves candidate from persistent or request state.
    suspend fun getCandidate(id: UUID): DestinationCandidate?
    // Updates candidate within the current transaction or request.
    suspend fun updateCandidate(id: UUID, request: UpdateDestinationCandidateRequest): DestinationCandidate?
    // Updates candidate within the current transaction or request.
    suspend fun rejectCandidate(id: UUID, adminId: UUID, reason: String): DestinationCandidate?
    // Updates candidate within the current transaction or request.
    suspend fun linkCandidate(id: UUID, adminId: UUID, destinationId: UUID): DestinationCandidate?
    // Updates candidate within the current transaction or request.
    suspend fun approveCandidate(id: UUID, adminId: UUID): DestinationCandidate?
    // Retrieves stale destinations from persistent or request state.
    suspend fun listStaleDestinations(): List<Destination> = emptyList()
    // Updates refresh pending within the current transaction or request.
    suspend fun markRefreshPending(destinationId: UUID): Destination? = null
    // Retrieves sources from persistent or request state.
    suspend fun listSources(destinationId: UUID): List<DestinationSourceReference> = emptyList()
    // Updates google place within the current transaction or request.
    suspend fun linkGooglePlace(
        destinationId: UUID,
        adminId: UUID,
        request: LinkGooglePlaceRequest
    ): DestinationSourceReference? = null
    // Removes or invalidates google place for the requested resource.
    suspend fun removeGooglePlace(destinationId: UUID): Boolean = false
}
