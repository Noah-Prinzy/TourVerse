package com.tourverse.plugins

import com.tourverse.dto.ApiMessage
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.application.log
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.response.respond

fun Application.configureStatusPages() {
    install(StatusPages) {
        exception<Throwable> { call, cause ->
            this@configureStatusPages.log.error(
                "Unhandled server error",
                cause
            )

            call.respond(
                status = HttpStatusCode.InternalServerError,
                message = ApiMessage(
                    status = "error",
                    message = "An unexpected server error occurred."
                )
            )
        }
    }
}
