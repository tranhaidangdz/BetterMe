package com.example.betterme.presentation.statistics

import com.example.betterme.base.MviViewState

data class StatisticsState(
    val isLoading: Boolean = false,
    val selectedTab: StatisticsTab = StatisticsTab.WEEKLY,

    /** Inclusive lower bound (startOfDay millis). When null, the tab's natural range is used. */
    val customRangeStart: Long? = null,
    /** Inclusive upper bound (startOfDay millis). When null, "today" is used. */
    val customRangeEnd: Long? = null,
    /** Resolved range used for the current emission — computed in the VM from selectedTab
     *  + custom values. Surfaced on state so the UI can show "21/01 → 27/01" labels. */
    val effectiveRangeStart: Long = 0,
    val effectiveRangeEnd: Long = 0,

    val overview: OverviewStats = OverviewStats(),
    val streakAnalytics: StreakAnalytics = StreakAnalytics(),
    val challengeStats: ChallengeStats = ChallengeStats(),
    val featuredHabits: List<FeaturedHabit> = emptyList(),
    val habitJourney: HabitJourney = HabitJourney(),
    val completedHabits: List<HabitStatusItem> = emptyList(),
    val failedHabits: List<HabitStatusItem> = emptyList(),
    val ongoingHabits: List<HabitStatusItem> = emptyList(),
    val additionalInsights: AdditionalInsights = AdditionalInsights(),
    val expandedSection: ExpandedSection? = null
) : MviViewState

enum class StatisticsTab(val label: String) {
    WEEKLY("Tuần"),
    MONTHLY("Tháng"),
    ALL_TIME("Tất cả"),
    CUSTOM("Tùy chọn")
}

data class ChallengeStats(
    val joined: Int = 0,
    val completed: Int = 0,
    val active: Int = 0,
    val completionRatePct: Int = 0
)

enum class ExpandedSection {
    COMPLETED, FAILED, ONGOING
}

data class OverviewStats(
    val completionRate: Int = 0,
    val totalCompleted: Int = 0,
    val totalFailed: Int = 0,
    val totalOngoing: Int = 0
)

data class StreakAnalytics(
    val longestStreak: Int = 0,
    val averageStreak: Int = 0,
    val currentStreak: Int = 0,
    val totalStreakDays: Int = 0
)

data class FeaturedHabit(
    val icon: String = "",
    val title: String = "",
    val completionPercentage: Int = 0,
    val completedDays: Int = 0,
    val totalDays: Int = 0
)

data class HabitJourney(
    val totalCreated: Int = 0,
    val totalCompleted: Int = 0,
    val totalFailed: Int = 0,
    val badgesEarned: Int = 0,
    val activeDays: Int = 0
)

data class HabitStatusItem(
    val name: String = "",
    val icon: String = "",
    val statusLabel: String = "",
    val completionInfo: String = "",
    val streakInfo: String = "",
    val percentage: Int = 0
)

data class AdditionalInsights(
    val mostProductiveDay: String = "",
    val mostActiveHour: String = "",
    val bestStreak: Int = 0,
    val totalCompletions: Int = 0
)
