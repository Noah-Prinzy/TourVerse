package com.tourverse.services

object SourceCategoryMapper {
    private val mappings = mapOf(
        "national park" to "National Parks", "museum" to "Museums",
        "waterfall" to "Waterfalls", "island" to "Islands",
        "beach" to "Beaches", "wildlife" to "Wildlife"
    )

    fun map(classifications: List<String>): String? {
        val matches = classifications.flatMap { value ->
            mappings.filterKeys { value.contains(it, ignoreCase = true) }.values
        }.distinct()
        return matches.singleOrNull()
    }
}
