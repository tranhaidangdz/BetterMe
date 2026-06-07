package com.example.betterme.data.local.room.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "user_achievements",
    indices = [
        Index("user_id"),
        Index("achievement_id"),
        Index(value = ["user_id", "achievement_id"], unique = true)
    ]
)
data class UserAchievementEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val achievement_id: Int,
    val user_id: String,
    val achieved_at: Long,
    val source_user_challenge_id: Int? = null,
    /**
     * Wall-clock of the last local mutation. Drives last-write-wins reconciliation
     * against `users/{uid}/badges/{achievement_id}`. Stamped on every insert; soft
     * deletes bump it so the remote sees the removal.
     */
    val updated_at: Long = achieved_at,
    /** Last successful push timestamp. Dirty when `synced_at < updated_at` OR null. */
    val synced_at: Long? = null,
    /**
     * Soft-delete marker. Hard-deleting a badge would race with upload; the sync
     * pass writes `is_deleted=true` to Firestore so other devices can converge.
     */
    val is_deleted: Boolean = false
)
