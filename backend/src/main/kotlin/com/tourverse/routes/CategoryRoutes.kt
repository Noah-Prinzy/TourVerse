package com.tourverse.routes

import com.tourverse.dto.ApiMessage
import com.tourverse.models.CreateCategoryRequest
import com.tourverse.models.UpdateCategoryRequest
import com.tourverse.security.authenticatedUser
import com.tourverse.services.CategoryService
import com.tourverse.utils.ValidationException
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.*
import java.util.UUID

// Registers the route endpoints and delegates each request to its service layer.
fun Route.categoryRoutes(service: CategoryService) {
    route("/api/categories") {
        get { call.respond(service.getAll(false)) }
        get("/{id}") { call.respond(service.getById(call.categoryId())) }
        post {
            call.authenticatedUser("ADMIN")
            call.respond(HttpStatusCode.Created, service.create(call.receive<CreateCategoryRequest>()))
        }
        put("/{id}") {
            call.authenticatedUser("ADMIN")
            call.respond(service.update(call.categoryId(), call.receive<UpdateCategoryRequest>()))
        }
        delete("/{id}") {
            call.authenticatedUser("ADMIN")
            service.delete(call.categoryId())
            call.respond(ApiMessage("success", "Category deleted successfully."))
        }
    }
    get("/api/admin/categories") {
        call.authenticatedUser("ADMIN")
        call.respond(service.getAll(true))
    }
}

// Encapsulates the io operation behind a reusable function.
private fun io.ktor.server.application.ApplicationCall.categoryId(): UUID =
    parameters["id"]?.let { runCatching { UUID.fromString(it) }.getOrNull() }
        ?: throw ValidationException("Category ID must be a valid UUID")
