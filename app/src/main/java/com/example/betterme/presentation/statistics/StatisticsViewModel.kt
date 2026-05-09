package com.example.betterme.presentation.statistics

import com.example.betterme.base.BaseMviViewModel

class StatisticsViewModel : BaseMviViewModel<StatisticsIntent, StatisticsState, StatisticsEvent>() {

    override fun initState(): StatisticsState = StatisticsState()

    init {
        processIntent(StatisticsIntent.LoadData)
    }

    override fun processIntent(intent: StatisticsIntent) {
        when (intent) {
            StatisticsIntent.LoadData -> loadMockData()
            is StatisticsIntent.SelectTab -> selectTab(intent.tab)
            is StatisticsIntent.ToggleSection -> toggleSection(intent.section)
        }
    }

    private fun selectTab(tab: StatisticsTab) {
        updateState { copy(selectedTab = tab) }
        loadMockData()
    }

    private fun toggleSection(section: ExpandedSection) {
        updateState {
            copy(expandedSection = if (expandedSection == section) null else section)
        }
    }

    private fun loadMockData() {
        updateState { copy(isLoading = true) }

        val tab = currentState.selectedTab

        val overview = when (tab) {
            StatisticsTab.WEEKLY -> OverviewStats(
                completionRate = 78,
                totalCompleted = 15,
                totalFailed = 3,
                totalOngoing = 12
            )
            StatisticsTab.MONTHLY -> OverviewStats(
                completionRate = 82,
                totalCompleted = 48,
                totalFailed = 8,
                totalOngoing = 15
            )
            StatisticsTab.ALL_TIME -> OverviewStats(
                completionRate = 75,
                totalCompleted = 320,
                totalFailed = 48,
                totalOngoing = 19
            )
        }

        val streaks = when (tab) {
            StatisticsTab.WEEKLY -> StreakAnalytics(
                longestStreak = 7,
                averageStreak = 4,
                currentStreak = 5,
                totalStreakDays = 28
            )
            StatisticsTab.MONTHLY -> StreakAnalytics(
                longestStreak = 21,
                averageStreak = 8,
                currentStreak = 5,
                totalStreakDays = 156
            )
            StatisticsTab.ALL_TIME -> StreakAnalytics(
                longestStreak = 28,
                averageStreak = 8,
                currentStreak = 5,
                totalStreakDays = 768
            )
        }

        val featured = listOf(
            FeaturedHabit("🏋️", "Tập Gym 30 phút mỗi chiều", 86, 24, 28),
            FeaturedHabit("💧", "Uống 2 lít nước mỗi ngày", 71, 20, 28),
            FeaturedHabit("📖", "Đọc sách 20 phút", 64, 18, 28),
            FeaturedHabit("🧘", "Thiền 10 phút mỗi sáng", 57, 16, 28),
            FeaturedHabit("🏃", "Chạy bộ 3km mỗi sáng", 50, 14, 28),
            FeaturedHabit("🛌", "Ngủ trước 23:00", 43, 12, 28)
        )

        val journey = when (tab) {
            StatisticsTab.WEEKLY -> HabitJourney(
                totalCreated = 30,
                totalCompleted = 15,
                totalFailed = 3,
                badgesEarned = 5,
                activeDays = 7
            )
            StatisticsTab.MONTHLY -> HabitJourney(
                totalCreated = 30,
                totalCompleted = 48,
                totalFailed = 8,
                badgesEarned = 8,
                activeDays = 28
            )
            StatisticsTab.ALL_TIME -> HabitJourney(
                totalCreated = 48,
                totalCompleted = 320,
                totalFailed = 48,
                badgesEarned = 12,
                activeDays = 320
            )
        }

        val completedHabits = listOf(
            HabitStatusItem("Dậy sớm 5:30", "⏰", "Hoàn thành", "Đã hoàn thành vào 12/05/2025", "45 ngày", 100),
            HabitStatusItem("Không ăn đồ ngọt", "🍬", "Hoàn thành", "Đã hoàn thành vào 29/05/2025", "30 ngày", 100),
            HabitStatusItem("Chạy bộ 3km mỗi sáng", "🏃", "Hoàn thành", "Đã hoàn thành vào 09/04/2025", "21 ngày", 100)
        )

        val failedHabits = listOf(
            HabitStatusItem("Học 30 phút tiếng Anh", "📚", "Thất bại", "Thất bại vào 18/04/2025", "7 ngày", 35),
            HabitStatusItem("Thiền 20 phút", "🧘", "Thất bại", "Thất bại vào 10/04/2025", "5 ngày", 28),
            HabitStatusItem("Đi bộ 10.000 bước", "🚶", "Thất bại", "Thất bại vào 03/04/2025", "3 ngày", 15)
        )

        val ongoingHabits = listOf(
            HabitStatusItem("Uống 2 lít nước mỗi ngày", "💧", "Đang thực hiện", "Chuỗi hiện tại: 12 ngày", "Tỷ lệ", 80),
            HabitStatusItem("Tập Gym 30 phút mỗi chiều", "🏋️", "Đang thực hiện", "Chuỗi hiện tại: 8 ngày", "Tỷ lệ", 72),
            HabitStatusItem("Đọc sách 20 phút", "📖", "Đang thực hiện", "Chuỗi hiện tại: 6 ngày", "Tỷ lệ", 60)
        )

        val insights = when (tab) {
            StatisticsTab.WEEKLY -> AdditionalInsights(
                mostProductiveDay = "Thứ 4",
                mostActiveHour = "19:00 – 21:00",
                bestStreak = 7,
                totalCompletions = 42
            )
            StatisticsTab.MONTHLY -> AdditionalInsights(
                mostProductiveDay = "Thứ 4",
                mostActiveHour = "19:00 – 21:00",
                bestStreak = 21,
                totalCompletions = 156
            )
            StatisticsTab.ALL_TIME -> AdditionalInsights(
                mostProductiveDay = "Thứ 4",
                mostActiveHour = "19:00 – 21:00",
                bestStreak = 28,
                totalCompletions = 768
            )
        }

        updateState {
            copy(
                isLoading = false,
                overview = overview,
                streakAnalytics = streaks,
                featuredHabits = featured,
                habitJourney = journey,
                completedHabits = completedHabits,
                failedHabits = failedHabits,
                ongoingHabits = ongoingHabits,
                additionalInsights = insights
            )
        }
    }
}
