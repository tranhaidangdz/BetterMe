package com.example.betterme.domain.repository

import com.example.betterme.data.local.room.entities.HabitLogEntity
import kotlinx.coroutines.flow.Flow

interface HabitLogRepository {

    fun getLogs(habitId: Int): Flow<List<HabitLogEntity>>

    suspend fun getLogByDate(habitId: Int, date: Long): HabitLogEntity?

    suspend fun addLog(log: HabitLogEntity)

    suspend fun updateLog(log: HabitLogEntity)

    suspend fun deleteLog(log: HabitLogEntity)

    suspend fun countCompleted(habitId: Int): Int

    suspend fun getCompletedHabitIdsByDate(dateMillis: Long): List<Int>
}