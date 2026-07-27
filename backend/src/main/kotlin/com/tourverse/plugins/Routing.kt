package com.tourverse.plugins

import com.tourverse.repositories.InMemoryDestinationRepository
import com.tourverse.routes.destinationRoutes
import com.tourverse.routes.healthRoutes
import com.tourverse.services.DestinationService
import io.ktor.server.application.Application
import io.ktor.server.routing.routing

fun Application.configureRouting() {
    val repository = InMemoryDestinationRepository()
    val service = DestinationService(repository)

    routing {
        healthRoutes()
        destinationRoutes(service)
    }
}
