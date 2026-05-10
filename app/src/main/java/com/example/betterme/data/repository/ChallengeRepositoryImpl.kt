package com.example.betterme.data.repository

import com.example.betterme.data.local.room.dao.ChallengeDao
import com.example.betterme.data.local.room.entities.ChallengeEntity
import com.example.betterme.domain.repository.ChallengeRepository

class ChallengeRepositoryImpl(
    private val dao: ChallengeDao
) : ChallengeRepository {

    override fun observeAll() = dao.observeAll()

    override fun observeFeatured() = dao.observeFeatured()

    override fun observeByCategory(categoryId: Int) = dao.observeByCategory(categoryId)

    override fun observeGroupChallenges() = dao.observeGroupChallenges()

    override suspend fun search(query: String) = dao.search(query)

    override suspend fun getById(id: Int) = dao.getById(id)

    override suspend fun insert(challenge: ChallengeEntity) = dao.insert(challenge)

    override suspend fun insertAll(challenges: List<ChallengeEntity>) = dao.insertAll(challenges)

    override suspend fun update(challenge: ChallengeEntity) = dao.update(challenge)

    override suspend fun delete(challenge: ChallengeEntity) = dao.delete(challenge)

    override suspend fun incrementParticipantCount(id: Int) = dao.incrementParticipantCount(id)

    override suspend fun count() = dao.count()
}
