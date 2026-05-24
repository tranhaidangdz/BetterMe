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
    /**
     * User tapped "Chia sẻ tiến độ". VM builds the rich text payload via the use
     * case, then emits [ChallengeOverviewEvent.LaunchShareSheet]; the screen
     * dispatches the OS share intent.
     */
    data object ShareProgress : ChallengeOverviewIntent()
}

sealed class ChallengeOverviewEvent : MviSingleEvent {
    data class ShowMessage(val text: String) : ChallengeOverviewEvent()
    /**
     * Open the native share sheet. [imageSources] carries URLs/local paths for
     * check-in photos across all the user's joined challenges, capped + sorted
     * server-side. The screen-level handler stages them through
     * [com.example.betterme.utils.ChallengeShareImagePrep] before launching.
     */
    data class LaunchShareSheet(
        val text: String,
        val imageSources: List<String> = emptyList()
    ) : ChallengeOverviewEvent()
}
