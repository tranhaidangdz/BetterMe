package com.example.betterme.data.local.room.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.betterme.data.local.room.entities.NotificationEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface NotificationDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(notification: NotificationEntity): Long

    @Query("SELECT * FROM notifications WHERE user_id = :userId ORDER BY created_at DESC")
    fun observeForUser(userId: String): Flow<List<NotificationEntity>>

    @Query("SELECT COUNT(*) FROM notifications WHERE user_id = :userId AND is_read = 0")
    fun observeUnreadCount(userId: String): Flow<Int>

    @Query("UPDATE notifications SET is_read = 1 WHERE id = :id")
    suspend fun markAsRead(id: Int)

    @Query("UPDATE notifications SET is_read = 1 WHERE user_id = :userId")
    suspend fun markAllReadForUser(userId: String)

    @Query("DELETE FROM notifications WHERE created_at < :olderThanMillis")
    suspend fun deleteOlderThan(olderThanMillis: Long): Int

    @Query("DELETE FROM notifications WHERE user_id = :userId")
    suspend fun deleteAllForUser(userId: String)

    @Query("DELETE FROM notifications WHERE id = :id")
    suspend fun deleteById(id: Int)
}
