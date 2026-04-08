package com.example.betterme.data.repository

import com.example.betterme.data.local.room.dao.ChallengeDao
import com.example.betterme.data.local.room.entities.ChallengeEntity
import com.example.betterme.domain.repository.ChallengeRepository

class ChallengeRepositoryImpl(
    private val dao: ChallengeDao
) : ChallengeRepository {

    override fun getAll() = dao.getAll()

    override suspend fun getById(id: Int) = dao.getById(id)

    override suspend fun insert(challenge: ChallengeEntity) =
        dao.insert(challenge)

    override suspend fun update(challenge: ChallengeEntity) =
        dao.update(challenge)

    override suspend fun delete(challenge: ChallengeEntity) =
        dao.delete(challenge)
}