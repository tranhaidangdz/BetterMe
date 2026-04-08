package com.example.betterme.data.local.room.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "user_achievements",
    indices = [Index("user_id"), Index("achievement_id")]
)
data class UserAchievementEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val achievement_id: Int,
    val user_id: Int,
    val achieved_at: Long
)
