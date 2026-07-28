package com.tourverse.routes

import com.tourverse.dto.ApiMessage
import com.tourverse.models.DeleteAccountRequest
import com.tourverse.models.UpdateProfileImageRequest
import com.tourverse.models.UpdateUserProfileRequest
import com.tourverse.security.authenticatedUser
import com.tourverse.services.UserProfileService
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.*

fun Route.userProfileRoutes(service: UserProfileService) {
    route("/api/users/me/profile") {
        get { call.respond(service.get(call.authenticatedUser().id)) }
        put { call.respond(service.update(call.authenticatedUser().id, call.receive<UpdateUserProfileRequest>())) }
        put("/image") { call.respond(service.updateImage(call.authenticatedUser().id, call.receive<UpdateProfileImageRequest>())) }
    }
    delete("/api/users/me") {
        service.deleteAccount(call.authenticatedUser().id, call.receive<DeleteAccountRequest>())
        call.respond(ApiMessage("success", "Account deleted successfully."))
    }
}
