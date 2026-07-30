package com.tourverse.plugins

import com.tourverse.utils.AppEnvironment
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.cors.routing.CORS

// Configures cross-origin access so the web client can call the backend safely.
fun Application.configureHTTP() {
    install(CORS) {
        val configuredOrigins = AppEnvironment.get("TOURVERSE_ALLOWED_ORIGINS")
            ?.split(',')
            ?.map(String::trim)
            ?.filter(String::isNotEmpty)
            .orEmpty()

        if (configuredOrigins.isEmpty() && !AppEnvironment.isProduction) {
            anyHost()
        } else {
            configuredOrigins.forEach { origin ->
                val normalized = origin.removePrefix("https://").removePrefix("http://")
                val scheme = when {
                    origin.startsWith("https://") -> "https"
                    origin.startsWith("http://") -> "http"
                    else -> "https"
                }
                allowHost(normalized, schemes = listOf(scheme))
            }
        }

        allowHeader(HttpHeaders.ContentType)
        allowHeader(HttpHeaders.Authorization)
        allowHeader(HttpHeaders.Accept)
        allowMethod(HttpMethod.Get)
        allowMethod(HttpMethod.Post)
        allowMethod(HttpMethod.Put)
        allowMethod(HttpMethod.Patch)
        allowMethod(HttpMethod.Delete)
        allowMethod(HttpMethod.Options)
        allowNonSimpleContentTypes = true
    }
}
