package com.example.betterme.data.repository

import com.example.betterme.data.local.room.dao.HabitLogDao
import com.example.betterme.data.local.room.entities.HabitLogEntity
import com.example.betterme.domain.repository.HabitLogRepository

class HabitLogRepositoryImpl(
    private val dao: HabitLogDao
) : HabitLogRepository {

    override fun getLogs(habitId: Int) =
        dao.getLogsByHabit(habitId)

    override suspend fun getLogByDate(habitId: Int, date: Long) =
        dao.getLogByDate(habitId, date)

    override suspend fun addLog(log: HabitLogEntity) =
        dao.insertLog(log)

    override suspend fun updateLog(log: HabitLogEntity) =
        dao.updateLog(log)

    override suspend fun deleteLog(log: HabitLogEntity) =
        dao.deleteLog(log)

    override suspend fun countCompleted(habitId: Int) =
        dao.countCompleted(habitId)

    override suspend fun getCompletedHabitIdsByDate(dateMillis: Long) =
        dao.getCompletedHabitIdsByDate(dateMillis)
}