package com.example.betterme.domain.repository

import com.example.betterme.data.local.room.entities.ReminderEntity
import kotlinx.coroutines.flow.Flow

interface ReminderRepository {

    // Habit-specific helpers (kept for backwards compat with existing habit code)
    fun getReminders(habitId: Int): Flow<List<ReminderEntity>>

    suspend fun addReminder(reminder: ReminderEntity): Long

    suspend fun updateReminder(reminder: ReminderEntity)

    suspend fun deleteReminder(reminder: ReminderEntity)

    suspend fun toggleReminder(id: Int, isActive: Boolean)

    // Polymorphic API
    fun observeForTarget(type: String, id: Int): Flow<List<ReminderEntity>>

    suspend fun getActiveByTarget(type: String, id: Int): ReminderEntity?

    suspend fun deleteByTarget(type: String, id: Int)

    suspend fun setWorkId(id: Int, workId: String?)
}
