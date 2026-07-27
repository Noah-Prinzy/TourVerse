package com.tourverse.dto

import kotlinx.serialization.Serializable

@Serializable
data class ApiMessage(
    val status: String,
    val message: String
)
