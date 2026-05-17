package com.example.betterme.presentation.main

import com.example.betterme.base.MviIntent
import com.example.betterme.base.MviSingleEvent
import com.example.betterme.base.MviViewState
import com.example.betterme.presentation.main.model.MainTab

data class MainState(
    val selectedTab: MainTab = MainTab.HOME,
    val homeRefreshVersion: Int = 0,
    val categoryDetailId: Int? = null,
    val categoryDetailName: String = "",
    val categoryDetailIcon: String = "",
    val habitDetailId: Int? = null,
    // Challenge overlay state
    val challengeDetailId: Int? = null,
    val challengeDetailIsPreview: Boolean = false,
    val showChallengeDiscover: Boolean = false,
    val showChallengeAchievements: Boolean = false,
    val showChallengeBadges: Boolean = false,
    val challengeGroupId: Int? = null,
    val showChallengeUpcoming: Boolean = false,
    val showChallengeCompleted: Boolean = false,
    val challengeCelebrationId: Int? = null,
    /** When non-null, render the monthly Leaderboard overlay for this challenge. */
    val leaderboardChallengeId: Int? = null,
    val leaderboardChallengeTitle: String = "",
    /** Phase 2B — global leaderboard overlay (tabbed Global / Friends / Winners). */
    val showGlobalLeaderboard: Boolean = false
) : MviViewState

sealed class MainIntent : MviIntent {
    data class SelectTab(val tab: MainTab) : MainIntent()
    data object HabitAdded : MainIntent()
    data class OpenCategoryDetail(
        val categoryId: Int,
        val categoryName: String,
        val categoryIcon: String
    ) : MainIntent()
    data object CloseCategoryDetail : MainIntent()
    data class OpenHabitDetail(val habitId: Int) : MainIntent()
    data object CloseHabitDetail : MainIntent()

    // Challenge overlays
    data class OpenChallengeDetail(val id: Int, val isPreview: Boolean = false) : MainIntent()
    data object CloseChallengeDetail : MainIntent()
    data object OpenChallengeDiscover : MainIntent()
    data object CloseChallengeDiscover : MainIntent()
    data object OpenChallengeAchievements : MainIntent()
    data object CloseChallengeAchievements : MainIntent()
    data object OpenChallengeBadges : MainIntent()
    data object CloseChallengeBadges : MainIntent()
    data class OpenChallengeGroup(val challengeId: Int) : MainIntent()
    data object CloseChallengeGroup : MainIntent()
    data object OpenChallengeUpcoming : MainIntent()
    data object CloseChallengeUpcoming : MainIntent()
    data object OpenChallengeCompleted : MainIntent()
    data object CloseChallengeCompleted : MainIntent()
    data class ShowChallengeCelebration(val userChallengeId: Int) : MainIntent()
    data object DismissChallengeCelebration : MainIntent()

    /** Open the monthly leaderboard overlay for a challenge. */
    data class OpenLeaderboard(val challengeId: Int, val challengeTitle: String) : MainIntent()
    data object CloseLeaderboard : MainIntent()

    /** Phase 2B — open / close the global tabbed leaderboard. */
    data object OpenGlobalLeaderboard : MainIntent()
    data object CloseGlobalLeaderboard : MainIntent()
}

sealed class MainEvent : MviSingleEvent
