package com.tourverse

import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.testApplication
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals

class DocumentationRoutesTest {
    @Test
    fun openApiSpecificationIsAvailable() = testApplication {
        application { configureApplication() }

        val response = client.get("/api/openapi.yaml")

        assertEquals(HttpStatusCode.OK, response.status)
        assertContains(response.bodyAsText(), "openapi: 3.0.3")
        assertContains(response.bodyAsText(), "/api/destinations:")
    }

    @Test
    fun documentationLandingPageIsAvailable() = testApplication {
        application { configureApplication() }

        val response = client.get("/api/docs")

        assertEquals(HttpStatusCode.OK, response.status)
        assertContains(response.bodyAsText(), "TourVerse API")
    }
}
