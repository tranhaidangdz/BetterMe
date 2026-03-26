package com.example.betterme.data.local.room.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.betterme.data.local.room.entities.AIChatEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AIChatDao {

    @Insert
    suspend fun insert(chat: AIChatEntity)

    @Query("SELECT * FROM ai_chat WHERE user_id = :userId ORDER BY created_at DESC")
    fun getChatHistory(userId: Int): Flow<List<AIChatEntity>>

    @Query("DELETE FROM ai_chat WHERE user_id = :userId")
    suspend fun clearHistory(userId: Int)
}