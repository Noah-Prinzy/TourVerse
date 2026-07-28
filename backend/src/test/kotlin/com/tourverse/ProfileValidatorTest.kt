package com.tourverse

import com.tourverse.models.UpdateProfileImageRequest
import com.tourverse.models.UpdateUserProfileRequest
import com.tourverse.services.ProfileValidator
import com.tourverse.utils.ValidationException
import kotlin.test.Test
import kotlin.test.assertFailsWith

class ProfileValidatorTest {
    @Test fun acceptsValidProfile() { ProfileValidator.validate(UpdateUserProfileRequest(nationality = "Ugandan", travelInterests = listOf("Wildlife", "Culture"))) }
    @Test fun rejectsTooManyInterests() { assertFailsWith<ValidationException> { ProfileValidator.validate(UpdateUserProfileRequest(travelInterests = (1..21).map { "Interest $it" })) } }
    @Test fun rejectsInvalidImageUrl() { assertFailsWith<ValidationException> { ProfileValidator.validateImage(UpdateProfileImageRequest("not-a-url")) } }
}
