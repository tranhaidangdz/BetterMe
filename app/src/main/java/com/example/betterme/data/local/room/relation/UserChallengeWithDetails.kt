package com.example.betterme.data.local.room.relation

import androidx.room.Embedded
import androidx.room.Relation
import com.example.betterme.data.local.room.entities.ChallengeEntity
import com.example.betterme.data.local.room.entities.GroupTeamEntity
import com.example.betterme.data.local.room.entities.UserChallengeEntity

data class UserChallengeWithDetails(
    @Embedded
    val userChallenge: UserChallengeEntity,

    @Relation(
        parentColumn = "challenge_id",
        entityColumn = "id"
    )
    val challenge: ChallengeEntity,

    @Relation(
        parentColumn = "team_id",
        entityColumn = "id"
    )
    val team: GroupTeamEntity? = null
)
