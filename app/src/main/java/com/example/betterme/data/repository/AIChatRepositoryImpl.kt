package com.example.betterme.data.repository

import com.example.betterme.data.local.room.dao.AIChatDao
import com.example.betterme.data.local.room.entities.AIChatEntity
import com.example.betterme.domain.repository.AIChatRepository

class AIChatRepositoryImpl(
    private val dao: AIChatDao
) : AIChatRepository {

    override fun getHistory(userId: Int) =
        dao.getChatHistory(userId)

    override suspend fun insert(chat: AIChatEntity) =
        dao.insert(chat)

    override suspend fun clear(userId: Int) =
        dao.clearHistory(userId)
}