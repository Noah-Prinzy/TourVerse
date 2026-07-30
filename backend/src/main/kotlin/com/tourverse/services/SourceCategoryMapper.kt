package com.tourverse.services

// Provides shared source category mapper behavior without requiring an instance.
object SourceCategoryMapper {
    private val mappings = mapOf(
        "national park" to "National Parks", "museum" to "Museums",
        "waterfall" to "Waterfalls", "island" to "Islands",
        "beach" to "Beaches", "wildlife" to "Wildlife"
    )

    // Transforms the supplied data into map output used by the application.
    fun map(classifications: List<String>): String? {
        val matches = classifications.flatMap { value ->
            mappings.filterKeys { value.contains(it, ignoreCase = true) }.values
        }.distinct()
        return matches.singleOrNull()
    }
}
