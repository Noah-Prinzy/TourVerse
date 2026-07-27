package com.tourverse.data.remote

import com.tourverse.BuildConfig
import com.tourverse.data.model.Destination
import io.ktor.client.call.body
import io.ktor.client.request.get

class TourismApi {

    companion object {
        private val baseUrl = BuildConfig.API_BASE_URL
            .trim()
            .also {
                require(it.isNotEmpty()) {
                    "API base URL is not configured for the ${BuildConfig.FLAVOR} build."
                }
            }
            .let { if (it.endsWith('/')) it else "$it/" }
    }

    suspend fun getDestinations(): List<Destination> {
        return ApiClient.client
            .get("${baseUrl}api/destinations")
            .body()
    }
}
