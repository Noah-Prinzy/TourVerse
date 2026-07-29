package com.tourverse.data.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class DestinationModelTest {
    @Test
    fun displayLocationCombinesCityAndCountry() {
        assertEquals("Nairobi, Kenya", destination(city = "Nairobi").displayLocation)
    }

    @Test
    fun displayLocationUsesCountryWhenCityIsBlank() {
        assertEquals("Kenya", destination(city = "  ").displayLocation)
    }

    @Test
    fun queryRejectsInvalidPagination() {
        assertFailsWith<IllegalArgumentException> { DestinationQuery(page = 0) }
        assertFailsWith<IllegalArgumentException> { DestinationQuery(size = 101) }
    }

    private fun destination(city: String?) = Destination(
        id = "c785d75a-8824-45de-8a93-e9a27a488a45",
        name = "Test destination",
        country = "Kenya",
        city = city,
        description = "Description",
        category = "Nature",
        createdAt = "2026-07-28T00:00:00Z",
        updatedAt = "2026-07-28T00:00:00Z"
    )
}
