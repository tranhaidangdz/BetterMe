package com.example.betterme.data.local.room.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.example.betterme.data.local.room.entities.HabitLogEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface HabitLogDao {

    @Insert
    suspend fun insertLog(log: HabitLogEntity)

    @Update
    suspend fun updateLog(log: HabitLogEntity)

    @Delete
    suspend fun deleteLog(log: HabitLogEntity)

    @Query("DELETE FROM habit_logs WHERE id = :logId")
    suspend fun deleteById(logId: Int)

    @Query("SELECT * FROM habit_logs WHERE habit_id = :habitId ORDER BY date DESC")
    fun getLogsByHabit(habitId: Int): Flow<List<HabitLogEntity>>

    /**
     * Used purely as a change signal — Room emits a fresh list every time `habit_logs` is
     * mutated, which lets list-style screens (Daily Habits, Tasks tab) recompute journey
     * completion + per-day check-in state reactively after any check-in lands.
     */
    @Query("SELECT * FROM habit_logs")
    fun observeAllLogs(): Flow<List<HabitLogEntity>>

    @Query("SELECT * FROM habit_logs WHERE habit_id = :habitId AND date = :date")
    suspend fun getLogByDate(habitId: Int, date: Long): HabitLogEntity?

    // thống kê
    @Query("""
        SELECT COUNT(*) FROM habit_logs 
        WHERE habit_id = :habitId AND status = 'DONE'
    """)
    suspend fun countCompleted(habitId: Int): Int

    /** Lấy tất cả habitId đã DONE trong ngày (date = startOfDay millis) */
    @Query("""
        SELECT habit_id FROM habit_logs
        WHERE date = :dateMillis AND status = 'DONE'
    """)
    suspend fun getCompletedHabitIdsByDate(dateMillis: Long): List<Int>

    // ============================================================
    // Sync helpers (offline-first)
    // ============================================================

    /**
     * Soft-delete a single log row. Used in preference to hard delete so the
     * deletion propagates via sync.
     */
    @Query("""
        UPDATE habit_logs
        SET is_deleted = 1,
            updated_at = :updatedAt,
            synced_at = NULL
        WHERE id = :logId
    """)
    suspend fun softDeleteLog(logId: Int, updatedAt: Long)

    @Query("""
        SELECT hl.* FROM habit_logs hl
        INNER JOIN habits h ON hl.habit_id = h.id
        WHERE h.user_id = :userId
          AND (hl.synced_at IS NULL OR hl.synced_at < hl.updated_at)
    """)
    suspend fun getDirtyHabitLogs(userId: String): List<HabitLogEntity>

    @Query("""
        SELECT COUNT(*) FROM habit_logs hl
        INNER JOIN habits h ON hl.habit_id = h.id
        WHERE h.user_id = :userId
          AND (hl.synced_at IS NULL OR hl.synced_at < hl.updated_at)
    """)
    suspend fun countDirtyHabitLogs(userId: String): Int

    @Query("UPDATE habit_logs SET synced_at = :syncedAt WHERE id = :id AND updated_at = :pushedUpdatedAt")
    suspend fun markHabitLogSynced(id: Int, pushedUpdatedAt: Long, syncedAt: Long)
}