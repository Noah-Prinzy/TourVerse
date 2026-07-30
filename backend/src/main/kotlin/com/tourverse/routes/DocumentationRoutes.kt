package com.tourverse.routes

import io.ktor.http.ContentType
import io.ktor.server.application.call
import io.ktor.server.response.respondText
import io.ktor.server.routing.Route
import io.ktor.server.routing.get

// Registers the route endpoints and delegates each request to its service layer.
fun Route.documentationRoutes() {
    get("/api/openapi.yaml") {
        val specification = checkNotNull(
            Thread.currentThread().contextClassLoader.getResourceAsStream("openapi/tourverse-openapi.yaml")
        ) { "OpenAPI specification is missing from application resources." }
            .bufferedReader()
            .use { it.readText() }

        call.respondText(specification, ContentType.parse("application/yaml"))
    }

    get("/api/docs") {
        call.respondText(
            """
            <!doctype html>
            <html lang="en">
            <head>
              <meta charset="utf-8" />
              <meta name="viewport" content="width=device-width, initial-scale=1" />
              <title>TourVerse API Documentation</title>
              <style>
                body { font-family: system-ui, sans-serif; max-width: 820px; margin: 48px auto; padding: 0 20px; line-height: 1.6; }
                code { background: #f2f4f7; padding: 2px 6px; border-radius: 4px; }
              </style>
            </head>
            <body>
              <h1>TourVerse API</h1>
              <p>The machine-readable OpenAPI document is available at <a href="/api/openapi.yaml"><code>/api/openapi.yaml</code></a>.</p>
              <p>Import that file into Postman, Insomnia, Swagger Editor, or an OpenAPI-compatible client generator.</p>
            </body>
            </html>
            """.trimIndent(),
            ContentType.Text.Html
        )
    }
}
