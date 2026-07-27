package com.tourverse

import com.tourverse.plugins.configureHTTP
import com.tourverse.plugins.configureRouting
import com.tourverse.plugins.configureSerialization
import com.tourverse.plugins.configureStatusPages
import io.ktor.server.application.Application

fun Application.module() {
    configureHTTP()
    configureSerialization()
    configureStatusPages()
    configureRouting()
}
