package com.example.betterme.data.local.room.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.betterme.data.local.room.entities.ChallengeEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ChallengeDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(challenge: ChallengeEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(challenges: List<ChallengeEntity>)

    @Update
    suspend fun update(challenge: ChallengeEntity)

    @Delete
    suspend fun delete(challenge: ChallengeEntity)

    @Query("SELECT * FROM challenges ORDER BY sort_order ASC, id ASC")
    fun observeAll(): Flow<List<ChallengeEntity>>

    @Query("SELECT * FROM challenges WHERE is_featured = 1 ORDER BY sort_order ASC")
    fun observeFeatured(): Flow<List<ChallengeEntity>>

    @Query("SELECT * FROM challenges WHERE category_id = :categoryId ORDER BY sort_order ASC")
    fun observeByCategory(categoryId: Int): Flow<List<ChallengeEntity>>

    @Query("SELECT * FROM challenges WHERE is_group = 1 ORDER BY sort_order ASC")
    fun observeGroupChallenges(): Flow<List<ChallengeEntity>>

    @Query("""
        SELECT * FROM challenges
        WHERE title LIKE '%' || :query || '%'
           OR description LIKE '%' || :query || '%'
        ORDER BY is_featured DESC, sort_order ASC
    """)
    suspend fun search(query: String): List<ChallengeEntity>

    @Query("SELECT * FROM challenges WHERE id = :id")
    suspend fun getById(id: Int): ChallengeEntity?

    @Query("UPDATE challenges SET participant_count = participant_count + 1 WHERE id = :id")
    suspend fun incrementParticipantCount(id: Int)

    @Query("SELECT COUNT(*) FROM challenges")
    suspend fun count(): Int
}
