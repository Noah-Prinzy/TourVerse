package com.tourverse.plugins

import com.tourverse.dto.ApiMessage
import com.tourverse.utils.AppEnvironment
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationCallPipeline
import io.ktor.server.application.call
import io.ktor.server.request.path
import io.ktor.server.response.respond
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

private data class RateWindow(val startsAt: Long, val count: AtomicInteger)

fun Application.configureSecurity() {
    validateProductionConfiguration()

    val requestsPerMinute = AppEnvironment.getInt("TOURVERSE_RATE_LIMIT_PER_MINUTE", 120)
        .coerceIn(10, 10_000)
    val windows = ConcurrentHashMap<String, RateWindow>()

    intercept(ApplicationCallPipeline.Plugins) {
        val now = Instant.now().epochSecond
        val minuteStart = now - (now % 60)
        val clientKey = call.request.local.remoteHost
        val current = windows.compute(clientKey) { _, old ->
            if (old == null || old.startsAt != minuteStart) RateWindow(minuteStart, AtomicInteger(1))
            else old.apply { count.incrementAndGet() }
        }!!

        if (current.count.get() > requestsPerMinute && !call.request.path().startsWith("/api/health")) {
            call.respond(
                HttpStatusCode.TooManyRequests,
                ApiMessage("error", "Too many requests. Please try again shortly.")
            )
            finish()
        }

        if (windows.size > 10_000) {
            windows.entries.removeIf { it.value.startsAt < minuteStart }
        }
    }
}

private fun validateProductionConfiguration() {
    if (!AppEnvironment.isProduction) return

    val secret = AppEnvironment.require("TOURVERSE_JWT_SECRET")
    require(secret.length >= 48) {
        "Production TOURVERSE_JWT_SECRET must contain at least 48 characters."
    }
    require(!secret.contains("replace", ignoreCase = true) && !secret.contains("development", ignoreCase = true)) {
        "Production TOURVERSE_JWT_SECRET must not use an example or development value."
    }

    val origins = AppEnvironment.require("TOURVERSE_ALLOWED_ORIGINS")
    require(origins.isNotBlank() && origins != "*") {
        "Production TOURVERSE_ALLOWED_ORIGINS must list explicit origins."
    }
    require(origins.split(',').map(String::trim).all { it.startsWith("https://") }) {
        "Production TOURVERSE_ALLOWED_ORIGINS must contain HTTPS origins only."
    }
    require(!AppEnvironment.getBoolean("TOURVERSE_INCLUDE_DEVELOPMENT_SEED_DATA", false)) {
        "Production must not expose development seed destination data."
    }

    val databaseUrl = AppEnvironment.get("TOURVERSE_DATABASE_URL")
    val marketplaceDatabaseUrl = AppEnvironment.get("DATABASE_URL")
    require(
        databaseUrl?.startsWith("jdbc:postgresql://") == true ||
            marketplaceDatabaseUrl?.let {
                it.startsWith("postgres://") || it.startsWith("postgresql://")
            } == true
    ) {
        "Production must use PostgreSQL through TOURVERSE_DATABASE_URL or DATABASE_URL."
    }
}
