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

    @Insert(onConflict = OnConflictStrategy.REPLACE)
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
