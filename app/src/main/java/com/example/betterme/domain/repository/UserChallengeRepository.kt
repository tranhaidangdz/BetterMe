package com.example.betterme.domain.repository

import com.example.betterme.data.local.room.entities.UserChallengeEntity
import kotlinx.coroutines.flow.Flow

interface UserChallengeRepository {

    fun getUserChallenges(userId: Int): Flow<List<UserChallengeEntity>>

    suspend fun joinChallenge(entity: UserChallengeEntity)

    suspend fun updateProgress(entity: UserChallengeEntity)

    suspend fun updateProgressById(id: Int, progress: Int)

    suspend fun leaveChallenge(entity: UserChallengeEntity)
}