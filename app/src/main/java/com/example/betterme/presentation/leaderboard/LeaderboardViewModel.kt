package com.example.betterme.presentation.leaderboard

import androidx.lifecycle.viewModelScope
import com.example.betterme.base.BaseMviViewModel
import com.example.betterme.domain.leaderboard.Season
import com.example.betterme.domain.usecase.leaderboard.GetChallengeLeaderboardUseCase
import kotlinx.coroutines.launch

/**
 * VM for the full monthly leaderboard screen. Stateful around the
 * (challengeId, activeSeasonKey) pair so season-picker switches re-fetch
 * from the same repository call site.
 *
 * Re-entrancy: [load] short-circuits while Loading or Refreshing. The
 * "Tải lại" pill always passes `forceRefresh = true` so the throttle
 * skip path in the repo's session memory is bypassed.
 */
class LeaderboardViewModel(
    private val getChallengeLeaderboard: GetChallengeLeaderboardUseCase
) : BaseMviViewModel<LeaderboardIntent, LeaderboardState, LeaderboardEvent>() {

    override fun initState(): LeaderboardState = LeaderboardState(
        availableSeasons = Season.recent(6)
    )

    override fun processIntent(intent: LeaderboardIntent) {
        when (intent) {
            is LeaderboardIntent.Initialize -> initialize(intent)
            is LeaderboardIntent.SelectSeason -> selectSeason(intent.seasonKey)
            LeaderboardIntent.Refresh -> refresh()
        }
    }

    private fun initialize(intent: LeaderboardIntent.Initialize) {
        if (currentState.challengeId == intent.challengeId &&
            currentState.ui is LeaderboardUi.Success
        ) {
            // Already loaded for this challenge — keep existing snapshot
            // visible to honor the "don't reload on tab switch" rule.
            return
        }
        updateState {
            copy(
                challengeId = intent.challengeId,
                challengeTitle = intent.challengeTitle,
                activeSeasonKey = Season.current()
            )
        }
        load(forceRefresh = false)
    }

    private fun selectSeason(seasonKey: String) {
        if (seasonKey == currentState.activeSeasonKey) return
        updateState { copy(activeSeasonKey = seasonKey) }
        load(forceRefresh = false)
    }

    private fun refresh() {
        if (currentState.ui is LeaderboardUi.Loading) return
        if (currentState.isRefreshing) return
        updateState { copy(isRefreshing = true) }
        load(forceRefresh = true)
    }

    private fun load(forceRefresh: Boolean) {
        val state = currentState
        val challengeId = state.challengeId
        val seasonKey = state.activeSeasonKey
        if (challengeId == 0 || seasonKey.isBlank()) return
        if (state.ui is LeaderboardUi.Loading) return

        // Show the full-screen Loading only on the very first hit. On
        // re-fetches keep the existing Success snapshot visible so the
        // user has context while the new month loads.
        if (state.ui !is LeaderboardUi.Success) {
            updateState { copy(ui = LeaderboardUi.Loading) }
        }

        viewModelScope.launch {
            val snapshot = runCatching {
                getChallengeLeaderboard(
                    challengeId = challengeId,
                    seasonKey = seasonKey,
                    forceRefresh = forceRefresh
                )
            }.getOrElse { error ->
                updateState {
                    copy(
                        ui = if (ui is LeaderboardUi.Success) ui
                        else LeaderboardUi.Error("Không thể tải bảng xếp hạng. Hãy thử lại."),
                        isRefreshing = false
                    )
                }
                sendEvent(LeaderboardEvent.ShowError(error.message ?: "Network error"))
                return@launch
            }
            val nextUi = if (snapshot.entries.isEmpty()) {
                LeaderboardUi.Empty
            } else {
                LeaderboardUi.Success(snapshot)
            }
            updateState { copy(ui = nextUi, isRefreshing = false) }
        }
    }
}
