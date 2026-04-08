package com.example.betterme.domain.repository

import com.example.betterme.data.local.room.entities.CategoryEntity
import kotlinx.coroutines.flow.Flow

interface CategoryRepository {

    fun getAll(): Flow<List<CategoryEntity>>

    suspend fun insert(category: CategoryEntity)

    suspend fun insertAll(list: List<CategoryEntity>)

    suspend fun update(category: CategoryEntity)

    suspend fun delete(category: CategoryEntity)

    suspend fun getById(id: Int): CategoryEntity?
}