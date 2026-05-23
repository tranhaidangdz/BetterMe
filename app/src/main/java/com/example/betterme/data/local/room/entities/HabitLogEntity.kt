package com.example.betterme.data.local.room.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "habit_logs",
    foreignKeys = [
        ForeignKey(
            entity = HabitEntity::class,
            parentColumns = ["id"],
            childColumns = ["habit_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("habit_id")]
)
data class HabitLogEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val habit_id: Int,
    val date: Long,
    val status: String,
    val note: String?,
    val image: String?,
    val created_at: Long,
    val latitude: Double? = null,
    val longitude: Double? = null,
    /**
     * Wall-clock of last local mutation. Drives last-write-wins reconciliation
     * against `users/{uid}/habit_logs/{habitId}_{date}`.
     */
    val updated_at: Long = System.currentTimeMillis(),
    /** Last successful push timestamp. Dirty when synced_at < updated_at. */
    val synced_at: Long? = null,
    /** Soft-delete marker for two-way sync. */
    val is_deleted: Boolean = false
)
