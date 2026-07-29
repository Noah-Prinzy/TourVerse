package com.tourverse

import com.tourverse.models.DestinationSortField
import com.tourverse.models.SortDirection
import com.tourverse.routes.parseDestinationQuery
import com.tourverse.utils.ValidationException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class DestinationQueryTest {

    @Test
    fun `default query uses first page and descending creation date`() {
        val query = parseDestinationQuery(null, null, null, null, null, null, null, null, null)

        assertEquals(1, query.page)
        assertEquals(20, query.size)
        assertEquals(DestinationSortField.CREATED_AT, query.sortBy)
        assertEquals(SortDirection.DESC, query.sortDirection)
    }

    @Test
    fun `query accepts pagination and sorting`() {
        val query = parseDestinationQuery(
            search = "Nile",
            country = "Uganda",
            countryCode = "UG",
            city = null,
            category = "Adventure",
            pageValue = "2",
            sizeValue = "10",
            sortByValue = "name",
            sortDirectionValue = "asc"
        )

        assertEquals(2, query.page)
        assertEquals(10, query.size)
        assertEquals(DestinationSortField.NAME, query.sortBy)
        assertEquals(SortDirection.ASC, query.sortDirection)
        assertEquals(10L, query.offset)
    }

    @Test
    fun `page below one is rejected`() {
        assertFailsWith<ValidationException> {
            parseDestinationQuery(null, null, null, null, null, "0", "20", null, null)
        }
    }
}
