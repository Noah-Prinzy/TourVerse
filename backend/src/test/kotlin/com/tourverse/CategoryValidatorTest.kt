package com.tourverse

import com.tourverse.models.CreateCategoryRequest
import com.tourverse.services.CategoryValidator
import com.tourverse.utils.ValidationException
import kotlin.test.*

class CategoryValidatorTest {
    @Test fun createsStableSlug() { assertEquals("food-and-drink", CategoryValidator.slug(" Food & Drink ")) }
    @Test fun acceptsValidCategory() { CategoryValidator.validate(CreateCategoryRequest("Wildlife", iconUrl = "https://example.com/icon.png")) }
    @Test fun rejectsInvalidIcon() { assertFailsWith<ValidationException> { CategoryValidator.validate(CreateCategoryRequest("Nature", iconUrl = "icon")) } }
}
