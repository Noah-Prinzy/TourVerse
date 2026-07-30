package com.tourverse.services

import com.tourverse.utils.ValidationException
import java.util.Locale

object CountryCodeService {
    private val countriesByCode = Locale.getISOCountries().associateWith { code ->
        Locale.Builder().setRegion(code).build().getDisplayCountry(Locale.ENGLISH)
    }
    private val aliases = mapOf(
        "usa" to "US",
        "u.s.a." to "US",
        "united states" to "US",
        "united states of america" to "US",
        "uk" to "GB",
        "u.k." to "GB",
        "united kingdom" to "GB"
    )
    private val codesByName = countriesByCode.entries.associate { (code, name) ->
        normalizeName(name) to code
    } + aliases

    // Converts the supplied values into the normalize code form required by the domain model.
    fun normalizeCode(value: String?): String? {
        val code = value?.trim()?.uppercase()?.takeIf(String::isNotEmpty) ?: return null
        if (!Regex("^[A-Z]{2}$").matches(code) || code !in countriesByCode) {
            throw ValidationException("Country code must be a recognized ISO 3166-1 alpha-2 code.")
        }
        return code
    }

    // Coordinates the code for name business workflow for callers.
    fun codeForName(value: String): String? = codesByName[normalizeName(value)]

    // Coordinates the display name business workflow for callers.
    fun displayName(code: String): String = countriesByCode[code]
        ?: throw ValidationException("Country code must be a recognized ISO 3166-1 alpha-2 code.")

    // Retrieves resolve from the relevant repository or external provider.
    fun resolve(country: String, countryCode: String?): Pair<String, String?> {
        val explicitCode = normalizeCode(countryCode)
        val inferredCode = codeForName(country)
        if (explicitCode != null && inferredCode != null && explicitCode != inferredCode) {
            throw ValidationException("Country name and country code do not match.")
        }
        val normalizedCode = explicitCode ?: inferredCode
        return (normalizedCode?.let(::displayName) ?: country.trim()) to normalizedCode
    }

    // Converts the supplied values into the normalize name form required by the domain model.
    private fun normalizeName(value: String) =
        value.trim().lowercase().replace(Regex("\\s+"), " ")
}
