package com.example.betterme.presentation.challenge.overview

import com.example.betterme.base.MviIntent
import com.example.betterme.base.MviSingleEvent
import com.example.betterme.base.MviViewState
import com.example.betterme.presentation.challenge.model.ChallengeProgressUiModel
import com.example.betterme.presentation.challenge.model.CompletedChallengeUiModel
import com.example.betterme.presentation.challenge.model.OverviewFilter
import com.example.betterme.presentation.challenge.model.OverviewStatsUi
import com.example.betterme.presentation.challenge.model.UpcomingChallengeUiModel

data class ChallengeOverviewState(
    val isLoading: Boolean = true,
    val stats: OverviewStatsUi = OverviewStatsUi(0, 0, 0),
    val selectedFilter: OverviewFilter = OverviewFilter.Active,
    val active: List<ChallengeProgressUiModel> = emptyList(),
    val upcoming: List<UpcomingChallengeUiModel> = emptyList(),
    val completed: List<CompletedChallengeUiModel> = emptyList(),
    val errorMessage: String? = null
) : MviViewState

sealed class ChallengeOverviewIntent : MviIntent {
    data object Load : ChallengeOverviewIntent()
    data class SelectFilter(val filter: OverviewFilter) : ChallengeOverviewIntent()
    data class ToggleStartReminder(val challengeId: Int) : ChallengeOverviewIntent()
}

sealed class ChallengeOverviewEvent : MviSingleEvent {
    data class ShowMessage(val text: String) : ChallengeOverviewEvent()
}
