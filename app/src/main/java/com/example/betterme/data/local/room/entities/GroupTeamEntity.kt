package com.example.betterme.data.local.room.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "group_teams",
    foreignKeys = [
        ForeignKey(
            entity = ChallengeEntity::class,
            parentColumns = ["id"],
            childColumns = ["challenge_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("challenge_id")]
)
data class GroupTeamEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val challenge_id: Int,
    val name: String,
    val icon_emoji: String = "👥",
    val color_hex: String = "#0077FF",
    val member_count: Int = 0,
    val total_coins: Int = 0,
    val rank: Int = 0
)
