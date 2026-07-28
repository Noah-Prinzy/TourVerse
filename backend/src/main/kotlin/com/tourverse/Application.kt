package com.tourverse

import com.tourverse.database.DatabaseFactory
import com.tourverse.plugins.configureHTTP
import com.tourverse.plugins.configureObservability
import com.tourverse.plugins.configureSecurity
import com.tourverse.plugins.configureRouting
import com.tourverse.plugins.configureSerialization
import com.tourverse.plugins.configureStatusPages
import io.ktor.server.application.Application

fun Application.module() {
    DatabaseFactory.init()
    configureApplication()
}

internal fun Application.configureApplication() {
    configureObservability()
    configureSerialization()
    configureStatusPages()
    configureSecurity()
    configureHTTP()
    configureRouting()
}
