package com.example.betterme.data.repository

import com.example.betterme.data.local.room.dao.ChallengeLogDao
import com.example.betterme.data.local.room.entities.ChallengeLogEntity
import com.example.betterme.domain.repository.ChallengeLogRepository

class ChallengeLogRepositoryImpl(
    private val dao: ChallengeLogDao
) : ChallengeLogRepository {

    override fun observeLogs(userChallengeId: Int) = dao.observeLogs(userChallengeId)

    override suspend fun getLogByDate(userChallengeId: Int, date: Long) =
        dao.getLogByDate(userChallengeId, date)

    override suspend fun addLog(log: ChallengeLogEntity) = dao.insert(log)

    override suspend fun updateLog(log: ChallengeLogEntity) = dao.update(log)

    override suspend fun deleteLog(log: ChallengeLogEntity) = dao.delete(log)

    override suspend fun countDoneLogs(userChallengeId: Int) = dao.countDoneLogs(userChallengeId)

    override suspend fun getDoneDates(userChallengeId: Int) = dao.getDoneDates(userChallengeId)

    override suspend fun countTotalCheckInsByUser(userId: String) =
        dao.countTotalCheckInsByUser(userId)
}
