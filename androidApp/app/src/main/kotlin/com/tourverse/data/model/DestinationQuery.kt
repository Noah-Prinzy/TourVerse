package com.tourverse.data.model

data class DestinationQuery(
    val search: String = "",
    val country: String = "",
    val city: String = "",
    val category: String = "",
    val page: Int = 1,
    val size: Int = 20,
    val sortBy: DestinationSortField = DestinationSortField.CREATED_AT,
    val sortDirection: SortDirection = SortDirection.DESC
) {
    init {
        require(page >= 1) { "Page must be at least 1." }
        require(size in 1..100) { "Size must be between 1 and 100." }
    }
}

enum class DestinationSortField(val apiValue: String, val label: String) {
    NAME("name", "Name"),
    COUNTRY("country", "Country"),
    CITY("city", "City"),
    CATEGORY("category", "Category"),
    CREATED_AT("createdAt", "Newest"),
    UPDATED_AT("updatedAt", "Recently updated");

    fun next(): DestinationSortField {
        val values = entries
        return values[(ordinal + 1) % values.size]
    }
}

enum class SortDirection(val apiValue: String, val label: String) {
    ASC("asc", "Ascending"),
    DESC("desc", "Descending");

    fun toggled(): SortDirection = if (this == ASC) DESC else ASC
}
