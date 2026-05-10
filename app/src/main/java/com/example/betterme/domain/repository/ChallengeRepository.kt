package com.example.betterme.domain.repository

import com.example.betterme.data.local.room.entities.ChallengeEntity
import kotlinx.coroutines.flow.Flow

interface ChallengeRepository {

    fun observeAll(): Flow<List<ChallengeEntity>>

    fun observeFeatured(): Flow<List<ChallengeEntity>>

    fun observeByCategory(categoryId: Int): Flow<List<ChallengeEntity>>

    fun observeGroupChallenges(): Flow<List<ChallengeEntity>>

    suspend fun search(query: String): List<ChallengeEntity>

    suspend fun getById(id: Int): ChallengeEntity?

    suspend fun insert(challenge: ChallengeEntity): Long

    suspend fun insertAll(challenges: List<ChallengeEntity>)

    suspend fun update(challenge: ChallengeEntity)

    suspend fun delete(challenge: ChallengeEntity)

    suspend fun incrementParticipantCount(id: Int)

    suspend fun count(): Int
}
