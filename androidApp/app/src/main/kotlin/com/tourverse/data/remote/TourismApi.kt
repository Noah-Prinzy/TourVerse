package com.tourverse.data.remote

import com.tourverse.data.model.Destination
import io.ktor.client.call.body
import io.ktor.client.request.get

class TourismApi {

    private val baseUrl = "http://10.0.2.2:8080"

    suspend fun getDestinations(): List<Destination> {
        return ApiClient.client
            .get("$baseUrl/api/destinations")
            .body()
    }
}
