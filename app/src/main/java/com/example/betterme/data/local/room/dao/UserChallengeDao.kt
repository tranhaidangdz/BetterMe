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

    @Query("""
        UPDATE user_challenges
        SET current_streak = :currentStreak,
            best_streak = :bestStreak,
            progress_pct = :progressPct,
            last_check_in_date = :lastCheckIn
        WHERE id = :id
    """)
    suspend fun updateProgress(
        id: Int,
        currentStreak: Int,
        bestStreak: Int,
        progressPct: Int,
        lastCheckIn: Long
    )

    @Query("""
        UPDATE user_challenges
        SET status = 'COMPLETED',
            end_date = :endDate,
            progress_pct = 100
        WHERE id = :id
    """)
    suspend fun markCompleted(id: Int, endDate: Long)

    @Query("UPDATE user_challenges SET status = 'ABANDONED', end_date = :endDate WHERE id = :id")
    suspend fun markAbandoned(id: Int, endDate: Long)

    @Query("SELECT COUNT(*) FROM user_challenges WHERE user_id = :userId AND status = 'COMPLETED'")
    suspend fun countCompletedByUser(userId: String): Int

    @Query("SELECT COUNT(*) FROM user_challenges WHERE user_id = :userId AND status = 'ACTIVE'")
    suspend fun countActiveByUser(userId: String): Int

    @Query("SELECT MAX(best_streak) FROM user_challenges WHERE user_id = :userId")
    suspend fun maxBestStreak(userId: String): Int?
}
