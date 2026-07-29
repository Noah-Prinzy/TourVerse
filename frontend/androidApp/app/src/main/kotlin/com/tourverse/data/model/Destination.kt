package com.tourverse.data.model

import kotlinx.serialization.Serializable

@Serializable
data class Destination(
    val id: String,
    val name: String,
    val country: String,
    val city: String? = null,
    val description: String,
    val category: String,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val coverImageUrl: String? = null,
    val createdAt: String,
    val updatedAt: String,
    val countryCode: String? = null,
    val dataOrigin: String = "TOURVERSE_CURATED",
    val lastVerifiedAt: String? = null,
    val verificationStatus: String = "VERIFIED",
    val attributionSummary: String? = null,
    val mapAvailable: Boolean = latitude != null && longitude != null,
    val googlePlaceId: String? = null
) {
    val displayLocation: String
        get() = listOfNotNull(city?.trim()?.takeIf(String::isNotEmpty), country.trim())
            .joinToString(", ")
}

@Serializable
data class DestinationCountry(
    val code: String,
    val name: String,
    val destinationCount: Long
)

@Serializable
data class DestinationCountriesResponse(val countries: List<DestinationCountry>)
