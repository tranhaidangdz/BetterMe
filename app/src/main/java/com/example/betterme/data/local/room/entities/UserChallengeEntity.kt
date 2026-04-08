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
    indices = [Index("user_id"), Index("challenge_id")]
)
data class UserChallengeEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val user_id: String,
    val challenge_id: Int,
    val progress: Int,
    val start_date: Long,
    val status: String
)