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

    // Stamp updated_at + clear synced_at so the row enters the dirty set for the
    // next sync pass. Callers can pass a row with whatever fields they want
    // edited — we always overwrite the sync columns here so the contract is
    // "any update via this method becomes a sync candidate."
    override suspend fun updateLog(log: ChallengeLogEntity) = dao.update(
        log.copy(updated_at = System.currentTimeMillis(), synced_at = null)
    )

    // Soft-delete: tombstone the row so the deletion propagates to other
    // devices via sync. Hard delete would race with the upload loop and lose
    // the deletion event before any other device sees it.
    override suspend fun deleteLog(log: ChallengeLogEntity) =
        dao.softDelete(log.id, System.currentTimeMillis())

    override suspend fun countDoneLogs(userChallengeId: Int) = dao.countDoneLogs(userChallengeId)

    override suspend fun getDoneDates(userChallengeId: Int) = dao.getDoneDates(userChallengeId)

    override suspend fun countTotalCheckInsByUser(userId: String) =
        dao.countTotalCheckInsByUser(userId)
}
