package com.example.betterme.domain.repository

import com.example.betterme.data.local.room.entities.ReminderEntity
import kotlinx.coroutines.flow.Flow

interface ReminderRepository {

    fun getReminders(habitId: Int): Flow<List<ReminderEntity>>

    suspend fun addReminder(reminder: ReminderEntity)

    suspend fun updateReminder(reminder: ReminderEntity)

    suspend fun deleteReminder(reminder: ReminderEntity)

    suspend fun toggleReminder(id: Int, isActive: Boolean)
}