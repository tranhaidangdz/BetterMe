package com.example.betterme.data.local.room.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "reminders",
    indices = [
        Index(value = ["target_type", "target_id"])
    ]
)
data class ReminderEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val target_type: String,                        // "HABIT" | "USER_CHALLENGE" | "CHALLENGE_START"
    val target_id: Int,
    val time: String,                               // "HH:mm" daily, OR ISO date for one-shot
    val is_active: Boolean = true,
    val work_id: String? = null,                    // WorkManager request UUID for cancellation
    val created_at: Long = System.currentTimeMillis()
)
