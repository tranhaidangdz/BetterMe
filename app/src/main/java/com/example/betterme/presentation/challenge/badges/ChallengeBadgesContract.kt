package com.example.betterme.presentation.challenge.badges

import com.example.betterme.base.MviIntent
import com.example.betterme.base.MviSingleEvent
import com.example.betterme.base.MviViewState
import com.example.betterme.presentation.challenge.model.BadgeUiModel

data class BadgeSectionUi(
    val title: String,
    val badges: List<BadgeUiModel>
)

enum class BadgeStatusFilter { All, Earned, Locked }

data class ChallengeBadgesState(
    val isLoading: Boolean = true,
    val sections: List<BadgeSectionUi> = emptyList(),
    val filter: BadgeStatusFilter = BadgeStatusFilter.All,
    val totalBadges: Int = 0,
    val earnedBadges: Int = 0
) : MviViewState

sealed class ChallengeBadgesIntent : MviIntent {
    data object Load : ChallengeBadgesIntent()
    data class SetFilter(val filter: BadgeStatusFilter) : ChallengeBadgesIntent()
}

sealed class ChallengeBadgesEvent : MviSingleEvent
