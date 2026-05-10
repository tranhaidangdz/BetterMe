package com.example.betterme.presentation.challenge.achievements

import com.example.betterme.base.MviIntent
import com.example.betterme.base.MviSingleEvent
import com.example.betterme.base.MviViewState
import com.example.betterme.presentation.challenge.model.AchievementHighlightUi
import com.example.betterme.presentation.challenge.model.AchievementsHeaderUi
import com.example.betterme.presentation.challenge.model.BadgeUiModel

data class ChallengeAchievementsState(
    val isLoading: Boolean = true,
    val header: AchievementsHeaderUi? = null,
    val featuredBadges: List<BadgeUiModel> = emptyList(),
    val highlights: List<AchievementHighlightUi> = emptyList()
) : MviViewState

sealed class ChallengeAchievementsIntent : MviIntent {
    data object Load : ChallengeAchievementsIntent()
}

sealed class ChallengeAchievementsEvent : MviSingleEvent
