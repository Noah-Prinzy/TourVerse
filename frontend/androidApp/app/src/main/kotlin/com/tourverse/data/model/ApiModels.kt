package com.tourverse.data.model

import kotlinx.serialization.Serializable

@Serializable
data class PagedDestinationResponse(
    val items: List<Destination>,
    val page: Int,
    val size: Int,
    val totalItems: Long,
    val totalPages: Int
)

@Serializable
data class ApiMessage(
    val status: String,
    val message: String
)
