package com.example.betterme.data.local.room.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.betterme.data.local.room.entities.GroupTeamEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface GroupTeamDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(teams: List<GroupTeamEntity>)

    @Query("SELECT * FROM group_teams WHERE challenge_id = :challengeId ORDER BY total_coins DESC, rank ASC")
    fun observeTeamsForChallenge(challengeId: Int): Flow<List<GroupTeamEntity>>

    @Query("SELECT * FROM group_teams WHERE id = :id")
    suspend fun getById(id: Int): GroupTeamEntity?

    @Query("UPDATE group_teams SET member_count = member_count + 1 WHERE id = :id")
    suspend fun incrementMemberCount(id: Int)

    @Query("UPDATE group_teams SET total_coins = total_coins + :coins WHERE id = :id")
    suspend fun addCoinsToTeam(id: Int, coins: Int)

    @Query("SELECT COUNT(*) FROM group_teams")
    suspend fun count(): Int
}
