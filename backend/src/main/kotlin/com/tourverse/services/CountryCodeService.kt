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

    fun normalizeCode(value: String?): String? {
        val code = value?.trim()?.uppercase()?.takeIf(String::isNotEmpty) ?: return null
        if (!Regex("^[A-Z]{2}$").matches(code) || code !in countriesByCode) {
            throw ValidationException("Country code must be a recognized ISO 3166-1 alpha-2 code.")
        }
        return code
    }

    fun codeForName(value: String): String? = codesByName[normalizeName(value)]

    fun displayName(code: String): String = countriesByCode[code]
        ?: throw ValidationException("Country code must be a recognized ISO 3166-1 alpha-2 code.")

    fun resolve(country: String, countryCode: String?): Pair<String, String?> {
        val explicitCode = normalizeCode(countryCode)
        val inferredCode = codeForName(country)
        if (explicitCode != null && inferredCode != null && explicitCode != inferredCode) {
            throw ValidationException("Country name and country code do not match.")
        }
        val normalizedCode = explicitCode ?: inferredCode
        return (normalizedCode?.let(::displayName) ?: country.trim()) to normalizedCode
    }

    private fun normalizeName(value: String) =
        value.trim().lowercase().replace(Regex("\\s+"), " ")
}
