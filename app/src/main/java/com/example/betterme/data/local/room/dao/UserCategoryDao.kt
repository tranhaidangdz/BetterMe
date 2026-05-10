package com.example.betterme.data.local.room.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.example.betterme.data.local.room.entities.CategoryEntity
import com.example.betterme.data.local.room.entities.UserCategoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UserCategoryDao {

    @Query("SELECT category_id FROM user_categories WHERE user_id = :userId")
    fun observeSelectedIds(userId: String): Flow<List<Int>>

    @Query("SELECT category_id FROM user_categories WHERE user_id = :userId")
    suspend fun getSelectedIds(userId: String): List<Int>

    @Query(
        """
        SELECT c.* FROM categories c
        INNER JOIN user_categories uc ON c.id = uc.category_id
        WHERE uc.user_id = :userId
        ORDER BY uc.created_at ASC
        """
    )
    fun observeSelectedCategories(userId: String): Flow<List<CategoryEntity>>

    @Query(
        """
        SELECT c.* FROM categories c
        INNER JOIN user_categories uc ON c.id = uc.category_id
        WHERE uc.user_id = :userId
        ORDER BY uc.created_at ASC
        """
    )
    suspend fun getSelectedCategories(userId: String): List<CategoryEntity>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(entry: UserCategoryEntity): Long

    @Query("DELETE FROM user_categories WHERE user_id = :userId AND category_id = :categoryId")
    suspend fun delete(userId: String, categoryId: Int)

    @Query("DELETE FROM user_categories WHERE user_id = :userId")
    suspend fun clearForUser(userId: String)

    /**
     * Replace the user's selection set with [categoryIds] in a single transaction.
     */
    @Transaction
    suspend fun replaceSelections(userId: String, categoryIds: Collection<Int>) {
        clearForUser(userId)
        val now = System.currentTimeMillis()
        categoryIds.forEach { id ->
            insert(UserCategoryEntity(user_id = userId, category_id = id, created_at = now))
        }
    }
}
