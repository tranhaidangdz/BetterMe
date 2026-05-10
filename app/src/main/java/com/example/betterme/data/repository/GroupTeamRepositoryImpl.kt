package com.example.betterme.data.repository

import com.example.betterme.data.local.room.dao.GroupTeamDao
import com.example.betterme.data.local.room.entities.GroupTeamEntity
import com.example.betterme.domain.repository.GroupTeamRepository

class GroupTeamRepositoryImpl(
    private val dao: GroupTeamDao
) : GroupTeamRepository {

    override fun observeTeamsForChallenge(challengeId: Int) =
        dao.observeTeamsForChallenge(challengeId)

    override suspend fun getById(id: Int) = dao.getById(id)

    override suspend fun insertAll(teams: List<GroupTeamEntity>) = dao.insertAll(teams)

    override suspend fun incrementMemberCount(id: Int) = dao.incrementMemberCount(id)

    override suspend fun addCoinsToTeam(id: Int, coins: Int) = dao.addCoinsToTeam(id, coins)

    override suspend fun count() = dao.count()
}
