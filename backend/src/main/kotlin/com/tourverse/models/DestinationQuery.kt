package com.tourverse.models

data class DestinationQuery(
    val search: String? = null,
    val country: String? = null,
    val city: String? = null,
    val category: String? = null,
    val page: Int = 1,
    val size: Int = 20,
    val sortBy: DestinationSortField = DestinationSortField.CREATED_AT,
    val sortDirection: SortDirection = SortDirection.DESC
) {
    val offset: Long
        get() = ((page - 1) * size).toLong()
}

enum class DestinationSortField {
    NAME,
    COUNTRY,
    CITY,
    CATEGORY,
    CREATED_AT,
    UPDATED_AT
}

enum class SortDirection {
    ASC,
    DESC
}
