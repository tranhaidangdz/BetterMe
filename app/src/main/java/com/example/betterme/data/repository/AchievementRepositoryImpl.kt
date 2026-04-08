package com.example.betterme.data.repository

import com.example.betterme.data.local.room.dao.AchievementDao
import com.example.betterme.data.local.room.entities.AchievementEntity
import com.example.betterme.domain.repository.AchievementRepository

class AchievementRepositoryImpl(
    private val dao: AchievementDao
) : AchievementRepository {

    override fun getAll() = dao.getAll()

    override suspend fun insert(achievement: AchievementEntity) =
        dao.insert(achievement)
}