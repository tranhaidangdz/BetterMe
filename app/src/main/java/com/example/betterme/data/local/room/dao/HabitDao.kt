package com.example.betterme.data.local.room.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.betterme.data.local.room.entities.HabitEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface HabitDao {

    // CREATE
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHabit(habit: HabitEntity): Long

    // UPDATE
    @Update
    suspend fun updateHabit(habit: HabitEntity)

    // DELETE
    @Delete
    suspend fun deleteHabit(habit: HabitEntity)

    @Query("DELETE FROM habits WHERE id = :habitId")
    suspend fun deleteHabitById(habitId: Int)

    /**
     * Soft-delete used by the sync layer. Hard deletes race with the upload loop
     * and disappear before propagation; this preserves the audit trail until the
     * deletion has been confirmed remotely.
     */
    @Query("""
        UPDATE habits
        SET is_deleted = 1,
            updated_at = :updatedAt,
            synced_at = NULL
        WHERE id = :habitId
    """)
    suspend fun softDeleteHabit(habitId: Int, updatedAt: Long)

    // READ
    @Query("SELECT * FROM habits WHERE user_id = :userId")
    fun getHabitsByUser(userId: String): Flow<List<HabitEntity>>

    @Query("SELECT * FROM habits WHERE id = :habitId")
    suspend fun getHabitById(habitId: Int): HabitEntity?

    @Query("SELECT * FROM habits WHERE category_id = :categoryId AND user_id = :userId")
    fun getHabitsByCategoryForUser(categoryId: Int, userId: String): Flow<List<HabitEntity>>

    @Query("SELECT COUNT(*) FROM habits WHERE category_id = :categoryId AND user_id = :userId")
    suspend fun getHabitCountByCategoryForUser(categoryId: Int, userId: String): Int

    @Query("DELETE FROM habits WHERE user_id = :userId")
    suspend fun deleteAllByUserId(userId: String)

    // ============================================================
    // Sync helpers (offline-first)
    // ============================================================

    @Query("""
        SELECT * FROM habits
        WHERE user_id = :userId
          AND (synced_at IS NULL OR synced_at < updated_at)
    """)
    suspend fun getDirtyHabits(userId: String): List<HabitEntity>

    @Query("""
        SELECT COUNT(*) FROM habits
        WHERE user_id = :userId
          AND (synced_at IS NULL OR synced_at < updated_at)
    """)
    suspend fun countDirtyHabits(userId: String): Int

    @Query("UPDATE habits SET synced_at = :syncedAt WHERE id = :id AND updated_at = :pushedUpdatedAt")
    suspend fun markHabitSynced(id: Int, pushedUpdatedAt: Long, syncedAt: Long)
}