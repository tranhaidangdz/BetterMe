package com.example.betterme.data.local.room.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.example.betterme.data.local.room.entities.CategoryEntity
import com.example.betterme.data.local.room.entities.UserCategoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UserCategoryDao {

    @Query("SELECT category_id FROM user_categories WHERE user_id = :userId AND is_deleted = 0")
    fun observeSelectedIds(userId: String): Flow<List<Int>>

    @Query("SELECT category_id FROM user_categories WHERE user_id = :userId AND is_deleted = 0")
    suspend fun getSelectedIds(userId: String): List<Int>

    @Query(
        """
        SELECT c.* FROM categories c
        INNER JOIN user_categories uc ON c.id = uc.category_id
        WHERE uc.user_id = :userId AND uc.is_deleted = 0
        ORDER BY uc.created_at ASC
        """
    )
    fun observeSelectedCategories(userId: String): Flow<List<CategoryEntity>>

    @Query(
        """
        SELECT c.* FROM categories c
        INNER JOIN user_categories uc ON c.id = uc.category_id
        WHERE uc.user_id = :userId AND uc.is_deleted = 0
        ORDER BY uc.created_at ASC
        """
    )
    suspend fun getSelectedCategories(userId: String): List<CategoryEntity>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(entry: UserCategoryEntity): Long

    @Update
    suspend fun update(entry: UserCategoryEntity)

    @Query(
        """
        SELECT * FROM user_categories
        WHERE user_id = :userId AND category_id = :categoryId
        LIMIT 1
        """
    )
    suspend fun find(userId: String, categoryId: Int): UserCategoryEntity?

    @Query(
        """
        UPDATE user_categories
        SET is_deleted = 1, updated_at = :now, synced_at = NULL
        WHERE user_id = :userId AND category_id = :categoryId
        """
    )
    suspend fun softDelete(userId: String, categoryId: Int, now: Long = System.currentTimeMillis())

    @Query(
        """
        UPDATE user_categories
        SET is_deleted = 1, updated_at = :now, synced_at = NULL
        WHERE user_id = :userId AND is_deleted = 0
        """
    )
    suspend fun softDeleteAll(userId: String, now: Long = System.currentTimeMillis())

    /**
     * Replace the user's selection set with [categoryIds] in a single transaction.
     * Existing rows for ids not in the new set are soft-deleted (so the sync layer
     * propagates the removal); existing rows in the new set are re-activated by
     * bumping `is_deleted=false` + `updated_at`; new ids get fresh inserts.
     */
    @Transaction
    suspend fun replaceSelections(userId: String, categoryIds: Collection<Int>) {
        val now = System.currentTimeMillis()
        val keep = categoryIds.toSet()

        // Soft-delete everything currently active that isn't in the new set.
        val current = getSelectedIds(userId).toSet()
        for (id in current) {
            if (id !in keep) softDelete(userId, id, now)
        }

        // Insert or re-activate the keep set.
        for (id in keep) {
            val existing = find(userId, id)
            if (existing == null) {
                insert(
                    UserCategoryEntity(
                        user_id = userId,
                        category_id = id,
                        created_at = now,
                        updated_at = now,
                        synced_at = null,
                        is_deleted = false
                    )
                )
            } else if (existing.is_deleted) {
                update(
                    existing.copy(
                        is_deleted = false,
                        updated_at = now,
                        synced_at = null
                    )
                )
            }
        }
    }

    // ===== Sync support =====
    @Query(
        """
        SELECT * FROM user_categories
        WHERE user_id = :userId
          AND (synced_at IS NULL OR synced_at < updated_at)
        """
    )
    suspend fun getDirty(userId: String): List<UserCategoryEntity>

    @Query(
        """
        SELECT COUNT(*) FROM user_categories
        WHERE user_id = :userId
          AND (synced_at IS NULL OR synced_at < updated_at)
        """
    )
    suspend fun countDirty(userId: String): Int

    @Query(
        """
        UPDATE user_categories
        SET synced_at = :syncedAt
        WHERE id = :id AND updated_at <= :upToUpdatedAt
        """
    )
    suspend fun markSynced(id: Int, upToUpdatedAt: Long, syncedAt: Long)
}
