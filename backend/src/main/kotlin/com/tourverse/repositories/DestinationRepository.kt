package com.tourverse.repositories

import com.tourverse.dto.PagedDestinationResponse
import com.tourverse.models.CreateDestinationRequest
import com.tourverse.models.Destination
import com.tourverse.models.DestinationCountry
import com.tourverse.models.DestinationQuery
import com.tourverse.models.UpdateDestinationRequest
import java.util.UUID

interface DestinationRepository {

    suspend fun getAll(query: DestinationQuery): PagedDestinationResponse

    suspend fun getById(id: UUID): Destination?

    suspend fun getCountries(): List<DestinationCountry>

    suspend fun create(request: CreateDestinationRequest): Destination

    suspend fun update(id: UUID, request: UpdateDestinationRequest): Destination?

    suspend fun delete(id: UUID): Boolean
}
