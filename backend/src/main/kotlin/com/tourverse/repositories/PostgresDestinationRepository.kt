package com.tourverse.repositories

import com.tourverse.database.tables.DestinationsTable
import com.tourverse.database.tables.DestinationSourceReferencesTable
import com.tourverse.dto.PagedDestinationResponse
import com.tourverse.models.CreateDestinationRequest
import com.tourverse.models.Destination
import com.tourverse.models.DestinationQuery
import com.tourverse.models.DestinationCountry
import com.tourverse.models.DestinationSortField
import com.tourverse.models.SortDirection
import com.tourverse.models.UpdateDestinationRequest
import com.tourverse.models.DataOrigin
import com.tourverse.models.CacheStatus
import com.tourverse.models.VerificationStatus
import com.tourverse.services.CountryCodeService
import com.tourverse.utils.AppEnvironment
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.neq
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.lowerCase
import org.jetbrains.exposed.v1.core.or
import org.jetbrains.exposed.v1.core.like
import org.jetbrains.exposed.v1.jdbc.Query
import org.jetbrains.exposed.v1.jdbc.andWhere
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.suspendTransaction
import org.jetbrains.exposed.v1.jdbc.update
import java.math.BigDecimal
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.UUID

class PostgresDestinationRepository : DestinationRepository {

    // Retrieves all from persistent or request state.
    override suspend fun getAll(query: DestinationQuery): PagedDestinationResponse =
        suspendTransaction {
            val filteredQuery = buildFilteredQuery(query)
            val totalItems = filteredQuery.count()
            val sortColumn = when (query.sortBy) {
                DestinationSortField.NAME -> DestinationsTable.name
                DestinationSortField.COUNTRY -> DestinationsTable.country
                DestinationSortField.CITY -> DestinationsTable.city
                DestinationSortField.CATEGORY -> DestinationsTable.category
                DestinationSortField.CREATED_AT -> DestinationsTable.createdAt
                DestinationSortField.UPDATED_AT -> DestinationsTable.updatedAt
            }
            val sortOrder = if (query.sortDirection == SortDirection.ASC) {
                SortOrder.ASC
            } else {
                SortOrder.DESC
            }

            val items = filteredQuery
                .orderBy(sortColumn to sortOrder, DestinationsTable.id to SortOrder.ASC)
                .limit(query.size)
                .offset(query.offset)
                .map(::toDestination)

            PagedDestinationResponse(
                items = items,
                page = query.page,
                size = query.size,
                totalItems = totalItems,
                totalPages = if (totalItems == 0L) 0 else ((totalItems + query.size - 1) / query.size).toInt()
            )
        }

    // Retrieves countries from persistent or request state.
    override suspend fun getCountries(): List<DestinationCountry> = suspendTransaction {
        val query = DestinationsTable.selectAll()
        query.andWhere {
            DestinationsTable.verificationStatus neq VerificationStatus.REJECTED.name
        }
        if (!AppEnvironment.getBoolean(
                "TOURVERSE_INCLUDE_DEVELOPMENT_SEED_DATA",
                default = !AppEnvironment.isProduction
            )
        ) {
            query.andWhere {
                DestinationsTable.dataOrigin neq DataOrigin.DEVELOPMENT_SEED.name
            }
        }
        query
            .mapNotNull { row ->
                row[DestinationsTable.countryCode]?.let { code ->
                    code to row[DestinationsTable.country]
                }
            }
            .groupBy({ it.first }, { it.second })
            .map { (code, names) ->
                DestinationCountry(
                    code = code,
                    name = names.groupingBy(String::trim).eachCount()
                        .maxByOrNull { it.value }!!.key,
                    destinationCount = names.size.toLong()
                )
            }
            .sortedBy { it.name.lowercase() }
    }

    // Retrieves by id from persistent or request state.
    override suspend fun getById(id: UUID): Destination? =
        suspendTransaction {
            DestinationsTable
                .selectAll()
                .where { DestinationsTable.id eq id }
                .singleOrNull()
                ?.let(::toDestination)
        }

    // Creates create and returns the resulting domain value.
    override suspend fun create(request: CreateDestinationRequest): Destination =
        suspendTransaction {
            val destinationId = UUID.randomUUID()
            val currentTime = OffsetDateTime.now(ZoneOffset.UTC)
            val normalizedCountry = CountryCodeService.resolve(request.country, request.countryCode)

            DestinationsTable.insert { statement ->
                statement[id] = destinationId
                statement[name] = request.name.trim()
                statement[country] = normalizedCountry.first
                statement[countryCode] = normalizedCountry.second
                statement[city] = request.city?.trim()?.takeIf(String::isNotEmpty)
                statement[description] = request.description.trim()
                statement[category] = request.category.trim()
                statement[latitude] = request.latitude?.let(BigDecimal::valueOf)
                statement[longitude] = request.longitude?.let(BigDecimal::valueOf)
                statement[coverImageUrl] = request.coverImageUrl?.trim()?.takeIf(String::isNotEmpty)
                statement[dataOrigin] = DataOrigin.TOURVERSE_CURATED.name
                statement[cacheStatus] = CacheStatus.NOT_APPLICABLE.name
                statement[verificationStatus] = VerificationStatus.VERIFIED.name
                statement[editoriallyLocked] = true
                statement[createdAt] = currentTime
                statement[updatedAt] = currentTime
            }

            DestinationsTable
                .selectAll()
                .where { DestinationsTable.id eq destinationId }
                .single()
                .let(::toDestination)
        }

