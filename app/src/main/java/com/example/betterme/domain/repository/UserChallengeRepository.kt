package com.example.betterme.domain.repository

import com.example.betterme.data.local.room.entities.UserChallengeEntity
import com.example.betterme.data.local.room.relation.UserChallengeWithDetails
import kotlinx.coroutines.flow.Flow

interface UserChallengeRepository {

    fun observeByUser(userId: String): Flow<List<UserChallengeEntity>>

    fun observeByStatus(userId: String, status: String): Flow<List<UserChallengeEntity>>

    fun observeWithDetails(userId: String): Flow<List<UserChallengeWithDetails>>

    fun observeWithDetailsByStatus(userId: String, status: String): Flow<List<UserChallengeWithDetails>>

    suspend fun getById(id: Int): UserChallengeEntity?

    suspend fun getWithDetailsById(id: Int): UserChallengeWithDetails?

    suspend fun getByUserAndChallenge(userId: String, challengeId: Int): UserChallengeEntity?

    suspend fun insert(userChallenge: UserChallengeEntity): Long

    suspend fun update(userChallenge: UserChallengeEntity)

    suspend fun delete(userChallenge: UserChallengeEntity)

    suspend fun updateProgress(
        id: Int,
        currentStreak: Int,
        bestStreak: Int,
        progressPct: Int,
        lastCheckIn: Long
    )

    suspend fun markCompleted(id: Int, endDate: Long)

    suspend fun markAbandoned(id: Int, endDate: Long)

    suspend fun countCompletedByUser(userId: String): Int

    suspend fun countActiveByUser(userId: String): Int

    suspend fun maxBestStreak(userId: String): Int?
}
