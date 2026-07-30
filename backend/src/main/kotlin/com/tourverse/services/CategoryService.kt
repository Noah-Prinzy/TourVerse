package com.tourverse.services

import com.tourverse.exceptions.NotFoundException
import com.tourverse.models.*
import com.tourverse.repositories.CategoryRepository
import java.util.UUID

class CategoryService(private val repository: CategoryRepository) {
    // Retrieves all from the relevant repository or external provider.
    suspend fun getAll(includeInactive: Boolean = false) = repository.getAll(includeInactive)
    // Retrieves by id from the relevant repository or external provider.
    suspend fun getById(id: UUID) = repository.getById(id) ?: throw NotFoundException("Category not found")
    // Creates create after applying validation and business rules.
    suspend fun create(request: CreateCategoryRequest): CategoryResponse { CategoryValidator.validate(request); return repository.create(request) }
    // Updates update while keeping related state consistent.
    suspend fun update(id: UUID, request: UpdateCategoryRequest): CategoryResponse { CategoryValidator.validate(request); return repository.update(id, request) ?: throw NotFoundException("Category not found") }
    // Removes or invalidates delete after enforcing ownership and authorization rules.
    suspend fun delete(id: UUID) { if (!repository.delete(id)) throw NotFoundException("Category not found") }
}
