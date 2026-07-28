package com.tourverse.models

import kotlinx.serialization.Serializable

@Serializable
data class CreateDestinationRequest(
    val name: String,
    val country: String,
    val city: String? = null,
    val description: String,
    val category: String,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val coverImageUrl: String? = null
)