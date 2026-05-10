package com.example.betterme.data.local.room.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "achievements",
    indices = [
        Index("category"),
        Index("criteria_challenge_id")
    ]
)
data class AchievementEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val title: String,
    val description: String,
    val icon: Int = 0,                              // legacy drawable res id
    val category: String = "BASIC",                 // "BASIC" | "HEALTH" | "LEARNING" | "SPECIAL"
    val icon_emoji: String = "🏅",
    /**
     * Optional Cloudinary (or any HTTPS) URL pointing to the badge artwork. When set, UI
     * layers (BadgeGridItem, RewardRow) prefer this over [icon] / [icon_emoji] and load
     * via Coil. Null means "no network artwork — fall back to drawable / emoji".
     */
    val image_url: String? = null,
    val color_hex: String = "#0077FF",
    val criteria_type: String = "MANUAL",           // "CHALLENGE_COMPLETED" | "STREAK" | "TOTAL_CHECKINS" | "COINS" | "MANUAL"
    val criteria_value: Int = 0,
    val criteria_challenge_id: Int? = null,
    val sort_order: Int = 0
)
