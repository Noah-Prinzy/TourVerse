package com.tourverse.routes

import com.tourverse.models.*
import com.tourverse.security.authenticatedUser
import com.tourverse.services.DestinationImportService
import com.tourverse.utils.ValidationException
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.*
import java.util.UUID

// Registers the route endpoints and delegates each request to its service layer.
fun Route.destinationImportRoutes(service: DestinationImportService) {
    route("/api/admin/destination-imports") {
        post("/search") {
            val admin = call.authenticatedUser("ADMIN")
            call.respond(HttpStatusCode.Created, service.search(admin.id, call.receive<DestinationImportQuery>()))
        }
        get { call.authenticatedUser("ADMIN"); call.respond(service.listBatches()) }
        get("/{batchId}") {
            call.authenticatedUser("ADMIN")
            call.respond(service.getBatch(importUuid(call.parameters["batchId"], "Batch")))
        }
        post("/{batchId}/retry") {
            val admin = call.authenticatedUser("ADMIN")
            call.respond(HttpStatusCode.Created, service.retry(
                admin.id, importUuid(call.parameters["batchId"], "Batch")
            ))
        }
        get("/candidates") {
            call.authenticatedUser("ADMIN")
            val batchId = call.request.queryParameters["batchId"]?.let { importUuid(it, "Batch") }
            val status = call.request.queryParameters["status"]?.trim()?.uppercase()?.let {
                runCatching { DestinationImportStatus.valueOf(it) }.getOrNull()
                    ?: throw ValidationException("Invalid candidate status.")
            }
            call.respond(service.listCandidates(batchId, status))
        }
        route("/candidates/{candidateId}") {
            get {
                call.authenticatedUser("ADMIN")
                call.respond(service.getCandidate(importUuid(call.parameters["candidateId"], "Candidate")))
            }
            put {
                call.authenticatedUser("ADMIN")
                call.respond(service.updateCandidate(
                    importUuid(call.parameters["candidateId"], "Candidate"),
                    call.receive<UpdateDestinationCandidateRequest>()
                ))
            }
            post("/approve") {
                val admin = call.authenticatedUser("ADMIN")
                call.respond(service.approveCandidate(importUuid(call.parameters["candidateId"], "Candidate"), admin.id))
            }
            post("/reject") {
                val admin = call.authenticatedUser("ADMIN")
                call.respond(service.rejectCandidate(
                    importUuid(call.parameters["candidateId"], "Candidate"), admin.id,
                    call.receive<RejectDestinationCandidateRequest>()
                ))
            }
            post("/link") {
                val admin = call.authenticatedUser("ADMIN")
                call.respond(service.linkCandidate(
                    importUuid(call.parameters["candidateId"], "Candidate"), admin.id,
                    call.receive<LinkDestinationCandidateRequest>()
                ))
            }
        }
    }
}

// Encapsulates the import uuid operation behind a reusable function.
private fun importUuid(value: String?, label: String): UUID =
    value?.let { runCatching { UUID.fromString(it) }.getOrNull() }
        ?: throw ValidationException("$label ID must be a valid UUID.")
