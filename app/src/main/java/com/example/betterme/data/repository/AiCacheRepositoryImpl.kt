package com.example.betterme.data.repository

import com.example.betterme.data.local.room.dao.AiCacheDao
import com.example.betterme.data.local.room.entities.AiCacheEntity
import com.example.betterme.domain.ai.AiCacheRepository
import com.example.betterme.domain.ai.AiCacheRepository.CachedEntry

class AiCacheRepositoryImpl(
    private val dao: AiCacheDao
) : AiCacheRepository {

    override suspend fun getFresh(categoryId: Int, type: String, ttlMs: Long): String? {
        val row = dao.get(categoryId, type) ?: return null
        val age = System.currentTimeMillis() - row.createdAt
        return if (age in 0..ttlMs) row.content else null
    }

    override suspend fun getAny(categoryId: Int, type: String): CachedEntry? {
        val row = dao.get(categoryId, type) ?: return null
        val age = (System.currentTimeMillis() - row.createdAt).coerceAtLeast(0L)
        return CachedEntry(content = row.content, ageMs = age)
    }

    override suspend fun save(categoryId: Int, type: String, content: String) {
        dao.upsert(
            AiCacheEntity(
                categoryId = categoryId,
                type = type,
                content = content,
                createdAt = System.currentTimeMillis()
            )
        )
    }

    override suspend fun clear(categoryId: Int, type: String) {
        dao.delete(categoryId, type)
    }
}
