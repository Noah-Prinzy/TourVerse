package com.tourverse

import com.tourverse.models.RegisterRequest
import com.tourverse.services.AuthValidator
import com.tourverse.utils.ValidationException
import kotlin.test.Test
import kotlin.test.assertFailsWith

class AuthValidatorTest {
    @Test
    fun `registration rejects weak password`() {
        assertFailsWith<ValidationException> {
            AuthValidator.validateRegistration(RegisterRequest("Noah", "Prince", "noah@example.com", "password"))
        }
    }

    @Test
    fun `registration accepts valid data`() {
        AuthValidator.validateRegistration(RegisterRequest("Noah", "Prince", "noah@example.com", "Secure123"))
    }

    @Test
    fun `registration rejects excessively long password`() {
        assertFailsWith<ValidationException> {
            AuthValidator.validateRegistration(
                RegisterRequest("Noah", "Prince", "noah@example.com", "A1" + "a".repeat(127))
            )
        }
    }
}
