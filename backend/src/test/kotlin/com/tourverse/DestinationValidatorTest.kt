package com.tourverse

import com.tourverse.models.CreateDestinationRequest
import com.tourverse.services.DestinationValidator
import com.tourverse.utils.ValidationException
import kotlin.test.Test
import kotlin.test.assertFailsWith

class DestinationValidatorTest {

    @Test
    fun `valid destination passes validation`() {
        DestinationValidator.validate(
            CreateDestinationRequest(
                name = "Murchison Falls National Park",
                country = "Uganda",
                city = "Masindi",
                description = "A national park known for wildlife and the Nile waterfall.",
                category = "Wildlife",
                latitude = 2.2530,
                longitude = 31.8057,
                coverImageUrl = "https://example.com/murchison.jpg"
            )
        )
    }

    @Test
    fun `blank name is rejected`() {
        assertFailsWith<ValidationException> {
            DestinationValidator.validate(
                CreateDestinationRequest(
                    name = " ",
                    country = "Uganda",
                    description = "A valid description.",
                    category = "Nature"
                )
            )
        }
    }

    @Test
    fun `invalid coordinates are rejected`() {
        assertFailsWith<ValidationException> {
            DestinationValidator.validate(
                CreateDestinationRequest(
                    name = "Test Destination",
                    country = "Uganda",
                    description = "A valid description.",
                    category = "Nature",
                    latitude = 95.0,
                    longitude = 31.0
                )
            )
        }
    }

    @Test
    fun `one coordinate without the other is rejected`() {
        assertFailsWith<ValidationException> {
            DestinationValidator.validate(
                CreateDestinationRequest(
                    name = "Test Destination",
                    country = "Uganda",
                    description = "A valid description.",
                    category = "Nature",
                    latitude = 1.0
                )
            )
        }
    }
}
