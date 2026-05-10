package com.example.betterme.data.local.room.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.example.betterme.data.local.room.entities.UserAchievementEntity
import com.example.betterme.data.local.room.relation.UserAchievementWithBadge
import kotlinx.coroutines.flow.Flow

@Dao
interface UserAchievementDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(userAchievement: UserAchievementEntity): Long

    @Query("SELECT * FROM user_achievements WHERE user_id = :userId ORDER BY achieved_at DESC")
    fun observeByUser(userId: String): Flow<List<UserAchievementEntity>>

    @Transaction
    @Query("SELECT * FROM user_achievements WHERE user_id = :userId ORDER BY achieved_at DESC")
    fun observeByUserWithBadge(userId: String): Flow<List<UserAchievementWithBadge>>

    @Query("""
        SELECT EXISTS(
            SELECT 1 FROM user_achievements
            WHERE user_id = :userId AND achievement_id = :achievementId
        )
    """)
    suspend fun hasEarned(userId: String, achievementId: Int): Boolean

    @Query("SELECT COUNT(*) FROM user_achievements WHERE user_id = :userId")
    suspend fun countByUser(userId: String): Int
}
