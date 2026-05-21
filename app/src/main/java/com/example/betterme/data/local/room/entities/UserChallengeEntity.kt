package com.example.betterme.data.local.room.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "user_challenges",
    foreignKeys = [
        ForeignKey(
            entity = ChallengeEntity::class,
            parentColumns = ["id"],
            childColumns = ["challenge_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("user_id"),
        Index("challenge_id"),
        Index("status"),
        Index("team_id"),
        Index(value = ["user_id", "challenge_id"], unique = true)
    ]
)
data class UserChallengeEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val user_id: String,
    val challenge_id: Int,
    /** One of [com.example.betterme.domain.challenge.UserChallengeStatus]. */
    val status: String = "ACTIVE",
    /** Day-precise start (startOfDay millis) — set at join time. */
    val start_date: Long,
    /**
     * Day-precise deadline (startOfDay millis) — `start_date + (duration_days - 1) * DAY_MS`.
     * Strict-daily validation requires a DONE log for every calendar day in [start_date, target_end_date].
     * Nullable only for legacy rows joined before v12; the migration backfills these.
     */
    val target_end_date: Long? = null,
    /** Terminal timestamp set on COMPLETED / FAILED / ABANDONED. Null while ACTIVE. */
    val end_date: Long? = null,
    val current_streak: Int = 0,
    val best_streak: Int = 0,
    val last_check_in_date: Long? = null,
    val progress_pct: Int = 0,
    val team_id: Int? = null,
    val joined_at: Long = System.currentTimeMillis()
)
