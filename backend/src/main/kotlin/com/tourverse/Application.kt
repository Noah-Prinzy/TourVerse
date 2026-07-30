package com.tourverse

import com.tourverse.database.DatabaseFactory
import com.tourverse.plugins.configureHTTP
import com.tourverse.plugins.configureObservability
import com.tourverse.plugins.configureSecurity
import com.tourverse.plugins.configureRouting
import com.tourverse.plugins.configureSerialization
import com.tourverse.plugins.configureStatusPages
import io.ktor.server.application.Application

// Main Ktor entry point for the TourVerse backend.
// This initializes the database and wires in the shared application plugins.
fun Application.module() {
    DatabaseFactory.init()
    configureApplication()
}

// Applies the app-wide configuration in a single place so startup remains easy to follow.
internal fun Application.configureApplication() {
    configureObservability()
    configureSerialization()
    configureStatusPages()
    configureSecurity()
    configureHTTP()
    configureRouting()
}
