package com.example.betterme.data.local.room.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * In-app notification record. Inserted whenever the system fires a push notification
 * (challenge reminder, challenge-start reminder, achievement award, etc.) so the user
 * has a persistent inbox accessible from the Home top-bar bell icon.
 *
 * The Home notification center reads from this table; the system tray push is fire-and-
 * forget. Cleanup worker runs daily at 00:00 to drop rows older than 24h so the inbox
 * stays focused on "what mattered today".
 */
@Entity(
    tableName = "notifications",
    indices = [
        Index("user_id"),
        Index(value = ["user_id", "is_read"]),
        Index("created_at")
    ]
)
data class NotificationEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val user_id: String,
    val title: String,
    val message: String,
    val type: String,                       // "CHALLENGE_REMINDER" | "CHALLENGE_START" | "BADGE_AWARDED" | "GENERIC"
    val challenge_id: Int? = null,          // for deep-linking to the challenge detail
    val user_challenge_id: Int? = null,     // for active-mode detail navigation
    val reminder_time_label: String? = null,
    val is_read: Boolean = false,
    val created_at: Long = System.currentTimeMillis()
)
