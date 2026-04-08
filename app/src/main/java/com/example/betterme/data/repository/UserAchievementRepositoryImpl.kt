package com.example.betterme.data.repository

import com.example.betterme.data.local.room.dao.UserAchievementDao
import com.example.betterme.data.local.room.entities.UserAchievementEntity
import com.example.betterme.domain.repository.UserAchievementRepository

class UserAchievementRepositoryImpl(
    private val dao: UserAchievementDao
) : UserAchievementRepository {

    override fun getByUser(userId: Int) =
        dao.getByUser(userId)

    override suspend fun insert(entity: UserAchievementEntity) =
        dao.insert(entity)

    override suspend fun isAchieved(userId: Int, achievementId: Int) =
        dao.isAchieved(userId, achievementId)
}