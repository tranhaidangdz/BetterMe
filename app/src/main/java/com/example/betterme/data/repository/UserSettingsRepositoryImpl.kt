package com.example.betterme.data.repository

import com.example.betterme.data.local.room.dao.UserSettingsDao
import com.example.betterme.data.local.room.entities.UserSettingsEntity
import com.example.betterme.domain.repository.UserSettingsRepository

class UserSettingsRepositoryImpl(
    private val dao: UserSettingsDao
) : UserSettingsRepository {

    override suspend fun getOrCreate(userId: String): UserSettingsEntity {
        dao.get(userId)?.let { return it }
        val now = System.currentTimeMillis()
        val row = UserSettingsEntity(
            user_id = userId,
            created_at = now,
            updated_at = now,
            synced_at = null
        )
        dao.upsert(row)
        return row
    }

    override fun observe(userId: String) = dao.observe(userId)

    override suspend fun updateLifestyle(
        userId: String,
        sleepStart: String,
        sleepEnd: String,
        sleepDurationTargetHours: Int,
        workStart: String,
        workEnd: String,
        breakfast: String,
        lunch: String,
        dinner: String,
        activityLevel: String
    ) {
        val now = System.currentTimeMillis()
        val current = getOrCreate(userId)
        dao.upsert(
            current.copy(
                sleep_start = sleepStart,
                sleep_end = sleepEnd,
                sleep_duration_target_hours = sleepDurationTargetHours,
                work_start = workStart,
                work_end = workEnd,
                breakfast = breakfast,
                lunch = lunch,
                dinner = dinner,
                activity_level = activityLevel,
                updated_at = now,
                synced_at = null
            )
        )
    }

    override suspend fun markHabitsSelected(userId: String) {
        val now = System.currentTimeMillis()
        val current = getOrCreate(userId)
        dao.upsert(current.copy(has_selected_habits = true, updated_at = now, synced_at = null))
    }

    override suspend fun markNotFirstTime(userId: String) {
        val now = System.currentTimeMillis()
        val current = getOrCreate(userId)
        dao.upsert(current.copy(is_first_time = false, updated_at = now, synced_at = null))
    }
}
