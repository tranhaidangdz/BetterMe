package com.example.betterme.domain.repository

import com.example.betterme.data.local.room.entities.HabitEntity
import kotlinx.coroutines.flow.Flow

interface HabitRepository {

    fun getHabits(userId: String): Flow<List<HabitEntity>>

    suspend fun getHabitById(id: Int): HabitEntity?

    suspend fun addHabit(habit: HabitEntity): Long

    suspend fun updateHabit(habit: HabitEntity)

    suspend fun deleteHabit(habit: HabitEntity)

    suspend fun deleteHabitById(id: Int)

    fun getHabitsByCategoryForUser(categoryId: Int, userId: String): Flow<List<HabitEntity>>

    suspend fun getHabitCountByCategoryForUser(categoryId: Int, userId: String): Int

    suspend fun deleteAllByUserId(userId: String)
}