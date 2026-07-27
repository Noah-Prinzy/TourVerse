package com.tourverse.data.model

import kotlinx.serialization.Serializable

@Serializable
data class Destination(
    val id: Int,
    val name: String,
    val description: String,
    val location: String,
    val category: String,
    val imageUrl: String,
    val latitude: Double,
    val longitude: Double,
    val rating: Double
)
