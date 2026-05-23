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

    // ============================================================
    // Sync helpers (offline-first)
    // ============================================================

    /**
     * Soft-delete every row in the user's history. Used instead of hard delete so
     * the deletion can propagate to other devices via sync.
     */
    @Query("""
        UPDATE ai_chat
        SET is_deleted = 1,
            updated_at = :updatedAt,
            synced_at = NULL
        WHERE user_id = :userId
    """)
    suspend fun softClearHistory(userId: String, updatedAt: Long)

    @Query("""
        SELECT * FROM ai_chat
        WHERE user_id = :userId
          AND (synced_at IS NULL OR synced_at < updated_at)
    """)
    suspend fun getDirtyChats(userId: String): List<AIChatEntity>

    @Query("""
        SELECT COUNT(*) FROM ai_chat
        WHERE user_id = :userId
          AND (synced_at IS NULL OR synced_at < updated_at)
    """)
    suspend fun countDirtyChats(userId: String): Int

    @Query("UPDATE ai_chat SET synced_at = :syncedAt WHERE id = :id AND updated_at = :pushedUpdatedAt")
    suspend fun markChatSynced(id: Int, pushedUpdatedAt: Long, syncedAt: Long)
}