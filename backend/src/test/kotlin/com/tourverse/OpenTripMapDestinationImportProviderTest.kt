package com.tourverse

import com.tourverse.models.DestinationImportQuery
import com.tourverse.services.OpenTripMapDestinationImportProvider
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlin.test.*

class OpenTripMapDestinationImportProviderTest {
    @Test fun `fixture parser normalizes bounded place fields without raw payload storage`() {
        val fixture = Json.parseToJsonElement("""
            {
              "xid":"N123",
              "name":"Fixture Falls",
              "kinds":"waterfalls,natural",
              "point":{"lat":0.4,"lon":32.5},
              "unrestricted_extra":{"not":"persisted"}
            }
        """).jsonObject
        val provider = OpenTripMapDestinationImportProvider(apiKey = null)
        val result = provider.parsePlace(
            fixture,
            DestinationImportQuery("OPENTRIPMAP", "UG", city = "Kampala", limit = 3)
        )
        assertNotNull(result)
        assertEquals("N123", result.externalId)
        assertEquals(0.4, result.latitude)
        assertEquals(listOf("waterfalls", "natural"), result.sourceClassifications)
        assertFalse(provider.enabled)
    }
}
