package com.example.betterme.data.local.room.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.betterme.data.local.room.entities.AiCacheEntity

/**
 * Two-method DAO — caching is intentionally simple. The repository handles TTL
 * checks at read time (so the row can be inspected for debugging even after it
 * expires) rather than DELETE-on-expiry, which would require a background sweeper.
 */
@Dao
interface AiCacheDao {

    @Query("SELECT * FROM ai_cache WHERE categoryId = :categoryId AND type = :type LIMIT 1")
    suspend fun get(categoryId: Int, type: String): AiCacheEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: AiCacheEntity)

    @Query("DELETE FROM ai_cache WHERE categoryId = :categoryId AND type = :type")
    suspend fun delete(categoryId: Int, type: String)
}
