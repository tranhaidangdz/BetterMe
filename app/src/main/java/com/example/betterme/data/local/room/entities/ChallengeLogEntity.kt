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
    val longitude: Double? = null
)
