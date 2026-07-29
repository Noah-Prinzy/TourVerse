package com.tourverse.routes

import com.tourverse.dto.ApiMessage
import com.tourverse.models.*
import com.tourverse.security.authenticatedUser
import com.tourverse.services.AuthService
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.*

fun Route.authRoutes(service: AuthService) {
    route("/api/auth") {
        post("/register") {
            call.respond(HttpStatusCode.Created, service.register(call.receive<RegisterRequest>()))
        }
        post("/login") {
            call.respond(service.login(call.receive<LoginRequest>()))
        }
        post("/refresh") {
            call.respond(service.refresh(call.receive<RefreshTokenRequest>()))
        }
        post("/logout") {
            service.logout(call.receive<LogoutRequest>())
            call.respond(ApiMessage("success", "Logged out successfully."))
        }
        post("/logout-all") {
            service.revokeAllSessions(call.authenticatedUser().id)
            call.respond(ApiMessage("success", "All sessions have been logged out."))
        }
    }

    route("/api/users/me") {
        get {
            call.respond(service.getCurrentUser(call.authenticatedUser().id))
        }
        put {
            call.respond(
                service.updateProfile(
                    call.authenticatedUser().id,
                    call.receive<UpdateProfileRequest>()
                )
            )
        }
        put("/password") {
            service.changePassword(
                call.authenticatedUser().id,
                call.receive<ChangePasswordRequest>()
            )
            call.respond(
                ApiMessage(
                    "success",
                    "Password changed successfully. Sign in again on your other devices."
                )
            )
        }
    }
}
