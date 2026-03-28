package com.example.betterme.domain.repository

import com.example.betterme.data.local.room.entities.UserAchievementEntity
import kotlinx.coroutines.flow.Flow

interface UserAchievementRepository {

    fun getByUser(userId: Int): Flow<List<UserAchievementEntity>>

    suspend fun insert(entity: UserAchievementEntity)

    suspend fun isAchieved(userId: Int, achievementId: Int): Boolean
}