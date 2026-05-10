package com.example.betterme.data.local.room.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.betterme.data.local.room.entities.AchievementEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AchievementDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(achievement: AchievementEntity): Long

    /**
     * Idempotent badge-catalog insert. IGNORE keeps existing rows intact, which matters
     * because [com.example.betterme.data.local.room.entities.ChallengeEntity] declares
     * `onDelete = SET_NULL` on its FK to this table — REPLACE deletes-then-reinserts, and
     * the brief delete pulse would null-out every challenge's `reward_badge_id` before
     * the new badge row lands. With IGNORE, existing badges stay, new badges get added,
     * and challenge → badge bindings remain valid.
     */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(achievements: List<AchievementEntity>)

    @Query("SELECT * FROM achievements ORDER BY category, sort_order ASC")
    fun observeAll(): Flow<List<AchievementEntity>>

    @Query("SELECT * FROM achievements WHERE category = :category ORDER BY sort_order ASC")
    fun observeByCategory(category: String): Flow<List<AchievementEntity>>

    @Query("SELECT * FROM achievements WHERE id = :id")
    suspend fun getById(id: Int): AchievementEntity?

    @Query("""
        SELECT * FROM achievements
        WHERE criteria_type = :type
          AND criteria_value <= :value
          AND id NOT IN (SELECT achievement_id FROM user_achievements WHERE user_id = :userId)
    """)
    suspend fun findUnclaimedByThreshold(userId: String, type: String, value: Int): List<AchievementEntity>

    @Query("SELECT COUNT(*) FROM achievements")
    suspend fun count(): Int
}
