package com.example.betterme.domain.repository

import com.example.betterme.data.local.room.entities.CategoryEntity
import kotlinx.coroutines.flow.Flow

/**
 * Per-user category selection. Replaces the global `CategoryEntity.isSelected` flag so each
 * user account on the device sees only their own selections.
 *
 * All read & write methods take `userId` explicitly — there is no implicit "current user"
 * fallback at this layer.
 */
interface UserCategoryRepository {

    fun observeSelectedIds(userId: String): Flow<List<Int>>

    suspend fun getSelectedIds(userId: String): List<Int>

    fun observeSelectedCategories(userId: String): Flow<List<CategoryEntity>>

    suspend fun getSelectedCategories(userId: String): List<CategoryEntity>

    /** Replaces the user's full selection list with [categoryIds] in a single transaction. */
    suspend fun saveSelections(userId: String, categoryIds: Collection<Int>)

    suspend fun clearForUser(userId: String)
}
