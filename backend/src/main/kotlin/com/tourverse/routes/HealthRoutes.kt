package com.tourverse.routes

import com.tourverse.dto.ApiMessage
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get

fun Route.healthRoutes() {
    get("/api/health") {
        call.respond(ApiMessage("ok", "Tourism API is running"))
    }
}
