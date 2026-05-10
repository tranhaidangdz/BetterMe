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
}
