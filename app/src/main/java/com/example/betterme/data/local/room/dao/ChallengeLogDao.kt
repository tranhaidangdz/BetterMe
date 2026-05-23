package com.example.betterme.data.local.room.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.betterme.data.local.room.entities.ChallengeLogEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ChallengeLogDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(log: ChallengeLogEntity): Long

    @Update
    suspend fun update(log: ChallengeLogEntity)

    @Delete
    suspend fun delete(log: ChallengeLogEntity)

    @Query("SELECT * FROM challenge_logs WHERE user_challenge_id = :ucId ORDER BY date DESC")
    fun observeLogs(ucId: Int): Flow<List<ChallengeLogEntity>>

    @Query("SELECT * FROM challenge_logs WHERE user_challenge_id = :ucId AND date = :date LIMIT 1")
    suspend fun getLogByDate(ucId: Int, date: Long): ChallengeLogEntity?

    @Query("SELECT COUNT(*) FROM challenge_logs WHERE user_challenge_id = :ucId AND status = 'DONE'")
    suspend fun countDoneLogs(ucId: Int): Int

    @Query("SELECT date FROM challenge_logs WHERE user_challenge_id = :ucId AND status = 'DONE' ORDER BY date ASC")
    suspend fun getDoneDates(ucId: Int): List<Long>

    @Query("""
        SELECT COUNT(*) FROM challenge_logs cl
        INNER JOIN user_challenges uc ON cl.user_challenge_id = uc.id
        WHERE uc.user_id = :userId AND cl.status = 'DONE'
    """)
    suspend fun countTotalCheckInsByUser(userId: String): Int

    // ============================================================
    // Sync helpers (offline-first)
    // ============================================================

    /**
     * Logs that belong to one of the user's challenges AND have not been pushed since
     * their last local mutation. Joins through `user_challenges` so a remote row owned
     * by a different user can never accidentally be uploaded under this user's tree.
     */
    @Query("""
        SELECT cl.* FROM challenge_logs cl
        INNER JOIN user_challenges uc ON cl.user_challenge_id = uc.id
        WHERE uc.user_id = :userId
          AND (cl.synced_at IS NULL OR cl.synced_at < cl.updated_at)
    """)
    suspend fun getDirtyLogs(userId: String): List<ChallengeLogEntity>

    @Query("""
        SELECT COUNT(*) FROM challenge_logs cl
        INNER JOIN user_challenges uc ON cl.user_challenge_id = uc.id
        WHERE uc.user_id = :userId
          AND (cl.synced_at IS NULL OR cl.synced_at < cl.updated_at)
    """)
    suspend fun countDirtyLogs(userId: String): Int

    @Query("UPDATE challenge_logs SET synced_at = :syncedAt WHERE id = :logId AND updated_at = :pushedUpdatedAt")
    suspend fun markLogSynced(logId: Int, pushedUpdatedAt: Long, syncedAt: Long)
}
