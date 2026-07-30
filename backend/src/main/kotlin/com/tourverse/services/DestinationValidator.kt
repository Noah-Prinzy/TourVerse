package com.tourverse.services

import com.tourverse.models.CreateDestinationRequest
import com.tourverse.models.UpdateDestinationRequest
import com.tourverse.utils.ValidationException
import java.net.URI

object DestinationValidator {

    // Validates validate and stops the workflow when input is invalid.
    fun validate(request: CreateDestinationRequest) {
        validateFields(
            name = request.name,
            country = request.country,
            countryCode = request.countryCode,
            city = request.city,
            description = request.description,
            category = request.category,
            latitude = request.latitude,
            longitude = request.longitude,
            coverImageUrl = request.coverImageUrl
        )
    }

    // Validates validate and stops the workflow when input is invalid.
    fun validate(request: UpdateDestinationRequest) {
        validateFields(
            name = request.name,
            country = request.country,
            countryCode = request.countryCode,
            city = request.city,
            description = request.description,
            category = request.category,
            latitude = request.latitude,
            longitude = request.longitude,
            coverImageUrl = request.coverImageUrl
        )
    }

    // Validates fields and stops the workflow when input is invalid.
    private fun validateFields(
        name: String,
        country: String,
        countryCode: String?,
        city: String?,
        description: String,
        category: String,
        latitude: Double?,
        longitude: Double?,
        coverImageUrl: String?
    ) {
        requireText(name, "Destination name", 150)
        requireText(country, "Country", 100)
        CountryCodeService.normalizeCode(countryCode)
        optionalText(city, "City", 100)
        requireText(description, "Description", 5_000)
        requireText(category, "Category", 80)

        if (latitude != null && latitude !in -90.0..90.0) {
            throw ValidationException("Latitude must be between -90 and 90.")
        }

        if (longitude != null && longitude !in -180.0..180.0) {
            throw ValidationException("Longitude must be between -180 and 180.")
        }

        if ((latitude == null) != (longitude == null)) {
            throw ValidationException("Latitude and longitude must be provided together.")
        }

        coverImageUrl?.trim()?.takeIf(String::isNotEmpty)?.let { validateHttpUrl(it) }
    }

    // Validates text and stops the workflow when input is invalid.
    private fun requireText(value: String, field: String, maxLength: Int) {
        val cleanValue = value.trim()
        if (cleanValue.isEmpty()) {
            throw ValidationException("$field must not be blank.")
        }
        if (cleanValue.length > maxLength) {
            throw ValidationException("$field must not exceed $maxLength characters.")
        }
    }

    // Coordinates the optional text business workflow for callers.
    private fun optionalText(value: String?, field: String, maxLength: Int) {
        value?.trim()?.takeIf(String::isNotEmpty)?.let {
            if (it.length > maxLength) {
                throw ValidationException("$field must not exceed $maxLength characters.")
            }
        }
    }

    // Validates http url and stops the workflow when input is invalid.
    private fun validateHttpUrl(value: String) {
        val uri = runCatching { URI(value) }.getOrNull()
        val validScheme = uri?.scheme.equals("http", ignoreCase = true) ||
            uri?.scheme.equals("https", ignoreCase = true)

        if (uri == null || !validScheme || uri.host.isNullOrBlank()) {
            throw ValidationException("Cover image URL must be a valid HTTP or HTTPS URL.")
        }
    }
}
