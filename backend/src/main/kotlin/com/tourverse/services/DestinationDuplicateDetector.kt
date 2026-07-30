package com.tourverse.services

import com.tourverse.models.Destination
import com.tourverse.models.DestinationCandidate
import com.tourverse.models.DuplicateAssessment
import com.tourverse.models.DuplicateReason
import java.text.Normalizer
import kotlin.math.*

object DestinationDuplicateDetector {
    // Coordinates the assess business workflow for callers.
    fun assess(candidate: DestinationCandidate, destination: Destination): DuplicateAssessment {
        val reasons = mutableListOf<DuplicateReason>()
        val sameCountry = candidate.countryCode != null && candidate.countryCode == destination.countryCode
        val sameName = normalize(candidate.name) == normalize(destination.name)
        val distance = distanceKm(
            candidate.latitude, candidate.longitude,
            destination.latitude, destination.longitude
        )
        if (sameName && sameCountry) reasons += DuplicateReason(
            "NAME_AND_COUNTRY", "Normalized names and country codes match."
        )
        if (distance != null && distance <= 1.0) reasons += DuplicateReason(
            "COORDINATE_PROXIMITY", "Coordinates are within one kilometre."
        )
        val outcome = when {
            reasons.any { it.code == "COORDINATE_PROXIMITY" } && sameCountry -> "LIKELY_DUPLICATE"
            sameName && sameCountry -> "POSSIBLE_DUPLICATE"
            else -> "NO_DUPLICATE_FOUND"
        }
        return DuplicateAssessment(outcome, reasons)
    }

    // Converts the supplied values into the normalize form required by the domain model.
    internal fun normalize(value: String): String = Normalizer
        .normalize(value, Normalizer.Form.NFD)
        .replace(Regex("\\p{M}+"), "")
        .lowercase()
        .replace(Regex("[^\\p{L}\\p{N}]+"), " ")
        .trim()
        .replace(Regex("\\s+"), " ")

    // Coordinates the distance km business workflow for callers.
    private fun distanceKm(lat1: Double?, lon1: Double?, lat2: Double?, lon2: Double?): Double? {
        if (lat1 == null || lon1 == null || lat2 == null || lon2 == null) return null
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2).pow(2) +
            cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLon / 2).pow(2)
        return 6371.0 * 2 * atan2(sqrt(a), sqrt(1 - a))
    }
}
