package com.tourverse.data.repository

import com.tourverse.data.model.Destination
import com.tourverse.data.remote.TourismApi

class DestinationRepository(
    private val api: TourismApi = TourismApi()
) {
    suspend fun getDestinations(): List<Destination> {
        return api.getDestinations()
    }
}
