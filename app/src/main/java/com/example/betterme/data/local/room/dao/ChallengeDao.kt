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

    /**
     * Idempotent catalog insert: keeps existing rows untouched. Using IGNORE here
     * (instead of REPLACE) is critical because [com.example.betterme.data.local.room.entities.UserChallengeEntity]
     * declares `onDelete = CASCADE` on its FK to this table — REPLACE deletes-then-inserts
     * the parent row, which would cascade-wipe a user's joined challenges and check-in
     * logs every time the seeder backfilled a new entry.
     *
     * Trade-off: re-running the seed never refreshes already-shipped content (e.g. an
     * edited title on challenge id=1 stays as the prior version on existing installs).
     * That's acceptable for a curated catalog where rewrites are rare; if we ever need
     * to push content updates, do it via a versioned migration that explicitly UPDATEs
     * the affected columns rather than re-INSERTing the row.
     */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
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