    // Updates update within the current transaction or request.
    override suspend fun update(id: UUID, request: UpdateDestinationRequest): Destination? =
        suspendTransaction {
            val normalizedCountry = CountryCodeService.resolve(request.country, request.countryCode)
            val updatedRows = DestinationsTable.update(
                where = { DestinationsTable.id eq id }
            ) { statement ->
                statement[name] = request.name.trim()
                statement[country] = normalizedCountry.first
                statement[countryCode] = normalizedCountry.second
                statement[city] = request.city?.trim()?.takeIf(String::isNotEmpty)
                statement[description] = request.description.trim()
                statement[category] = request.category.trim()
                statement[latitude] = request.latitude?.let(BigDecimal::valueOf)
                statement[longitude] = request.longitude?.let(BigDecimal::valueOf)
                statement[coverImageUrl] = request.coverImageUrl?.trim()?.takeIf(String::isNotEmpty)
                statement[dataOrigin] = DataOrigin.TOURVERSE_CURATED.name
                statement[cacheStatus] = CacheStatus.NOT_APPLICABLE.name
                statement[verificationStatus] = VerificationStatus.VERIFIED.name
                statement[editoriallyLocked] = true
                statement[updatedAt] = OffsetDateTime.now(ZoneOffset.UTC)
            }

            if (updatedRows == 0) {
                null
            } else {
                DestinationsTable
                    .selectAll()
                    .where { DestinationsTable.id eq id }
                    .single()
                    .let(::toDestination)
            }
        }

    // Removes or invalidates delete for the requested resource.
    override suspend fun delete(id: UUID): Boolean =
        suspendTransaction {
            DestinationsTable.deleteWhere { DestinationsTable.id eq id } > 0
        }

    // Converts input into the build filtered query representation used by the next application layer.
    private fun buildFilteredQuery(query: DestinationQuery): Query {
        val databaseQuery = DestinationsTable.selectAll()
        databaseQuery.andWhere {
            DestinationsTable.verificationStatus neq VerificationStatus.REJECTED.name
        }
        if (!AppEnvironment.getBoolean(
                "TOURVERSE_INCLUDE_DEVELOPMENT_SEED_DATA",
                default = !AppEnvironment.isProduction
            )
        ) {
            databaseQuery.andWhere {
                DestinationsTable.dataOrigin neq DataOrigin.DEVELOPMENT_SEED.name
            }
        }

        query.country?.trim()?.takeIf(String::isNotEmpty)?.let { country ->
            databaseQuery.andWhere { DestinationsTable.country.lowerCase() eq country.lowercase() }
        }
        query.countryCode?.trim()?.takeIf(String::isNotEmpty)?.let { countryCode ->
            databaseQuery.andWhere { DestinationsTable.countryCode eq countryCode.uppercase() }
        }
        query.city?.trim()?.takeIf(String::isNotEmpty)?.let { city ->
            databaseQuery.andWhere { DestinationsTable.city.lowerCase() eq city.lowercase() }
        }
        query.category?.trim()?.takeIf(String::isNotEmpty)?.let { category ->
            databaseQuery.andWhere { DestinationsTable.category.lowerCase() eq category.lowercase() }
        }
        query.search?.trim()?.takeIf(String::isNotEmpty)?.let { search ->
            val pattern = "%${search.lowercase()}%"
            databaseQuery.andWhere {
                (DestinationsTable.name.lowerCase() like pattern) or
                    (DestinationsTable.country.lowerCase() like pattern) or
                    (DestinationsTable.city.lowerCase() like pattern) or
                    (DestinationsTable.description.lowerCase() like pattern) or
                    (DestinationsTable.category.lowerCase() like pattern)
            }
        }

        return databaseQuery
    }

    // Converts input into the to destination representation used by the next application layer.
    private fun toDestination(row: ResultRow): Destination {
        val references = DestinationSourceReferencesTable.selectAll().where {
            (DestinationSourceReferencesTable.destinationId eq row[DestinationsTable.id]) and
                (DestinationSourceReferencesTable.active eq true)
        }.toList()
        val attribution = references.mapNotNull { it[DestinationSourceReferencesTable.attribution] }
            .distinct().takeIf { it.isNotEmpty() }?.joinToString("; ")
        val googlePlaceId = references.firstOrNull {
            it[DestinationSourceReferencesTable.sourceProvider] == "GOOGLE_PLACES"
        }?.get(DestinationSourceReferencesTable.providerPlaceId)
        return Destination(
            id = row[DestinationsTable.id],
            name = row[DestinationsTable.name],
            country = row[DestinationsTable.country],
            city = row[DestinationsTable.city],
            description = row[DestinationsTable.description],
            category = row[DestinationsTable.category],
            latitude = row[DestinationsTable.latitude]?.toDouble(),
            longitude = row[DestinationsTable.longitude]?.toDouble(),
            coverImageUrl = row[DestinationsTable.coverImageUrl],
            countryCode = row[DestinationsTable.countryCode],
            createdAt = row[DestinationsTable.createdAt].toInstant(),
            updatedAt = row[DestinationsTable.updatedAt].toInstant(),
            dataOrigin = DataOrigin.valueOf(row[DestinationsTable.dataOrigin]),
            lastVerifiedAt = row[DestinationsTable.lastVerifiedAt]?.toInstant(),
            verificationStatus = VerificationStatus.valueOf(row[DestinationsTable.verificationStatus]),
            attributionSummary = attribution,
            googlePlaceId = googlePlaceId
        )
    }
}
