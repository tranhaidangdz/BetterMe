package com.example.betterme.data.repository

import com.example.betterme.data.local.room.dao.HabitDao
import com.example.betterme.data.local.room.entities.HabitEntity
import com.example.betterme.domain.repository.HabitRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class HabitRepositoryImpl(
    private val dao: HabitDao,
    private val nowProvider: () -> Long = { System.currentTimeMillis() }
) : HabitRepository {

    override fun getHabits(userId: String) = dao.getHabitsByUser(userId)

    override fun getActiveHabits(userId: String): Flow<List<HabitEntity>> =
        dao.getHabitsByUser(userId).map { habits ->
            val now = nowProvider()
            habits.filter { h ->
                !h.is_deleted && (h.end_date == null || h.end_date >= now)
            }
        }

    override suspend fun getHabitById(id: Int) = dao.getHabitById(id)

    override suspend fun addHabit(habit: HabitEntity) =
        dao.insertHabit(habit)

    override suspend fun updateHabit(habit: HabitEntity) =
        dao.updateHabit(habit)

    override suspend fun deleteHabit(habit: HabitEntity) =
        dao.deleteHabit(habit)

    override suspend fun deleteHabitById(id: Int) =
        dao.deleteHabitById(id)

    override fun getHabitsByCategoryForUser(categoryId: Int, userId: String) =
        dao.getHabitsByCategoryForUser(categoryId, userId)

    override suspend fun getHabitCountByCategoryForUser(categoryId: Int, userId: String) =
        dao.getHabitCountByCategoryForUser(categoryId, userId)

    override suspend fun deleteAllByUserId(userId: String) =
        dao.deleteAllByUserId(userId)
}