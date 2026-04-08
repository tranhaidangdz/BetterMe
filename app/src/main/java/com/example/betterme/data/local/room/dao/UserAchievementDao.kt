package com.example.betterme.data.local.room.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.betterme.data.local.room.entities.UserAchievementEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UserAchievementDao {

    @Insert
    suspend fun insert(userAchievement: UserAchievementEntity)

    @Query("SELECT * FROM user_achievements WHERE user_id = :userId")
    fun getByUser(userId: Int): Flow<List<UserAchievementEntity>>

    @Query("""
        SELECT EXISTS(
            SELECT 1 FROM user_achievements 
            WHERE user_id = :userId AND achievement_id = :achievementId
        )
    """)
    suspend fun isAchieved(userId: Int, achievementId: Int): Boolean
}