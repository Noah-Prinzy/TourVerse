package com.tourverse.database.tables

import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.java.javaUUID
import org.jetbrains.exposed.v1.javatime.timestampWithTimeZone

object DestinationImportBatchesTable : Table("destination_import_batches") {
    val id = javaUUID("id")
    val provider = varchar("provider", 40)
    val requestedBy = javaUUID("requested_by").references(UsersTable.id)
    val countryCode = varchar("country_code", 2).nullable()
    val city = varchar("city", 100).nullable()
    val queryText = varchar("query_text", 200).nullable()
    val requestedLimit = integer("requested_limit")
    val status = varchar("status", 30)
    val retrievedCount = integer("retrieved_count")
    val errorMessage = varchar("error_message", 500).nullable()
    val createdAt = timestampWithTimeZone("created_at")
    val updatedAt = timestampWithTimeZone("updated_at")
    override val primaryKey = PrimaryKey(id)
}

object DestinationImportCandidatesTable : Table("destination_import_candidates") {
    val id = javaUUID("id")
    val batchId = javaUUID("batch_id").references(DestinationImportBatchesTable.id)
    val sourceProvider = varchar("source_provider", 40)
    val externalId = varchar("external_id", 200)
    val sourceUrl = text("source_url").nullable()
    val name = varchar("name", 150)
    val countryCode = varchar("country_code", 2).nullable()
    val country = varchar("country", 100)
    val region = varchar("region", 100).nullable()
    val city = varchar("city", 100).nullable()
    val latitude = decimal("latitude", 9, 6).nullable()
    val longitude = decimal("longitude", 9, 6).nullable()
    val categoryHint = varchar("category_hint", 80).nullable()
    val mappedCategory = varchar("mapped_category", 80).nullable()
    val descriptionHint = text("description_hint").nullable()
    val officialWebsite = text("official_website").nullable()
    val imageReference = text("image_reference").nullable()
    val imageLicence = varchar("image_licence", 150).nullable()
    val imageAttribution = varchar("image_attribution", 500).nullable()
    val imageLicenceUrl = text("image_licence_url").nullable()
    val sourceClassifications = text("source_classifications")
    val retrievedAt = timestampWithTimeZone("retrieved_at")
    val reviewStatus = varchar("review_status", 30)
    val rejectionReason = varchar("rejection_reason", 500).nullable()
    val duplicateOfDestinationId = javaUUID("duplicate_of_destination_id").references(DestinationsTable.id).nullable()
    val approvedDestinationId = javaUUID("approved_destination_id").references(DestinationsTable.id).nullable()
    val reviewedBy = javaUUID("reviewed_by").references(UsersTable.id).nullable()
    val createdAt = timestampWithTimeZone("created_at")
    val updatedAt = timestampWithTimeZone("updated_at")
    override val primaryKey = PrimaryKey(id)
}

object DestinationSourceReferencesTable : Table("destination_source_references") {
    val id = javaUUID("id")
    val destinationId = javaUUID("destination_id").references(DestinationsTable.id)
    val sourceProvider = varchar("source_provider", 40)
    val externalId = varchar("external_id", 200)
    val sourceUrl = text("source_url").nullable()
    val retrievedAt = timestampWithTimeZone("retrieved_at")
    val lastVerifiedAt = timestampWithTimeZone("last_verified_at").nullable()
    val providerContentUpdatedAt = timestampWithTimeZone("provider_content_updated_at").nullable()
    val attribution = varchar("attribution", 500).nullable()
    val licence = varchar("licence", 150).nullable()
    val providerPlaceId = varchar("provider_place_id", 255).nullable()
    val metadataHash = varchar("metadata_hash", 64).nullable()
    val active = bool("active")
    val createdAt = timestampWithTimeZone("created_at")
    override val primaryKey = PrimaryKey(id)
}

object DestinationFieldProvenanceTable : Table("destination_field_provenance") {
    val id = javaUUID("id")
    val destinationId = javaUUID("destination_id").references(DestinationsTable.id)
    val fieldName = varchar("field_name", 50)
    val sourceProvider = varchar("source_provider", 40)
    val providerReferenceId = javaUUID("provider_reference_id")
        .references(DestinationSourceReferencesTable.id).nullable()
    val retrievedAt = timestampWithTimeZone("retrieved_at")
    val lastVerifiedAt = timestampWithTimeZone("last_verified_at").nullable()
    val confidence = decimal("confidence", 5, 4).nullable()
    val editoriallyLocked = bool("editorially_locked")
    val createdAt = timestampWithTimeZone("created_at")
    val updatedAt = timestampWithTimeZone("updated_at")
    override val primaryKey = PrimaryKey(id)
}
