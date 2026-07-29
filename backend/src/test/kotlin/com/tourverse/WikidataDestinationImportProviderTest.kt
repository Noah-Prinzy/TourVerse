package com.tourverse

import com.tourverse.services.WikidataDestinationImportProvider
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlin.test.*

class WikidataDestinationImportProviderTest {
    @Test fun `binding parser handles stable id coordinates and missing optional fields`() {
        val binding = Json.parseToJsonElement("""
            {
              "place":{"value":"http://www.wikidata.org/entity/Q123"},
              "placeLabel":{"value":"Example Place"},
              "coordinate":{"value":"Point(32.5 0.25)"}
            }
        """).jsonObject
        val candidate = WikidataDestinationImportProvider(throttleMillis = 0).parseBinding(binding, "UG")
        assertNotNull(candidate)
        assertEquals("Q123", candidate.externalId)
        assertEquals(0.25, candidate.latitude)
        assertEquals(32.5, candidate.longitude)
        assertNull(candidate.city)
    }

    @Test fun `binding parser rejects malformed source identity`() {
        val binding = Json.parseToJsonElement("""{"place":{"value":"bad"},"placeLabel":{"value":"Place"}}""").jsonObject
        assertNull(WikidataDestinationImportProvider(throttleMillis = 0).parseBinding(binding, "UG"))
    }

    @Test fun `discovery query stays bounded and avoids expensive recursive enrichment`() {
        val query = WikidataDestinationImportProvider(throttleMillis = 0)
            .buildSparql("Q1036", 500)

        assertContains(query, "wdt:P17 wd:Q1036")
        assertContains(query, "VALUES ?type")
        assertContains(query, "LIMIT 100")
        assertFalse("wdt:P279*" in query)
        assertFalse("schema:about" in query)
        assertFalse("wdt:P18" in query)
        assertFalse("wdt:P856" in query)
        assertFalse("SERVICE wikibase:label" in query)
    }
}
