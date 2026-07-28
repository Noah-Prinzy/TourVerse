package com.tourverse.models

import com.tourverse.utils.InstantSerializer
import com.tourverse.utils.UUIDSerializer
import kotlinx.serialization.Serializable
import java.time.Instant
import java.util.UUID

@Serializable
data class CategoryResponse(
    @Serializable(with = UUIDSerializer::class) val id: UUID,
    val name: String,
    val slug: String,
    val description: String?,
    val iconUrl: String?,
    val active: Boolean,
    @Serializable(with = InstantSerializer::class) val createdAt: Instant,
    @Serializable(with = InstantSerializer::class) val updatedAt: Instant
)

@Serializable
data class CreateCategoryRequest(
    val name: String,
    val description: String? = null,
    val iconUrl: String? = null,
    val active: Boolean = true
)

@Serializable
data class UpdateCategoryRequest(
    val name: String? = null,
    val description: String? = null,
    val iconUrl: String? = null,
    val active: Boolean? = null
)
