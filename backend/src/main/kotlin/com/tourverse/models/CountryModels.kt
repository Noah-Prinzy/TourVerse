package com.tourverse.models

import kotlinx.serialization.Serializable

@Serializable
data class DestinationCountry(
    val code: String,
    val name: String,
    val destinationCount: Long
)

@Serializable
data class DestinationCountriesResponse(
    val countries: List<DestinationCountry>
)
