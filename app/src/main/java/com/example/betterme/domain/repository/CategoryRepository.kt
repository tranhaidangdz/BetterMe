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

    /** Lưu danh sách id được chọn, các id không có trong list sẽ được bỏ chọn */
    suspend fun saveSelections(selectedIds: Set<Int>)

    /** Xóa toàn bộ trạng thái chọn (dùng khi reset) */
    suspend fun clearAllSelections()
}