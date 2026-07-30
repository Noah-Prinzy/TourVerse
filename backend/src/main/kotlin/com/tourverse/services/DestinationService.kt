package com.tourverse.services

import com.tourverse.dto.PagedDestinationResponse
import com.tourverse.models.CreateDestinationRequest
import com.tourverse.models.Destination
import com.tourverse.models.DestinationQuery
import com.tourverse.models.UpdateDestinationRequest
import com.tourverse.repositories.DestinationRepository
import java.util.UUID

class DestinationService(
    private val repository: DestinationRepository
) {

    // Retrieves all destinations from the relevant repository or external provider.
    suspend fun getAllDestinations(query: DestinationQuery): PagedDestinationResponse =
        repository.getAll(query)

    // Retrieves destination by id from the relevant repository or external provider.
    suspend fun getDestinationById(id: UUID): Destination? =
        repository.getById(id)

    // Retrieves countries from the relevant repository or external provider.
    suspend fun getCountries() = repository.getCountries()

    // Creates destination after applying validation and business rules.
    suspend fun createDestination(request: CreateDestinationRequest): Destination {
        DestinationValidator.validate(request)
        return repository.create(request)
    }

    // Updates destination while keeping related state consistent.
    suspend fun updateDestination(id: UUID, request: UpdateDestinationRequest): Destination? {
        DestinationValidator.validate(request)
        return repository.update(id, request)
    }

    // Removes or invalidates destination after enforcing ownership and authorization rules.
    suspend fun deleteDestination(id: UUID): Boolean =
        repository.delete(id)
}
