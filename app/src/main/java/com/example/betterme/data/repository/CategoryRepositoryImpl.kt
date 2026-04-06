package com.example.betterme.data.repository

import com.example.betterme.data.local.room.dao.CategoryDao
import com.example.betterme.data.local.room.entities.CategoryEntity
import com.example.betterme.domain.repository.CategoryRepository

class CategoryRepositoryImpl(
    private val dao: CategoryDao
) : CategoryRepository {

    override fun getAll() = dao.getAll()

    override suspend fun insert(category: CategoryEntity) =
        dao.insert(category)

    override suspend fun insertAll(list: List<CategoryEntity>) =
        dao.insertAll(list)

    override suspend fun update(category: CategoryEntity) =
        dao.update(category)

    override suspend fun delete(category: CategoryEntity) =
        dao.delete(category)

    override suspend fun getById(id: Int) =
        dao.getById(id)

    override suspend fun saveSelections(selectedIds: Set<Int>) =
        dao.updateSelections(selectedIds.toList())

    override suspend fun clearAllSelections() =
        dao.clearAllSelections()
}