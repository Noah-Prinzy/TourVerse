package com.tourverse.models

// Carries destination query values between application layers.
data class DestinationQuery(
    val search: String? = null,
    val country: String? = null,
    val countryCode: String? = null,
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

// Lists the supported destination sort field values used by validation and persistence.
enum class DestinationSortField {
    NAME,
    COUNTRY,
    CITY,
    CATEGORY,
    CREATED_AT,
    UPDATED_AT
}

// Lists the supported sort direction values used by validation and persistence.
enum class SortDirection {
    ASC,
    DESC
}
