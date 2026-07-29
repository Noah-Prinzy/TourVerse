package com.tourverse.services

import com.tourverse.models.DestinationCandidate
import com.tourverse.models.DestinationImportQuery
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.serialization.json.*
import io.ktor.serialization.kotlinx.json.json
import java.time.Instant

class WikidataDestinationImportProvider(
    private val client: HttpClient = defaultClient(),
    private val throttleMillis: Long = 250
) : DestinationImportProvider {
    override val providerName = "WIKIDATA"
    override val enabled = true

    override suspend fun search(query: DestinationImportQuery): List<DestinationCandidate> {
        val code = CountryCodeService.normalizeCode(query.countryCode)!!
        val limit = query.limit.coerceIn(1, 100)
        val search = query.search?.trim()?.takeIf(String::isNotEmpty)
        try {
            delay(throttleMillis)
            val countryEntityId = resolveCountryEntity(code)
            delay(throttleMillis)
            val discoveryLimit = if (search == null) (limit * 3).coerceAtMost(100) else 100
            val discoveryPayload = executeSparql(buildSparql(countryEntityId, discoveryLimit))
            val bindings = discoveryPayload["results"]?.jsonObject?.get("bindings")?.jsonArray
                ?: throw DestinationImportProviderException("Wikidata returned a malformed response.")
            val discovered = bindings.mapNotNull(::parseDiscoveredPlace)
                .distinctBy { it.externalId }
            if (discovered.isEmpty()) return emptyList()

            delay(throttleMillis)
            val metadata = loadMetadata(discovered.map { it.externalId })
            return discovered.mapNotNull { place ->
                val details = metadata[place.externalId] ?: return@mapNotNull null
                DestinationCandidate(
                    sourceProvider = providerName,
                    externalId = place.externalId,
                    sourceUrl = "https://www.wikidata.org/wiki/${place.externalId}",
                    name = details.name,
                    countryCode = code,
                    country = CountryCodeService.displayName(code),
                    latitude = place.latitude,
                    longitude = place.longitude,
                    descriptionHint = details.description,
                    sourceClassifications = emptyList(),
                    retrievedAt = Instant.now()
                )
            }.filter { candidate ->
                search == null ||
                    candidate.name.contains(search, ignoreCase = true) ||
                    candidate.descriptionHint?.contains(search, ignoreCase = true) == true
            }.take(limit)
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: HttpRequestTimeoutException) {
            throw DestinationImportProviderException("Wikidata request timed out.", exception)
        } catch (exception: DestinationImportProviderException) {
            throw exception
        } catch (exception: Exception) {
            throw DestinationImportProviderException("Wikidata import request failed.", exception)
        }
    }

    override suspend fun getDetails(externalId: String): DestinationCandidate? = null

    internal fun buildSparql(countryEntityId: String, limit: Int): String {
        require(ENTITY_ID.matches(countryEntityId)) { "Invalid Wikidata country entity ID." }
        return """
            SELECT DISTINCT ?place ?coordinate WHERE {
              ?place wdt:P17 wd:$countryEntityId;
                     wdt:P31 ?type;
                     wdt:P625 ?coordinate.
              VALUES ?type {
                wd:Q570116
                wd:Q33506
                wd:Q46169
                wd:Q23397
                wd:Q8502
                wd:Q34038
                wd:Q4421
                wd:Q23413
                wd:Q839954
                wd:Q44539
              }
            }
            LIMIT ${limit.coerceIn(1, 100)}
        """.trimIndent()
    }

    private suspend fun resolveCountryEntity(countryCode: String): String {
        val query = """SELECT ?country WHERE { ?country wdt:P297 "$countryCode". } LIMIT 1"""
        val binding = executeSparql(query)["results"]?.jsonObject
            ?.get("bindings")?.jsonArray?.firstOrNull()?.jsonObject
        val entityUrl = binding?.get("country")?.jsonObject
            ?.get("value")?.jsonPrimitive?.contentOrNull
            ?: throw DestinationImportProviderException(
                "Wikidata does not recognize country code $countryCode."
            )
        return entityUrl.substringAfterLast('/').takeIf(ENTITY_ID::matches)
            ?: throw DestinationImportProviderException("Wikidata returned an invalid country identity.")
    }

    private suspend fun executeSparql(query: String): JsonObject {
        val responseText = client.get(SPARQL_ENDPOINT) {
            parameter("query", query)
            parameter("format", "json")
            header(HttpHeaders.UserAgent, USER_AGENT)
            header(HttpHeaders.Accept, "application/sparql-results+json")
        }.bodyAsText()
        return Json.parseToJsonElement(responseText).jsonObject
    }

    private suspend fun loadMetadata(entityIds: List<String>): Map<String, EntityMetadata> {
        val responseText = client.get(ENTITY_ENDPOINT) {
            parameter("action", "wbgetentities")
            parameter("ids", entityIds.joinToString("|"))
            parameter("props", "labels|descriptions")
            parameter("languages", "en")
            parameter("format", "json")
            header(HttpHeaders.UserAgent, USER_AGENT)
            header(HttpHeaders.Accept, "application/json")
        }.bodyAsText()
        val entities = Json.parseToJsonElement(responseText).jsonObject["entities"]?.jsonObject
            ?: throw DestinationImportProviderException("Wikidata returned malformed entity metadata.")
        return entities.mapNotNull { (id, value) ->
            if (!ENTITY_ID.matches(id)) return@mapNotNull null
            val entity = value.jsonObject
            val name = entity["labels"]?.jsonObject?.get("en")?.jsonObject
                ?.get("value")?.jsonPrimitive?.contentOrNull?.trim()
                ?.takeIf(String::isNotEmpty)
                ?: return@mapNotNull null
            val description = entity["descriptions"]?.jsonObject?.get("en")?.jsonObject
                ?.get("value")?.jsonPrimitive?.contentOrNull?.trim()
                ?.takeIf(String::isNotEmpty)
            id to EntityMetadata(name, description)
        }.toMap()
    }

    private fun parseDiscoveredPlace(element: JsonElement): DiscoveredPlace? {
        val binding = element.jsonObject
        fun value(name: String) = binding[name]?.jsonObject?.get("value")?.jsonPrimitive?.contentOrNull
        val externalId = value("place")?.substringAfterLast('/')?.takeIf(ENTITY_ID::matches)
            ?: return null
        val point = value("coordinate")?.let(::parsePoint)
        return DiscoveredPlace(externalId, point?.first, point?.second)
    }

    internal fun parseBinding(binding: JsonObject, countryCode: String): DestinationCandidate? {
        fun value(name: String) = binding[name]?.jsonObject?.get("value")?.jsonPrimitive?.contentOrNull
        val entityUrl = value("place") ?: return null
        val externalId = entityUrl.substringAfterLast('/').takeIf { Regex("Q\\d+").matches(it) } ?: return null
        val name = value("placeLabel")?.trim()?.takeIf(String::isNotEmpty) ?: return null
        val point = value("coordinate")?.let(::parsePoint)
        return DestinationCandidate(
            sourceProvider = providerName,
            externalId = externalId,
            sourceUrl = "https://www.wikidata.org/wiki/$externalId",
            name = name,
            countryCode = countryCode,
            country = CountryCodeService.displayName(countryCode),
            latitude = point?.first,
            longitude = point?.second,
            descriptionHint = value("placeDescription"),
            officialWebsite = value("website"),
            imageReference = value("image"),
            imageLicence = null,
            imageAttribution = null,
            sourceClassifications = listOfNotNull(value("article")),
            retrievedAt = Instant.now()
        )
    }

    private fun parsePoint(value: String): Pair<Double, Double>? {
        val match = Regex("""Point\((-?\d+(?:\.\d+)?) (-?\d+(?:\.\d+)?)\)""").matchEntire(value)
            ?: return null
        val longitude = match.groupValues[1].toDoubleOrNull() ?: return null
        val latitude = match.groupValues[2].toDoubleOrNull() ?: return null
        return latitude to longitude
    }

    companion object {
        private val ENTITY_ID = Regex("Q\\d+")
        private const val SPARQL_ENDPOINT = "https://query.wikidata.org/sparql"
        private const val ENTITY_ENDPOINT = "https://www.wikidata.org/w/api.php"
        private const val USER_AGENT = "TourVerse/1.0 destination-catalogue (contact: project administrator)"
        private fun defaultClient() = HttpClient(CIO) {
            install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
            install(HttpTimeout) {
                requestTimeoutMillis = 30_000
                connectTimeoutMillis = 5_000
                socketTimeoutMillis = 30_000
            }
        }
    }

    private data class DiscoveredPlace(
        val externalId: String,
        val latitude: Double?,
        val longitude: Double?
    )

    private data class EntityMetadata(
        val name: String,
        val description: String?
    )
}
