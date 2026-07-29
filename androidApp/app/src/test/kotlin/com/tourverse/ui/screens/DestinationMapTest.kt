package com.tourverse.ui.screens

import com.tourverse.data.model.Destination
import kotlin.test.*

class DestinationMapTest {
    private fun destination(latitude: Double?, longitude: Double?, placeId: String? = null) =
        Destination(
            id = "00000000-0000-0000-0000-000000000001",
            name = "Queen Elizabeth National Park",
            country = "Uganda",
            description = "Destination",
            category = "Wildlife",
            latitude = latitude,
            longitude = longitude,
            createdAt = "2026-01-01T00:00:00Z",
            updatedAt = "2026-01-01T00:00:00Z",
            googlePlaceId = placeId
        )

    @Test fun `map state handles configuration and coordinate failures`() {
        assertEquals(DestinationMapState.MISSING_KEY, destinationMapState(false, 0.2, 32.1))
        assertEquals(DestinationMapState.MISSING_COORDINATES, destinationMapState(true, null, null))
        assertEquals(DestinationMapState.INVALID_COORDINATES, destinationMapState(true, 91.0, 32.1))
        assertEquals(DestinationMapState.READY, destinationMapState(true, 0.2, 32.1))
    }

    @Test fun `external map intents are encoded and have browser fallback`() {
        val uris = googleMapsUris(destination(-0.2, 29.9, "ChIJ_example"))
        assertEquals(2, uris.size)
        assertTrue(uris.first().startsWith("geo:-0.2,29.9"))
        assertTrue(uris.last().startsWith("https://www.google.com/maps/search/"))
        assertTrue("query_place_id=ChIJ_example" in uris.last())
        assertTrue(googleMapsUris(destination(100.0, 0.0)).isEmpty())
    }
}
