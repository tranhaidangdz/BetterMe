package com.example.betterme.presentation.challenge.badges

import com.example.betterme.base.MviIntent
import com.example.betterme.base.MviSingleEvent
import com.example.betterme.base.MviViewState
import com.example.betterme.presentation.challenge.model.BadgeUiModel

data class BadgeSectionUi(
    val title: String,
    val badges: List<BadgeUiModel>
)

data class ChallengeBadgesState(
    val isLoading: Boolean = true,
    val sections: List<BadgeSectionUi> = emptyList()
) : MviViewState

sealed class ChallengeBadgesIntent : MviIntent {
    data object Load : ChallengeBadgesIntent()
}

sealed class ChallengeBadgesEvent : MviSingleEvent
