package com.example.betterme.data.local.room.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.betterme.data.local.room.entities.CategoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(categories: List<CategoryEntity>)

    @Insert
    suspend fun insert(category: CategoryEntity)

    @Update
    suspend fun update(category: CategoryEntity)

    @Delete
    suspend fun delete(category: CategoryEntity)

    @Query("SELECT * FROM categories")
    fun getAll(): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM categories WHERE id = :id")
    suspend fun getById(id: Int): CategoryEntity?

    @Query("UPDATE categories SET isSelected = CASE WHEN id IN (:selectedIds) THEN 1 ELSE 0 END")
    suspend fun updateSelections(selectedIds: List<Int>)

    @Query("UPDATE categories SET isSelected = 0")
    suspend fun clearAllSelections()

    @Query("SELECT * FROM categories WHERE isSelected = 1")
    fun getSelectedCategories(): Flow<List<CategoryEntity>>
}