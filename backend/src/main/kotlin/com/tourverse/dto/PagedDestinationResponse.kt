package com.tourverse.dto

import com.tourverse.models.Destination
import kotlinx.serialization.Serializable

@Serializable
data class PagedDestinationResponse(
    val items: List<Destination>,
    val page: Int,
    val size: Int,
    val totalItems: Long,
    val totalPages: Int
)
