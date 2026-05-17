package com.example.betterme.presentation.leaderboard.global

import com.example.betterme.base.MviIntent
import com.example.betterme.base.MviSingleEvent
import com.example.betterme.base.MviViewState
import com.example.betterme.domain.leaderboard.LeaderboardProfile
import com.example.betterme.domain.leaderboard.MonthlyWinner
import com.example.betterme.domain.repository.GlobalLeaderboardRepository.FriendSnapshot
import com.example.betterme.domain.repository.GlobalLeaderboardRepository.GlobalSnapshot

enum class GlobalLeaderboardTab(val label: String) {
    GLOBAL("Toàn cầu"),
    FRIENDS("Đối thủ"),
    WINNERS("Quán quân")
}

/**
 * State machine for the cross-challenge GLOBAL leaderboard screen.
 *
 * Each tab caches its last-loaded payload so flipping between tabs
 * doesn't blank the screen. Only the active tab fires its load on
 * selection; other tabs lazy-load on first visit.
 */
sealed class GlobalLeaderboardUi {
    data object Loading : GlobalLeaderboardUi()
    data class Loaded(
        val global: GlobalSnapshot?,
        val friends: FriendSnapshot?,
        val winners: List<MonthlyWinner>
    ) : GlobalLeaderboardUi()
    data class Error(val message: String) : GlobalLeaderboardUi()
}

data class GlobalLeaderboardState(
    val ui: GlobalLeaderboardUi = GlobalLeaderboardUi.Loading,
    val activeTab: GlobalLeaderboardTab = GlobalLeaderboardTab.GLOBAL,
    val isRefreshing: Boolean = false,
    /** Profile sheet for the row the user tapped, if any. */
    val profileSheet: LeaderboardProfile? = null
) : MviViewState

sealed class GlobalLeaderboardIntent : MviIntent {
    data object Initialize : GlobalLeaderboardIntent()
    data class SelectTab(val tab: GlobalLeaderboardTab) : GlobalLeaderboardIntent()
    data object Refresh : GlobalLeaderboardIntent()
    data class OpenProfile(val userId: String) : GlobalLeaderboardIntent()
    data object CloseProfile : GlobalLeaderboardIntent()
}

sealed class GlobalLeaderboardEvent : MviSingleEvent {
    data class ShowError(val message: String) : GlobalLeaderboardEvent()
}
