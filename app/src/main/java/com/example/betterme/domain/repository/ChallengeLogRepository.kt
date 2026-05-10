package com.example.betterme.domain.repository

import com.example.betterme.data.local.room.entities.ChallengeLogEntity
import kotlinx.coroutines.flow.Flow

interface ChallengeLogRepository {

    fun observeLogs(userChallengeId: Int): Flow<List<ChallengeLogEntity>>

    suspend fun getLogByDate(userChallengeId: Int, date: Long): ChallengeLogEntity?

    suspend fun addLog(log: ChallengeLogEntity): Long

    suspend fun updateLog(log: ChallengeLogEntity)

    suspend fun deleteLog(log: ChallengeLogEntity)

    suspend fun countDoneLogs(userChallengeId: Int): Int

    suspend fun getDoneDates(userChallengeId: Int): List<Long>

    suspend fun countTotalCheckInsByUser(userId: String): Int
}
