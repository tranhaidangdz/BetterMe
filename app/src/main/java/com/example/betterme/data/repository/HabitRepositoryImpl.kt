package com.example.betterme.data.repository

import com.example.betterme.data.local.room.dao.HabitDao
import com.example.betterme.data.local.room.entities.HabitEntity
import com.example.betterme.domain.repository.HabitRepository

class HabitRepositoryImpl(
    private val dao: HabitDao
) : HabitRepository {

    override fun getHabits(userId: Int) = dao.getHabitsByUser(userId)

    override suspend fun getHabitById(id: Int) = dao.getHabitById(id)

    override suspend fun addHabit(habit: HabitEntity) =
        dao.insertHabit(habit)

    override suspend fun updateHabit(habit: HabitEntity) =
        dao.updateHabit(habit)

    override suspend fun deleteHabit(habit: HabitEntity) =
        dao.deleteHabit(habit)

    override suspend fun deleteHabitById(id: Int) =
        dao.deleteHabitById(id)
}