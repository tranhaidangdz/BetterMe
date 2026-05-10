package com.example.betterme.domain.repository

import com.example.betterme.data.local.room.entities.UserAchievementEntity
import com.example.betterme.data.local.room.relation.UserAchievementWithBadge
import kotlinx.coroutines.flow.Flow

interface UserAchievementRepository {

    fun observeByUser(userId: String): Flow<List<UserAchievementEntity>>

    fun observeByUserWithBadge(userId: String): Flow<List<UserAchievementWithBadge>>

    suspend fun insert(entity: UserAchievementEntity): Long

    suspend fun hasEarned(userId: String, achievementId: Int): Boolean

    suspend fun award(userId: String, achievementId: Int, sourceUserChallengeId: Int? = null): Long

    suspend fun countByUser(userId: String): Int
}
