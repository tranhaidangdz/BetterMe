package com.example.betterme.data.local.room.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.example.betterme.data.local.room.entities.UserChallengeEntity
import com.example.betterme.data.local.room.relation.UserChallengeWithDetails
import kotlinx.coroutines.flow.Flow

@Dao
interface UserChallengeDao {

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(userChallenge: UserChallengeEntity): Long

    @Update
    suspend fun update(userChallenge: UserChallengeEntity)

    @Delete
    suspend fun delete(userChallenge: UserChallengeEntity)

    @Query("SELECT * FROM user_challenges WHERE user_id = :userId ORDER BY joined_at DESC")
    fun observeByUser(userId: String): Flow<List<UserChallengeEntity>>

    @Query("SELECT * FROM user_challenges WHERE user_id = :userId AND status = :status ORDER BY joined_at DESC")
    fun observeByStatus(userId: String, status: String): Flow<List<UserChallengeEntity>>

    @Transaction
    @Query("SELECT * FROM user_challenges WHERE user_id = :userId ORDER BY joined_at DESC")
    fun observeWithDetails(userId: String): Flow<List<UserChallengeWithDetails>>

    @Transaction
    @Query("SELECT * FROM user_challenges WHERE user_id = :userId AND status = :status ORDER BY joined_at DESC")
    fun observeWithDetailsByStatus(userId: String, status: String): Flow<List<UserChallengeWithDetails>>

    @Transaction
    @Query("SELECT * FROM user_challenges WHERE id = :id")
    suspend fun getWithDetailsById(id: Int): UserChallengeWithDetails?

    @Query("SELECT * FROM user_challenges WHERE id = :id")
    suspend fun getById(id: Int): UserChallengeEntity?

    @Query("SELECT * FROM user_challenges WHERE user_id = :userId AND challenge_id = :challengeId LIMIT 1")
    suspend fun getByUserAndChallenge(userId: String, challengeId: Int): UserChallengeEntity?

    // Every state-mutating UPDATE here also bumps `updated_at` and nulls `synced_at`
    // so the sync layer can recognize the row as dirty. Callers (the repository)
    // pass `updatedAt = System.currentTimeMillis()` at the call site.

    @Query("""
        UPDATE user_challenges
        SET current_streak = :currentStreak,
            best_streak = :bestStreak,
            progress_pct = :progressPct,
            last_check_in_date = :lastCheckIn,
            updated_at = :updatedAt,
            synced_at = NULL
        WHERE id = :id
    """)
    suspend fun updateProgress(
        id: Int,
        currentStreak: Int,
        bestStreak: Int,
        progressPct: Int,
        lastCheckIn: Long,
        updatedAt: Long
    )

    @Query("""
        UPDATE user_challenges
        SET status = 'COMPLETED',
            end_date = :endDate,
            progress_pct = 100,
            updated_at = :updatedAt,
            synced_at = NULL
        WHERE id = :id AND status NOT IN ('COMPLETED', 'FAILED', 'ABANDONED')
    """)
    suspend fun markCompleted(id: Int, endDate: Long, updatedAt: Long): Int

    @Query("""
        UPDATE user_challenges
        SET status = 'FAILED',
            end_date = :endDate,
            updated_at = :updatedAt,
            synced_at = NULL
        WHERE id = :id AND status NOT IN ('COMPLETED', 'FAILED', 'ABANDONED')
    """)
    suspend fun markFailed(id: Int, endDate: Long, updatedAt: Long): Int

    @Query("""
        UPDATE user_challenges
        SET status = 'ABANDONED',
            end_date = :endDate,
            updated_at = :updatedAt,
            synced_at = NULL
        WHERE id = :id
    """)
    suspend fun markAbandoned(id: Int, endDate: Long, updatedAt: Long)

    @Query("""
        UPDATE user_challenges
        SET target_end_date = :targetEndDate,
            updated_at = :updatedAt,
            synced_at = NULL
        WHERE id = :id
    """)
    suspend fun updateTargetEndDate(id: Int, targetEndDate: Long, updatedAt: Long)

    @Query("SELECT * FROM user_challenges WHERE status IN ('ACTIVE', 'UPCOMING')")
    suspend fun getAllActiveOrUpcoming(): List<UserChallengeEntity>

    @Query("SELECT COUNT(*) FROM user_challenges WHERE user_id = :userId AND status = 'COMPLETED'")
    suspend fun countCompletedByUser(userId: String): Int

    @Query("SELECT COUNT(*) FROM user_challenges WHERE user_id = :userId AND status = 'ACTIVE'")
    suspend fun countActiveByUser(userId: String): Int

    @Query("SELECT COUNT(*) FROM user_challenges WHERE user_id = :userId AND status = 'FAILED'")
    suspend fun countFailedByUser(userId: String): Int

    @Query("SELECT MAX(best_streak) FROM user_challenges WHERE user_id = :userId")
    suspend fun maxBestStreak(userId: String): Int?

    // ============================================================
    // Sync helpers (offline-first)
    // ============================================================

    /**
     * Rows for this user that have been mutated locally since their last successful
     * push. Includes soft-deleted rows (is_deleted=1) so deletions propagate.
     */
    @Query("""
        SELECT * FROM user_challenges
        WHERE user_id = :userId AND (synced_at IS NULL OR synced_at < updated_at)
    """)
    suspend fun getDirtyForUser(userId: String): List<UserChallengeEntity>

    @Query("""
        SELECT COUNT(*) FROM user_challenges
        WHERE user_id = :userId AND (synced_at IS NULL OR synced_at < updated_at)
    """)
    suspend fun countDirtyForUser(userId: String): Int

    @Query("UPDATE user_challenges SET synced_at = :syncedAt WHERE id = :id AND updated_at = :pushedUpdatedAt")
    suspend fun markSynced(id: Int, pushedUpdatedAt: Long, syncedAt: Long)
}
