package com.example.betterme.data.local.room.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.example.betterme.data.local.room.entities.ChallengeEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ChallengeDao {

    @Insert
    suspend fun insert(challenge: ChallengeEntity)

    @Update
    suspend fun update(challenge: ChallengeEntity)

    @Delete
    suspend fun delete(challenge: ChallengeEntity)

    @Query("SELECT * FROM challenges")
    fun getAll(): Flow<List<ChallengeEntity>>

    @Query("SELECT * FROM challenges WHERE id = :id")
    suspend fun getById(id: Int): ChallengeEntity?
}