package com.tourverse.data.repository

import com.tourverse.data.model.DestinationQuery
import com.tourverse.data.model.PagedDestinationResponse
import com.tourverse.data.model.DestinationCountriesResponse
import com.tourverse.data.remote.TourismApi

class DestinationRepository(
    private val api: TourismApi = TourismApi()
) {
    suspend fun getDestinations(query: DestinationQuery): PagedDestinationResponse =
        api.getDestinations(query)

    suspend fun getCountries(): DestinationCountriesResponse = api.getDestinationCountries()
}
