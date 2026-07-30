package com.tourverse.dto

import kotlinx.serialization.Serializable

@Serializable
// Carries api message values between application layers.
data class ApiMessage(
    val status: String,
    val message: String
)
