package com.example.betterme.data.repository

import com.example.betterme.data.local.room.dao.AchievementDao
import com.example.betterme.data.local.room.entities.AchievementEntity
import com.example.betterme.domain.repository.AchievementRepository

class AchievementRepositoryImpl(
    private val dao: AchievementDao
) : AchievementRepository {

    override fun observeAll() = dao.observeAll()

    override fun observeByCategory(category: String) = dao.observeByCategory(category)

    override suspend fun getById(id: Int) = dao.getById(id)

    override suspend fun insert(achievement: AchievementEntity) = dao.insert(achievement)

    override suspend fun insertAll(achievements: List<AchievementEntity>) =
        dao.insertAll(achievements)

    override suspend fun findUnclaimedByThreshold(
        userId: String,
        type: String,
        value: Int
    ) = dao.findUnclaimedByThreshold(userId, type, value)

    override suspend fun count() = dao.count()
}
