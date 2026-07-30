package com.tourverse.models

import kotlinx.serialization.Serializable

@Serializable
// Carries update destination request values between application layers.
data class UpdateDestinationRequest(
    val name: String,
    val country: String,
    val countryCode: String? = null,
    val city: String? = null,
    val description: String,
    val category: String,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val coverImageUrl: String? = null
)
