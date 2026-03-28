package com.example.betterme.data.repository

import com.example.betterme.data.local.room.dao.ReminderDao
import com.example.betterme.data.local.room.entities.ReminderEntity
import com.example.betterme.domain.repository.ReminderRepository

class ReminderRepositoryImpl(
    private val dao: ReminderDao
) : ReminderRepository {

    override fun getReminders(habitId: Int) =
        dao.getRemindersByHabit(habitId)

    override suspend fun addReminder(reminder: ReminderEntity) =
        dao.insertReminder(reminder)

    override suspend fun updateReminder(reminder: ReminderEntity) =
        dao.updateReminder(reminder)

    override suspend fun deleteReminder(reminder: ReminderEntity) =
        dao.deleteReminder(reminder)

    override suspend fun toggleReminder(id: Int, isActive: Boolean) =
        dao.toggleReminder(id, isActive)
}