package com.example.betterme.domain.repository

import com.example.betterme.data.local.room.entities.UserSettingsEntity
import kotlinx.coroutines.flow.Flow

/**
 * Per-user syncable settings: lifestyle profile + onboarding flags. Created on first
 * access via [getOrCreate]. All mutations route through this repo so `updated_at`
 * stamps consistently and the sync layer can pick up dirty rows.
 */
interface UserSettingsRepository {

    /** Returns the row for the user, creating it with defaults if absent. */
    suspend fun getOrCreate(userId: String): UserSettingsEntity

    fun observe(userId: String): Flow<UserSettingsEntity?>

    /**
     * Overwrites the lifestyle fields and bumps `updated_at`. Other fields
     * (onboarding flags) stay untouched.
     */
    suspend fun updateLifestyle(
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
    )

    /** Marks onboarding-flow completion. Synced so the user skips onboarding on a new device. */
    suspend fun markHabitsSelected(userId: String)

    /** Marks first-time complete. */
    suspend fun markNotFirstTime(userId: String)
}
