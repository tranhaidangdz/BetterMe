package com.example.betterme.domain.repository

import com.example.betterme.data.local.room.entities.ChallengeEntity
import kotlinx.coroutines.flow.Flow

interface ChallengeRepository {

    fun getAll(): Flow<List<ChallengeEntity>>

    suspend fun getById(id: Int): ChallengeEntity?

    suspend fun insert(challenge: ChallengeEntity)

    suspend fun update(challenge: ChallengeEntity)

    suspend fun delete(challenge: ChallengeEntity)
}