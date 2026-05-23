package com.example.betterme.data.repository

import com.example.betterme.data.local.room.dao.UserChallengeDao
import com.example.betterme.data.local.room.entities.UserChallengeEntity
import com.example.betterme.domain.repository.UserChallengeRepository

class UserChallengeRepositoryImpl(
    private val dao: UserChallengeDao
) : UserChallengeRepository {

    override fun observeByUser(userId: String) = dao.observeByUser(userId)

    override fun observeByStatus(userId: String, status: String) =
        dao.observeByStatus(userId, status)

    override fun observeWithDetails(userId: String) = dao.observeWithDetails(userId)

    override fun observeWithDetailsByStatus(userId: String, status: String) =
        dao.observeWithDetailsByStatus(userId, status)

    override suspend fun getById(id: Int) = dao.getById(id)

    override suspend fun getWithDetailsById(id: Int) = dao.getWithDetailsById(id)

    override suspend fun getByUserAndChallenge(userId: String, challengeId: Int) =
        dao.getByUserAndChallenge(userId, challengeId)

    override suspend fun insert(userChallenge: UserChallengeEntity) = dao.insert(userChallenge)

    override suspend fun update(userChallenge: UserChallengeEntity) = dao.update(userChallenge)

    override suspend fun delete(userChallenge: UserChallengeEntity) = dao.delete(userChallenge)

    // All state mutations stamp `updated_at = now` and null `synced_at` via the DAO
    // queries below. The sync layer's dirty-row query catches the change on the
    // next pass.

    override suspend fun updateProgress(
        id: Int,
        currentStreak: Int,
        bestStreak: Int,
        progressPct: Int,
        lastCheckIn: Long
    ) = dao.updateProgress(
        id, currentStreak, bestStreak, progressPct, lastCheckIn,
        updatedAt = System.currentTimeMillis()
    )

    override suspend fun markCompleted(id: Int, endDate: Long): Boolean =
        dao.markCompleted(id, endDate, updatedAt = System.currentTimeMillis()) > 0

    override suspend fun markFailed(id: Int, endDate: Long): Boolean =
        dao.markFailed(id, endDate, updatedAt = System.currentTimeMillis()) > 0

    override suspend fun markAbandoned(id: Int, endDate: Long) =
        dao.markAbandoned(id, endDate, updatedAt = System.currentTimeMillis())

    override suspend fun updateTargetEndDate(id: Int, targetEndDate: Long) =
        dao.updateTargetEndDate(id, targetEndDate, updatedAt = System.currentTimeMillis())

    override suspend fun getAllActiveOrUpcoming() = dao.getAllActiveOrUpcoming()

    override suspend fun countCompletedByUser(userId: String) = dao.countCompletedByUser(userId)

    override suspend fun countActiveByUser(userId: String) = dao.countActiveByUser(userId)

    override suspend fun countFailedByUser(userId: String) = dao.countFailedByUser(userId)

    override suspend fun maxBestStreak(userId: String) = dao.maxBestStreak(userId)
}
