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

    suspend fun getAllDestinations(query: DestinationQuery): PagedDestinationResponse =
        repository.getAll(query)

    suspend fun getDestinationById(id: UUID): Destination? =
        repository.getById(id)

    suspend fun getCountries() = repository.getCountries()

    suspend fun createDestination(request: CreateDestinationRequest): Destination {
        DestinationValidator.validate(request)
        return repository.create(request)
    }

    suspend fun updateDestination(id: UUID, request: UpdateDestinationRequest): Destination? {
        DestinationValidator.validate(request)
        return repository.update(id, request)
    }

    suspend fun deleteDestination(id: UUID): Boolean =
        repository.delete(id)
}
