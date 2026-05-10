package com.example.betterme.data.local.room.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "challenges",
    foreignKeys = [
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["category_id"],
            onDelete = ForeignKey.SET_NULL
        ),
        ForeignKey(
            entity = AchievementEntity::class,
            parentColumns = ["id"],
            childColumns = ["reward_badge_id"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index("category_id"),
        Index("reward_badge_id"),
        Index("is_featured"),
        Index("is_group")
    ]
)
data class ChallengeEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val title: String,
    val description: String,
    val short_description: String = "",
    val category_id: Int? = null,
    val difficulty: String = "EASY",                // "EASY" | "MEDIUM" | "HARD"
    val duration_days: Int = 7,
    val target_streak: Int = 7,
    val reward_coins: Int = 0,
    val reward_badge_id: Int? = null,
    val icon_emoji: String = "🏆",
    val image_asset: String? = null,
    val color_hex: String = "#0077FF",
    val is_group: Boolean = false,
    val is_featured: Boolean = false,
    val participant_count: Int = 0,
    val start_date: Long? = null,
    val end_date: Long? = null,
    val sort_order: Int = 0,
    val created_at: Long = System.currentTimeMillis()
)
