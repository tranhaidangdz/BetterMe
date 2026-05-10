package com.example.betterme.presentation.main

import com.example.betterme.base.BaseMviViewModel
import com.example.betterme.presentation.main.model.MainTab

class MainViewModel : BaseMviViewModel<MainIntent, MainState, MainEvent>() {

    override fun initState(): MainState = MainState()

    override fun processIntent(intent: MainIntent) {
        when (intent) {
            is MainIntent.SelectTab -> updateState {
                copy(
                    selectedTab = intent.tab,
                    homeRefreshVersion = if (intent.tab == MainTab.HOME) homeRefreshVersion + 1 else homeRefreshVersion,
                    // Tự đóng overlay khi chuyển tab
                    categoryDetailId = null,
                    categoryDetailName = "",
                    categoryDetailIcon = "",
                    habitDetailId = null,
                    challengeDetailId = null,
                    challengeDetailIsPreview = false,
                    showChallengeDiscover = false,
                    showChallengeAchievements = false,
                    showChallengeBadges = false,
                    challengeGroupId = null,
                    showChallengeUpcoming = false,
                    showChallengeCompleted = false,
                    challengeCelebrationId = null
                )
            }
            MainIntent.HabitAdded -> updateState { copy(homeRefreshVersion = homeRefreshVersion + 1) }
            is MainIntent.OpenCategoryDetail -> updateState {
                copy(
                    categoryDetailId = intent.categoryId,
                    categoryDetailName = intent.categoryName,
                    categoryDetailIcon = intent.categoryIcon
                )
            }
            MainIntent.CloseCategoryDetail -> updateState {
                copy(
                    categoryDetailId = null,
                    categoryDetailName = "",
                    categoryDetailIcon = ""
                )
            }
            is MainIntent.OpenHabitDetail -> updateState { copy(habitDetailId = intent.habitId) }
            MainIntent.CloseHabitDetail -> updateState { copy(habitDetailId = null) }

            // Challenge overlays
            is MainIntent.OpenChallengeDetail -> updateState {
                copy(challengeDetailId = intent.id, challengeDetailIsPreview = intent.isPreview)
            }
            MainIntent.CloseChallengeDetail -> updateState {
                copy(challengeDetailId = null, challengeDetailIsPreview = false)
            }
            MainIntent.OpenChallengeDiscover -> updateState { copy(showChallengeDiscover = true) }
            MainIntent.CloseChallengeDiscover -> updateState { copy(showChallengeDiscover = false) }
            MainIntent.OpenChallengeAchievements -> updateState { copy(showChallengeAchievements = true) }
            MainIntent.CloseChallengeAchievements -> updateState { copy(showChallengeAchievements = false) }
            MainIntent.OpenChallengeBadges -> updateState { copy(showChallengeBadges = true) }
            MainIntent.CloseChallengeBadges -> updateState { copy(showChallengeBadges = false) }
            is MainIntent.OpenChallengeGroup -> updateState { copy(challengeGroupId = intent.challengeId) }
            MainIntent.CloseChallengeGroup -> updateState { copy(challengeGroupId = null) }
            MainIntent.OpenChallengeUpcoming -> updateState { copy(showChallengeUpcoming = true) }
            MainIntent.CloseChallengeUpcoming -> updateState { copy(showChallengeUpcoming = false) }
            MainIntent.OpenChallengeCompleted -> updateState { copy(showChallengeCompleted = true) }
            MainIntent.CloseChallengeCompleted -> updateState { copy(showChallengeCompleted = false) }
            is MainIntent.ShowChallengeCelebration -> updateState { copy(challengeCelebrationId = intent.userChallengeId) }
            MainIntent.DismissChallengeCelebration -> updateState { copy(challengeCelebrationId = null) }
        }
    }
}
