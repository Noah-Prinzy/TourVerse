package com.tourverse.repositories

import com.tourverse.dto.PagedDestinationResponse
import com.tourverse.models.CreateDestinationRequest
import com.tourverse.models.Destination
import com.tourverse.models.DestinationCountry
import com.tourverse.models.DestinationQuery
import com.tourverse.models.UpdateDestinationRequest
import java.util.UUID

interface DestinationRepository {

    // Retrieves all from persistent or request state.
    suspend fun getAll(query: DestinationQuery): PagedDestinationResponse

    // Retrieves by id from persistent or request state.
    suspend fun getById(id: UUID): Destination?

    // Retrieves countries from persistent or request state.
    suspend fun getCountries(): List<DestinationCountry>

    // Creates create and returns the resulting domain value.
    suspend fun create(request: CreateDestinationRequest): Destination

    // Updates update within the current transaction or request.
    suspend fun update(id: UUID, request: UpdateDestinationRequest): Destination?

    // Removes or invalidates delete for the requested resource.
    suspend fun delete(id: UUID): Boolean
}
