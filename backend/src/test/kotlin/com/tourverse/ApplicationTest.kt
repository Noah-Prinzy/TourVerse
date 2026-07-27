package com.tourverse

import io.ktor.client.request.get
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.testApplication
import kotlin.test.Test
import kotlin.test.assertEquals

class ApplicationTest {

    @Test
    fun healthEndpointReturnsOk() = testApplication {
        application {
            module()
        }

        val response = client.get("/api/health")
        assertEquals(HttpStatusCode.OK, response.status)
    }
}
