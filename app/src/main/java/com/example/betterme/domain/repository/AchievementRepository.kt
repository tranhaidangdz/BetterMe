package com.example.betterme.domain.repository

import com.example.betterme.data.local.room.entities.AchievementEntity
import kotlinx.coroutines.flow.Flow

interface AchievementRepository {

    fun getAll(): Flow<List<AchievementEntity>>

    suspend fun insert(achievement: AchievementEntity)
}