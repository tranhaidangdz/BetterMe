package com.example.betterme.domain.repository

import com.example.betterme.data.local.room.entities.AchievementEntity
import kotlinx.coroutines.flow.Flow

interface AchievementRepository {

    fun observeAll(): Flow<List<AchievementEntity>>

    fun observeByCategory(category: String): Flow<List<AchievementEntity>>

    suspend fun getById(id: Int): AchievementEntity?

    suspend fun insert(achievement: AchievementEntity): Long

    suspend fun insertAll(achievements: List<AchievementEntity>)

    suspend fun findUnclaimedByThreshold(
        userId: String,
        type: String,
        value: Int
    ): List<AchievementEntity>

    suspend fun count(): Int
}
