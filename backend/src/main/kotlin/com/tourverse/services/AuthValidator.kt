package com.tourverse.services

import com.tourverse.models.RegisterRequest
import com.tourverse.models.UpdateProfileRequest
import com.tourverse.utils.ValidationException

object AuthValidator {
    private val emailPattern = Regex("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")

    fun validateRegistration(request: RegisterRequest) {
        if (request.firstName.trim().length !in 2..80) {
            throw ValidationException("First name must contain between 2 and 80 characters.")
        }
        if (request.lastName.trim().length !in 2..80) {
            throw ValidationException("Last name must contain between 2 and 80 characters.")
        }
        if (!emailPattern.matches(request.email.trim())) {
            throw ValidationException("A valid email address is required.")
        }
        validatePassword(request.password)
    }

    fun validatePassword(password: String) {
        if (password.length !in 8..128 || !password.any(Char::isUpperCase) ||
            !password.any(Char::isLowerCase) || !password.any(Char::isDigit)
        ) {
            throw ValidationException(
                "Password must contain 8 to 128 characters, including uppercase, lowercase, and a number."
            )
        }
    }

    fun validateProfile(request: UpdateProfileRequest) {
        request.firstName?.let {
            if (it.trim().length !in 2..80) throw ValidationException("First name must contain between 2 and 80 characters.")
        }
        request.lastName?.let {
            if (it.trim().length !in 2..80) throw ValidationException("Last name must contain between 2 and 80 characters.")
        }
        request.bio?.let {
            if (it.length > 500) throw ValidationException("Biography cannot exceed 500 characters.")
        }
        request.profileImageUrl?.takeIf(String::isNotBlank)?.let {
            if (!it.startsWith("https://") && !it.startsWith("http://")) {
                throw ValidationException("Profile image URL must begin with http:// or https://.")
            }
        }
    }
}
