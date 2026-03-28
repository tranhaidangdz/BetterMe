package com.example.betterme.domain.repository

import com.example.betterme.data.local.room.entities.AIChatEntity
import kotlinx.coroutines.flow.Flow

interface AIChatRepository {

    fun getHistory(userId: Int): Flow<List<AIChatEntity>>

    suspend fun insert(chat: AIChatEntity)

    suspend fun clear(userId: Int)
}