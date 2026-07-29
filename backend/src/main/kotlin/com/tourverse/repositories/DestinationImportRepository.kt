package com.tourverse.repositories

import com.tourverse.models.*
import java.util.UUID

interface DestinationImportRepository {
    suspend fun createBatch(adminId: UUID, query: DestinationImportQuery): DestinationImportBatch
    suspend fun saveCandidates(batchId: UUID, candidates: List<DestinationCandidate>): List<DestinationCandidate>
    suspend fun completeBatch(batchId: UUID, retrievedCount: Int, error: String? = null): DestinationImportBatch
    suspend fun listBatches(): List<DestinationImportBatch>
    suspend fun getBatch(id: UUID): DestinationImportBatch?
    suspend fun listCandidates(batchId: UUID? = null, status: DestinationImportStatus? = null): List<DestinationCandidate>
    suspend fun getCandidate(id: UUID): DestinationCandidate?
    suspend fun updateCandidate(id: UUID, request: UpdateDestinationCandidateRequest): DestinationCandidate?
    suspend fun rejectCandidate(id: UUID, adminId: UUID, reason: String): DestinationCandidate?
    suspend fun linkCandidate(id: UUID, adminId: UUID, destinationId: UUID): DestinationCandidate?
    suspend fun approveCandidate(id: UUID, adminId: UUID): DestinationCandidate?
    suspend fun listStaleDestinations(): List<Destination> = emptyList()
    suspend fun markRefreshPending(destinationId: UUID): Destination? = null
    suspend fun listSources(destinationId: UUID): List<DestinationSourceReference> = emptyList()
    suspend fun linkGooglePlace(
        destinationId: UUID,
        adminId: UUID,
        request: LinkGooglePlaceRequest
    ): DestinationSourceReference? = null
    suspend fun removeGooglePlace(destinationId: UUID): Boolean = false
}
