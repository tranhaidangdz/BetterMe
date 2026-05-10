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
    val source_user_challenge_id: Int? = null
)
