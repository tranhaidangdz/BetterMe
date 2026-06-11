package com.example.betterme.data.repository

import com.example.betterme.data.local.room.dao.HabitDao
import com.example.betterme.data.local.room.entities.HabitEntity
import com.example.betterme.domain.repository.HabitRepository

class HabitRepositoryImpl(
    private val dao: HabitDao
) : HabitRepository {

    override fun getHabits(userId: String) = dao.getHabitsByUser(userId)

    override suspend fun getHabitById(id: Int) = dao.getHabitById(id)

    override suspend fun addHabit(habit: HabitEntity) =
        dao.insertHabit(habit)

    // Every UPDATE through the repository becomes a sync candidate. We bump
    // updated_at and clear synced_at here so the next SyncCoordinator pass
    // picks the row up — callers no longer have to remember the .copy() dance.
    override suspend fun updateHabit(habit: HabitEntity) =
        dao.updateHabit(
            habit.copy(updated_at = System.currentTimeMillis(), synced_at = null)
        )

    // Soft-delete so the removal propagates to other devices via sync. The
    // DAO's softDeleteHabit sets is_deleted=1, bumps updated_at, clears
    // synced_at — exactly what the next push pass needs.
    override suspend fun deleteHabit(habit: HabitEntity) =
        dao.softDeleteHabit(habit.id, System.currentTimeMillis())

    override suspend fun deleteHabitById(id: Int) =
        dao.softDeleteHabit(id, System.currentTimeMillis())

    override fun getHabitsByCategoryForUser(categoryId: Int, userId: String) =
        dao.getHabitsByCategoryForUser(categoryId, userId)

    override suspend fun getHabitCountByCategoryForUser(categoryId: Int, userId: String) =
        dao.getHabitCountByCategoryForUser(categoryId, userId)

    override suspend fun deleteAllByUserId(userId: String) =
        dao.deleteAllByUserId(userId)
}