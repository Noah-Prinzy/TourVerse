package com.tourverse.repositories

import com.tourverse.models.*
import java.util.UUID

interface CategoryRepository {
    suspend fun getAll(includeInactive: Boolean = false): List<CategoryResponse>
    suspend fun getById(id: UUID): CategoryResponse?
    suspend fun create(request: CreateCategoryRequest): CategoryResponse
    suspend fun update(id: UUID, request: UpdateCategoryRequest): CategoryResponse?
    suspend fun delete(id: UUID): Boolean
}
