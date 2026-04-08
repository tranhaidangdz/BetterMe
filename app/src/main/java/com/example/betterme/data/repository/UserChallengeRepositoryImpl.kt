package com.example.betterme.data.repository

import com.example.betterme.data.local.room.dao.UserChallengeDao
import com.example.betterme.data.local.room.entities.UserChallengeEntity
import com.example.betterme.domain.repository.UserChallengeRepository

class UserChallengeRepositoryImpl(
    private val dao: UserChallengeDao
) : UserChallengeRepository {

    override fun getUserChallenges(userId: Int) =
        dao.getUserChallenges(userId)

    override suspend fun joinChallenge(entity: UserChallengeEntity) =
        dao.joinChallenge(entity)

    override suspend fun updateProgress(entity: UserChallengeEntity) =
        dao.updateProgress(entity)

    override suspend fun updateProgressById(id: Int, progress: Int) =
        dao.updateProgressById(id, progress)

    override suspend fun leaveChallenge(entity: UserChallengeEntity) =
        dao.leaveChallenge(entity)
}