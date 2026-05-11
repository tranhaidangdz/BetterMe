package com.example.betterme.domain.repository

import com.example.betterme.data.local.room.entities.NotificationEntity
import kotlinx.coroutines.flow.Flow

interface NotificationRepository {

    fun observeForUser(userId: String): Flow<List<NotificationEntity>>

    fun observeUnreadCount(userId: String): Flow<Int>

    suspend fun insert(notification: NotificationEntity): Long

    suspend fun markAsRead(id: Int)

    suspend fun markAllReadForUser(userId: String)

    /** Delete notifications created before [olderThanMillis]. Returns rows deleted. */
    suspend fun deleteOlderThan(olderThanMillis: Long): Int

    suspend fun deleteAllForUser(userId: String)

    /** Remove a single notification by id — used by swipe-to-delete in the inbox. */
    suspend fun deleteById(id: Int)
}
