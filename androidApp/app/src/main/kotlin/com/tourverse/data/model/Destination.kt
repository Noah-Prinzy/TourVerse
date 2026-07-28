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
    val updatedAt: String
) {
    val displayLocation: String
        get() = listOfNotNull(city?.trim()?.takeIf(String::isNotEmpty), country.trim())
            .joinToString(", ")
}
