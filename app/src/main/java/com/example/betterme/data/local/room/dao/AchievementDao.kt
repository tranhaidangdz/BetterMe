package com.example.betterme.data.local.room.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.betterme.data.local.room.entities.AchievementEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AchievementDao {

    @Insert
    suspend fun insert(achievement: AchievementEntity)

    @Query("SELECT * FROM achievements")
    fun getAll(): Flow<List<AchievementEntity>>
}