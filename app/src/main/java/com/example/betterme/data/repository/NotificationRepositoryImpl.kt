package com.example.betterme.data.repository

import com.example.betterme.data.local.room.dao.NotificationDao
import com.example.betterme.data.local.room.entities.NotificationEntity
import com.example.betterme.domain.repository.NotificationRepository

class NotificationRepositoryImpl(
    private val dao: NotificationDao
) : NotificationRepository {

    override fun observeForUser(userId: String) = dao.observeForUser(userId)

    override fun observeUnreadCount(userId: String) = dao.observeUnreadCount(userId)

    override suspend fun insert(notification: NotificationEntity) = dao.insert(notification)

    override suspend fun markAsRead(id: Int) = dao.markAsRead(id)

    override suspend fun markAllReadForUser(userId: String) = dao.markAllReadForUser(userId)

    override suspend fun deleteOlderThan(olderThanMillis: Long) =
        dao.deleteOlderThan(olderThanMillis)

    override suspend fun deleteAllForUser(userId: String) = dao.deleteAllForUser(userId)
}
