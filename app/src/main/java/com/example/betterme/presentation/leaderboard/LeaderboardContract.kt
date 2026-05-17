package com.example.betterme.presentation.leaderboard

import com.example.betterme.base.MviIntent
import com.example.betterme.base.MviSingleEvent
import com.example.betterme.base.MviViewState
import com.example.betterme.domain.repository.ChallengeLeaderboardRepository.LeaderboardSnapshot

/**
 * State machine for the monthly challenge leaderboard screen.
 *
 *  - [Idle] — first composition, before the initial load fires.
 *  - [Loading] — full-screen spinner; only on first load. Subsequent
 *    refreshes keep the previous snapshot on screen and overlay a small
 *    "refreshing" pill so the user keeps context.
 *  - [Success] — has a snapshot to render.
 *  - [Empty] — challenge has no entries (real OR seeded — vanishingly
 *    rare due to the seeder, but still a defined state).
 *  - [Error] — total failure with no cached snapshot to fall back to.
 *
 * The same screen handles both single-season view and season-picker
 * switching; [activeSeasonKey] tracks which month is currently shown.
 */
sealed class LeaderboardUi {
    data object Idle : LeaderboardUi()
    data object Loading : LeaderboardUi()
    data class Success(val snapshot: LeaderboardSnapshot) : LeaderboardUi()
    data object Empty : LeaderboardUi()
    data class Error(val message: String) : LeaderboardUi()
}

data class LeaderboardState(
    val ui: LeaderboardUi = LeaderboardUi.Idle,
    val challengeId: Int = 0,
    val challengeTitle: String = "",
    val activeSeasonKey: String = "",
    val availableSeasons: List<String> = emptyList(),
    val isRefreshing: Boolean = false,
    /**
     * Phase 2 — the highest-priority motivational event the latest
     * refresh produced. Surfaced via the toast composable; null clears
     * the toast. Auto-cleared by the screen after the toast self-
     * dismisses (3.5s) so we don't re-show it on recomposition.
     */
    val activeMotivationalEvent: com.example.betterme.domain.leaderboard.MotivationalEvent? = null
) : MviViewState

sealed class LeaderboardIntent : MviIntent {
    /** Boot — load the current season for the given challenge. */
    data class Initialize(val challengeId: Int, val challengeTitle: String) : LeaderboardIntent()
    /** Switch the active season (still same challenge). */
    data class SelectSeason(val seasonKey: String) : LeaderboardIntent()
    /** Manual user-initiated refresh — bypasses the session cache. */
    data object Refresh : LeaderboardIntent()
    /** User tapped or auto-dismiss timer fired on the toast. */
    data object DismissMotivationalEvent : LeaderboardIntent()
}

sealed class LeaderboardEvent : MviSingleEvent {
    data class ShowError(val message: String) : LeaderboardEvent()
}
