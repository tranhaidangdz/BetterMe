package com.example.betterme.data.local.room.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Per-user syncable settings row.
 *
 * Replaces the previous in-memory `UserLifestyleProfile.Default` + the device-local
 * DataStore onboarding flags as the source of truth for things the user should see
 * the same way on every device they sign in on. Created on first read with all
 * fields at their default; mutations are routed through the repository so
 * `updated_at` always bumps cleanly.
 *
 * The lifestyle fields mirror [com.example.betterme.domain.ai.schedule.UserLifestyleProfile]
 * field-for-field so the AI engines can keep consuming a single domain shape.
 *
 * `has_selected_habits` and `is_first_time` move out of DataStore so onboarding
 * state survives reinstalls (the moment the user signs back in, the sync layer
 * restores their flags and the app correctly skips the onboarding flow).
 */
@Entity(tableName = "user_settings")
data class UserSettingsEntity(
    @PrimaryKey
    val user_id: String,

    // Lifestyle profile — mirrors UserLifestyleProfile.Default values.
    val sleep_start: String = "23:00",
    val sleep_end: String = "07:00",
    val sleep_duration_target_hours: Int = 8,
    val work_start: String = "08:30",
    val work_end: String = "17:30",
    val breakfast: String = "07:30",
    val lunch: String = "12:00",
    val dinner: String = "18:30",
    val activity_level: String = "MODERATE", // EASY | MODERATE | INTENSE

    // Onboarding / first-time flags. Cross-device meaningful so they move here from
    // device-local DataStore.
    val has_selected_habits: Boolean = false,
    val is_first_time: Boolean = true,

    val created_at: Long = System.currentTimeMillis(),
    /** Wall-clock of last local mutation; drives last-write-wins. */
    val updated_at: Long = System.currentTimeMillis(),
    /** Last successful push timestamp. Dirty when synced_at < updated_at. */
    val synced_at: Long? = null
)
