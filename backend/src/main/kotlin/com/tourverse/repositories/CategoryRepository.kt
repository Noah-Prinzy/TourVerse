package com.tourverse.repositories

import com.tourverse.models.*
import java.util.UUID

interface CategoryRepository {
    // Retrieves all from persistent or request state.
    suspend fun getAll(includeInactive: Boolean = false): List<CategoryResponse>
    // Retrieves by id from persistent or request state.
    suspend fun getById(id: UUID): CategoryResponse?
    // Creates create and returns the resulting domain value.
    suspend fun create(request: CreateCategoryRequest): CategoryResponse
    // Updates update within the current transaction or request.
    suspend fun update(id: UUID, request: UpdateCategoryRequest): CategoryResponse?
    // Removes or invalidates delete for the requested resource.
    suspend fun delete(id: UUID): Boolean
}
