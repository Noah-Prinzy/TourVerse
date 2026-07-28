package com.tourverse.repositories

import com.tourverse.database.tables.CategoriesTable
import com.tourverse.exceptions.ConflictException
import com.tourverse.models.*
import com.tourverse.services.CategoryValidator
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.*
import org.jetbrains.exposed.v1.jdbc.transactions.suspendTransaction
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.UUID

class PostgresCategoryRepository : CategoryRepository {
    override suspend fun getAll(includeInactive: Boolean): List<CategoryResponse> = suspendTransaction {
        val query = CategoriesTable.selectAll()
        (if (includeInactive) query else query.where { CategoriesTable.active eq true })
            .orderBy(CategoriesTable.name to SortOrder.ASC).map { it.toCategory() }
    }

    override suspend fun getById(id: UUID): CategoryResponse? = suspendTransaction {
        CategoriesTable.selectAll().where { CategoriesTable.id eq id }.singleOrNull()?.toCategory()
    }

    override suspend fun create(request: CreateCategoryRequest): CategoryResponse = suspendTransaction {
        val slug = CategoryValidator.slug(request.name)
        if (CategoriesTable.selectAll().where { CategoriesTable.slug eq slug }.any()) throw ConflictException("Category already exists")
        val id = UUID.randomUUID(); val now = OffsetDateTime.now(ZoneOffset.UTC)
        CategoriesTable.insert { row ->
            row[CategoriesTable.id] = id; row[name] = request.name.trim(); row[CategoriesTable.slug] = slug
            row[description] = request.description?.trim()?.takeIf(String::isNotEmpty)
            row[iconUrl] = request.iconUrl?.trim()?.takeIf(String::isNotEmpty); row[active] = request.active
            row[createdAt] = now; row[updatedAt] = now
        }
        CategoriesTable.selectAll().where { CategoriesTable.id eq id }.single().toCategory()
    }

    override suspend fun update(id: UUID, request: UpdateCategoryRequest): CategoryResponse? = suspendTransaction {
        val newSlug = request.name?.let(CategoryValidator::slug)
        if (newSlug != null && CategoriesTable.selectAll().where { CategoriesTable.slug eq newSlug }.any { it[CategoriesTable.id] != id }) {
            throw ConflictException("Category already exists")
        }
        val count = CategoriesTable.update({ CategoriesTable.id eq id }) { row ->
            request.name?.let { row[name] = it.trim(); row[slug] = newSlug!! }
            request.description?.let { row[description] = it.trim().takeIf(String::isNotEmpty) }
            request.iconUrl?.let { row[iconUrl] = it.trim().takeIf(String::isNotEmpty) }
            request.active?.let { row[active] = it }
            row[updatedAt] = OffsetDateTime.now(ZoneOffset.UTC)
        }
        if (count == 0) null else CategoriesTable.selectAll().where { CategoriesTable.id eq id }.single().toCategory()
    }

    override suspend fun delete(id: UUID): Boolean = suspendTransaction {
        CategoriesTable.deleteWhere { CategoriesTable.id eq id } > 0
    }

    private fun ResultRow.toCategory() = CategoryResponse(
        id = this[CategoriesTable.id], name = this[CategoriesTable.name], slug = this[CategoriesTable.slug],
        description = this[CategoriesTable.description], iconUrl = this[CategoriesTable.iconUrl], active = this[CategoriesTable.active],
        createdAt = this[CategoriesTable.createdAt].toInstant(), updatedAt = this[CategoriesTable.updatedAt].toInstant()
    )
}
