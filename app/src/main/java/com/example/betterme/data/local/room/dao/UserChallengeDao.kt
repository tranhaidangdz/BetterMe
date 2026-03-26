package com.example.betterme.data.local.room.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.example.betterme.data.local.room.entities.UserChallengeEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UserChallengeDao {

    @Insert
    suspend fun joinChallenge(userChallenge: UserChallengeEntity)

    @Update
    suspend fun updateProgress(userChallenge: UserChallengeEntity)

    @Delete
    suspend fun leaveChallenge(userChallenge: UserChallengeEntity)

    @Query("SELECT * FROM user_challenges WHERE user_id = :userId")
    fun getUserChallenges(userId: Int): Flow<List<UserChallengeEntity>>

    @Query("""
        UPDATE user_challenges 
        SET progress = :progress 
        WHERE id = :id
    """)
    suspend fun updateProgressById(id: Int, progress: Int)
}