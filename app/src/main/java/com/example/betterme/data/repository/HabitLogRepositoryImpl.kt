package com.example.betterme.data.repository

import com.example.betterme.data.local.room.dao.HabitLogDao
import com.example.betterme.data.local.room.entities.HabitLogEntity
import com.example.betterme.domain.repository.HabitLogRepository

class HabitLogRepositoryImpl(
    private val dao: HabitLogDao
) : HabitLogRepository {

    override fun getLogs(habitId: Int) =
        dao.getLogsByHabit(habitId)

    override fun observeAllLogs() = dao.observeAllLogs()

    override suspend fun getLogByDate(habitId: Int, date: Long) =
        dao.getLogByDate(habitId, date)

    override suspend fun addLog(log: HabitLogEntity) =
        dao.insertLog(log)

    // Every UPDATE through the repository becomes a sync candidate.
    override suspend fun updateLog(log: HabitLogEntity) =
        dao.updateLog(
            log.copy(updated_at = System.currentTimeMillis(), synced_at = null)
        )

    // Soft-delete so the removal propagates to other devices via sync.
    // Hard delete (the previous behaviour) would lose the deletion event
    // because the row disappears from Room before the next sync upload.
    override suspend fun deleteLog(log: HabitLogEntity) =
        dao.softDeleteLog(log.id, System.currentTimeMillis())

    override suspend fun countCompleted(habitId: Int) =
        dao.countCompleted(habitId)

    override suspend fun getCompletedHabitIdsByDate(dateMillis: Long) =
        dao.getCompletedHabitIdsByDate(dateMillis)
}