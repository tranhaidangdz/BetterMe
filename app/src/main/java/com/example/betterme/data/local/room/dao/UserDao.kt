package com.example.betterme.data.local.room.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.betterme.data.local.room.entities.UserEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserEntity)

    @Update
    suspend fun updateUser(user: UserEntity)

    @Delete
    suspend fun deleteUser(user: UserEntity)

    @Query("SELECT * FROM users WHERE id = :userId")
    suspend fun getUserById(userId: String): UserEntity?

    @Query("SELECT * FROM users WHERE id = :userId")
    fun observeUser(userId: String): Flow<UserEntity?>

    @Query("""
        UPDATE users
        SET coins = coins + :delta,
            xp = xp + :delta,
            updated_at = :updatedAt,
            synced_at = NULL
        WHERE id = :userId
    """)
    suspend fun addCoins(userId: String, delta: Int, updatedAt: Long)

    @Query("""
        UPDATE users
        SET level = :level,
            updated_at = :updatedAt,
            synced_at = NULL
        WHERE id = :userId
    """)
    suspend fun setLevel(userId: String, level: Int, updatedAt: Long)

    // ============================================================
    // Sync helpers (offline-first)
    // ============================================================

    /**
     * Row dirty when `synced_at < updated_at` or `synced_at IS NULL`. Pulled by
     * the synchronizer to decide what to push to Firestore on the next pass.
     */
    @Query("""
        SELECT * FROM users
        WHERE id = :userId AND (synced_at IS NULL OR synced_at < updated_at)
    """)
    suspend fun getDirtyUser(userId: String): UserEntity?

    @Query("SELECT COUNT(*) FROM users WHERE id = :userId AND (synced_at IS NULL OR synced_at < updated_at)")
    suspend fun countDirtyUsers(userId: String): Int

    /**
     * Stamps the row as successfully pushed to Firestore. Called by the synchronizer
     * after `set(...)` returns. Uses == on updated_at so a concurrent local edit that
     * landed *after* the upload won't be mis-stamped as synced.
     */
    @Query("UPDATE users SET synced_at = :syncedAt WHERE id = :userId AND updated_at = :pushedUpdatedAt")
    suspend fun markUserSynced(userId: String, pushedUpdatedAt: Long, syncedAt: Long)
}
