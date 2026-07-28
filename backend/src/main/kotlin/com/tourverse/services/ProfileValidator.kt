package com.tourverse.services

import com.tourverse.models.UpdateProfileImageRequest
import com.tourverse.models.UpdateUserProfileRequest
import com.tourverse.utils.ValidationException
import java.net.URI

object ProfileValidator {
    fun validate(request: UpdateUserProfileRequest) {
        request.firstName?.let { if (it.trim().length !in 2..80) fail("First name must contain 2 to 80 characters") }
        request.lastName?.let { if (it.trim().length !in 2..80) fail("Last name must contain 2 to 80 characters") }
        request.bio?.let { if (it.trim().length > 500) fail("Bio must not exceed 500 characters") }
        request.nationality?.let { if (it.trim().length > 100) fail("Nationality must not exceed 100 characters") }
        request.travelInterests?.let { interests ->
            if (interests.size > 20) fail("A profile can contain at most 20 travel interests")
            if (interests.any { it.trim().isEmpty() || it.trim().length > 50 }) fail("Each travel interest must contain 1 to 50 characters")
        }
    }

    fun validateImage(request: UpdateProfileImageRequest) {
        val value = request.profileImageUrl?.trim()?.takeIf(String::isNotEmpty) ?: return
        val uri = runCatching { URI(value) }.getOrNull()
        if (uri == null || uri.scheme !in setOf("http", "https") || uri.host.isNullOrBlank()) {
            fail("Profile image URL must be a valid HTTP or HTTPS URL")
        }
    }

    private fun fail(message: String): Nothing = throw ValidationException(message)
}
