package com.tourverse.services

import com.tourverse.models.DestinationCandidate
import com.tourverse.models.DestinationImportQuery
import io.github.cdimascio.dotenv.Dotenv
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.time.Instant

class OpenTripMapDestinationImportProvider(
    private val apiKey: String? = Dotenv.configure().ignoreIfMissing().load()
        .get("TOURVERSE_OPENTRIPMAP_API_KEY")
) : DestinationImportProvider {
    override val providerName = "OPENTRIPMAP"
    override val enabled = !apiKey.isNullOrBlank()

    override suspend fun search(query: DestinationImportQuery): List<DestinationCandidate> {
        if (!enabled) throw DestinationImportProviderException(
            "OpenTripMap is disabled. Configure TOURVERSE_OPENTRIPMAP_API_KEY on the backend."
        )
        throw DestinationImportProviderException(
            "OpenTripMap live import is disabled pending API terms and attribution review."
        )
    }

    override suspend fun getDetails(externalId: String): DestinationCandidate? = null

    internal fun parsePlace(place: JsonObject, query: DestinationImportQuery): DestinationCandidate? {
        val externalId = place["xid"]?.jsonPrimitive?.contentOrNull?.trim()
            ?.takeIf(String::isNotEmpty) ?: return null
        val name = place["name"]?.jsonPrimitive?.contentOrNull?.trim()
            ?.takeIf(String::isNotEmpty) ?: return null
        val point = place["point"]?.jsonObject
        return DestinationCandidate(
            sourceProvider = providerName,
            externalId = externalId,
            sourceUrl = "https://opentripmap.com/en/card/$externalId",
            name = name,
            countryCode = CountryCodeService.normalizeCode(query.countryCode),
            country = CountryCodeService.displayName(query.countryCode),
            city = query.city?.trim()?.takeIf(String::isNotEmpty),
            latitude = point?.get("lat")?.jsonPrimitive?.doubleOrNull,
            longitude = point?.get("lon")?.jsonPrimitive?.doubleOrNull,
            sourceClassifications = place["kinds"]?.jsonPrimitive?.contentOrNull
                ?.split(',')?.map(String::trim)?.filter(String::isNotEmpty).orEmpty(),
            retrievedAt = Instant.now()
        )
    }
}
