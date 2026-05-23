package com.example.betterme.data.local.room.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "challenge_logs",
    foreignKeys = [
        ForeignKey(
            entity = UserChallengeEntity::class,
            parentColumns = ["id"],
            childColumns = ["user_challenge_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("user_challenge_id"),
        Index(value = ["user_challenge_id", "date"])
    ]
)
data class ChallengeLogEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val user_challenge_id: Int,
    val date: Long,                                 // startOfDay millis
    val status: String = "DONE",                    // "DONE" | "SKIPPED" | "MISSED"
    val note: String? = null,
    val image: String? = null,
    val created_at: Long = System.currentTimeMillis(),
    val latitude: Double? = null,
    val longitude: Double? = null,
    /**
     * Wall-clock of the last local mutation. Drives last-write-wins reconciliation
     * against `users/{uid}/challenge_logs/{challengeId}_{date}` — whichever side
     * has the larger `updated_at` wins.
     */
    val updated_at: Long = System.currentTimeMillis(),
    /** Last successful push timestamp. Row is dirty when synced_at < updated_at. */
    val synced_at: Long? = null,
    /**
     * Soft-delete marker for two-way sync. Hard deletes would race with the upload
     * loop and disappear before propagation; this flag preserves the audit trail.
     */
    val is_deleted: Boolean = false
)
