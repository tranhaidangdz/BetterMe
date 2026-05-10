package com.example.betterme.domain.repository

import com.example.betterme.data.local.room.entities.GroupTeamEntity
import kotlinx.coroutines.flow.Flow

interface GroupTeamRepository {

    fun observeTeamsForChallenge(challengeId: Int): Flow<List<GroupTeamEntity>>

    suspend fun getById(id: Int): GroupTeamEntity?

    suspend fun insertAll(teams: List<GroupTeamEntity>)

    suspend fun incrementMemberCount(id: Int)

    suspend fun addCoinsToTeam(id: Int, coins: Int)

    suspend fun count(): Int
}
