package com.example.betterme.data.local.room.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.betterme.data.local.room.entities.UserSettingsEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UserSettingsDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(settings: UserSettingsEntity)

    @Query("SELECT * FROM user_settings WHERE user_id = :userId")
    suspend fun get(userId: String): UserSettingsEntity?

    @Query("SELECT * FROM user_settings WHERE user_id = :userId")
    fun observe(userId: String): Flow<UserSettingsEntity?>

    // ============================================================
    // Sync helpers (offline-first)
    // ============================================================

    @Query("""
        SELECT * FROM user_settings
        WHERE user_id = :userId AND (synced_at IS NULL OR synced_at < updated_at)
    """)
    suspend fun getDirty(userId: String): UserSettingsEntity?

    @Query("""
        SELECT COUNT(*) FROM user_settings
        WHERE user_id = :userId AND (synced_at IS NULL OR synced_at < updated_at)
    """)
    suspend fun countDirty(userId: String): Int

    @Query("""
        UPDATE user_settings
        SET synced_at = :syncedAt
        WHERE user_id = :userId AND updated_at = :pushedUpdatedAt
    """)
    suspend fun markSynced(userId: String, pushedUpdatedAt: Long, syncedAt: Long)
}
