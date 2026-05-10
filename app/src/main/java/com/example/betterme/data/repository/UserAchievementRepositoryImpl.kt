package com.example.betterme.data.repository

import com.example.betterme.data.local.room.dao.UserAchievementDao
import com.example.betterme.data.local.room.entities.UserAchievementEntity
import com.example.betterme.domain.repository.UserAchievementRepository

class UserAchievementRepositoryImpl(
    private val dao: UserAchievementDao
) : UserAchievementRepository {

    override fun observeByUser(userId: String) = dao.observeByUser(userId)

    override fun observeByUserWithBadge(userId: String) = dao.observeByUserWithBadge(userId)

    override suspend fun insert(entity: UserAchievementEntity) = dao.insert(entity)

    override suspend fun hasEarned(userId: String, achievementId: Int) =
        dao.hasEarned(userId, achievementId)

    override suspend fun award(
        userId: String,
        achievementId: Int,
        sourceUserChallengeId: Int?
    ): Long {
        val entity = UserAchievementEntity(
            user_id = userId,
            achievement_id = achievementId,
            achieved_at = System.currentTimeMillis(),
            source_user_challenge_id = sourceUserChallengeId
        )
        return dao.insert(entity)
    }

    override suspend fun countByUser(userId: String) = dao.countByUser(userId)
}
