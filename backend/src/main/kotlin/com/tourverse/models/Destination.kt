package com.tourverse.models

import com.tourverse.utils.InstantSerializer
import com.tourverse.utils.UUIDSerializer
import kotlinx.serialization.Serializable
import java.time.Instant
import java.util.UUID

@Serializable
data class Destination(
    @Serializable(with = UUIDSerializer::class)
    val id: UUID,
    val name: String,
    val country: String,
    val city: String?,
    val description: String,
    val category: String,
    val latitude: Double?,
    val longitude: Double?,
    val coverImageUrl: String?,
    @Serializable(with = InstantSerializer::class)
    val createdAt: Instant,
    @Serializable(with = InstantSerializer::class)
    val updatedAt: Instant
)
