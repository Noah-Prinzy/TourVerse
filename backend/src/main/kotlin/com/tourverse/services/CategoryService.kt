package com.tourverse.services

import com.tourverse.exceptions.NotFoundException
import com.tourverse.models.*
import com.tourverse.repositories.CategoryRepository
import java.util.UUID

class CategoryService(private val repository: CategoryRepository) {
    suspend fun getAll(includeInactive: Boolean = false) = repository.getAll(includeInactive)
    suspend fun getById(id: UUID) = repository.getById(id) ?: throw NotFoundException("Category not found")
    suspend fun create(request: CreateCategoryRequest): CategoryResponse { CategoryValidator.validate(request); return repository.create(request) }
    suspend fun update(id: UUID, request: UpdateCategoryRequest): CategoryResponse { CategoryValidator.validate(request); return repository.update(id, request) ?: throw NotFoundException("Category not found") }
    suspend fun delete(id: UUID) { if (!repository.delete(id)) throw NotFoundException("Category not found") }
}
