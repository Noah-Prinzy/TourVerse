package com.tourverse

import com.tourverse.models.Destination
import com.tourverse.models.DestinationCandidate
import com.tourverse.services.DestinationDuplicateDetector
import java.time.Instant
import java.util.UUID
import kotlin.test.*

class DestinationDuplicateDetectorTest {
    private fun destination(name: String, code: String, latitude: Double? = null, longitude: Double? = null) =
        Destination(UUID.randomUUID(), name, "Country", null, "Description", "Nature",
            latitude, longitude, null, Instant.EPOCH, Instant.EPOCH, code)

    private fun candidate(name: String, code: String, latitude: Double? = null, longitude: Double? = null) =
        DestinationCandidate(sourceProvider = "FIXTURE", externalId = UUID.randomUUID().toString(),
            name = name, country = "Country", countryCode = code, latitude = latitude, longitude = longitude)

    @Test fun `accented punctuation and whitespace normalize`() {
        assertEquals("musee national", DestinationDuplicateDetector.normalize(" Musée---National "))
    }
    @Test fun `same normalized name and country requires review`() {
        assertEquals("POSSIBLE_DUPLICATE",
            DestinationDuplicateDetector.assess(candidate("Murchison Falls N P", "UG"),
                destination("Murchison Falls N.P.", "UG")).outcome)
    }
    @Test fun `same name in different countries is not duplicate`() {
        assertEquals("NO_DUPLICATE_FOUND",
            DestinationDuplicateDetector.assess(candidate("National Museum", "KE"),
                destination("National Museum", "UG")).outcome)
    }
    @Test fun `near coordinates in same country are likely duplicate`() {
        assertEquals("LIKELY_DUPLICATE",
            DestinationDuplicateDetector.assess(candidate("Falls", "UG", 0.1, 32.1),
                destination("Waterfall", "UG", 0.1005, 32.1005)).outcome)
    }
    @Test fun `far coordinates and missing coordinates are not duplicate`() {
        assertEquals("NO_DUPLICATE_FOUND",
            DestinationDuplicateDetector.assess(candidate("One", "UG", 0.0, 0.0),
                destination("Two", "UG", 5.0, 5.0)).outcome)
        assertEquals("NO_DUPLICATE_FOUND",
            DestinationDuplicateDetector.assess(candidate("One", "UG"),
                destination("Two", "UG")).outcome)
    }
}
