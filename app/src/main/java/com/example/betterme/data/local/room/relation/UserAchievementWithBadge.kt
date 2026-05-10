package com.example.betterme.data.local.room.relation

import androidx.room.Embedded
import androidx.room.Relation
import com.example.betterme.data.local.room.entities.AchievementEntity
import com.example.betterme.data.local.room.entities.UserAchievementEntity

data class UserAchievementWithBadge(
    @Embedded
    val userAchievement: UserAchievementEntity,

    @Relation(
        parentColumn = "achievement_id",
        entityColumn = "id"
    )
    val badge: AchievementEntity
)
