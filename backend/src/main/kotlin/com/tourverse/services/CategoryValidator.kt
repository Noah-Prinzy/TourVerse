package com.tourverse.services

import com.tourverse.models.CreateCategoryRequest
import com.tourverse.models.UpdateCategoryRequest
import com.tourverse.utils.ValidationException
import java.net.URI

object CategoryValidator {
    fun validate(request: CreateCategoryRequest) {
        validateName(request.name)
        validateDescription(request.description)
        validateIcon(request.iconUrl)
    }

    fun validate(request: UpdateCategoryRequest) {
        if (request.name == null && request.description == null && request.iconUrl == null && request.active == null) {
            throw ValidationException("At least one category field must be supplied")
        }
        request.name?.let(::validateName)
        validateDescription(request.description)
        validateIcon(request.iconUrl)
    }

    fun slug(name: String): String = name.trim().lowercase()
        .replace("&", " and ")
        .replace(Regex("[^a-z0-9]+"), "-").trim('-')

    private fun validateName(value: String) {
        if (value.trim().length !in 2..80) throw ValidationException("Category name must contain 2 to 80 characters")
        if (slug(value).isBlank()) throw ValidationException("Category name must contain letters or numbers")
    }
    private fun validateDescription(value: String?) {
        if (value != null && value.trim().length > 500) throw ValidationException("Category description must not exceed 500 characters")
    }
    private fun validateIcon(value: String?) {
        val text = value?.trim()?.takeIf(String::isNotEmpty) ?: return
        val uri = runCatching { URI(text) }.getOrNull()
        if (uri == null || uri.scheme !in setOf("http", "https") || uri.host.isNullOrBlank()) {
            throw ValidationException("Category icon URL must be a valid HTTP or HTTPS URL")
        }
    }
}
