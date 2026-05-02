package com.example.betterme.presentation.habitdetail

import com.example.betterme.base.MviIntent
import com.example.betterme.base.MviSingleEvent
import com.example.betterme.base.MviViewState

// ============================================================
// ENUM — tab bên dưới
// ============================================================
enum class HabitDetailTab(val label: String) {
    HISTORY("Lịch sử check in"),
    AI_SUGGEST("Nhắc nhở"),
    STATS("Thống kê")
}

// ============================================================
// UI MODELS
// ============================================================
data class CheckInLogUiModel(
    val logId: Int,
    val dateFormatted: String,   // "Thứ Ba, 28/1/2026"
    val timeFormatted: String,   // "21:30"
    val note: String?,
    val imageUri: String?,
    val status: String           // "DONE" or "SKIPPED"
)

data class CalendarDayUiModel(
    val day: Int,
    val dateMillis: Long,
    val isCurrentMonth: Boolean,
    val isToday: Boolean,
    val isCheckedIn: Boolean
)

data class HabitStatUiModel(
    val totalCheckIns: Int = 0,
    val currentStreak: Int = 0,
    val longestStreak: Int = 0,
    val weeklyProgress: Int = 0,    // x/7
    val totalDays: Int = 0,
    val completionRate: Int = 0     // %
)

// ============================================================
// STATE
// ============================================================
data class HabitDetailState(
    val isLoading: Boolean = false,
    val habitId: Int = -1,
    val habitTitle: String = "",
    val habitDescription: String? = null,
    val categoryName: String = "",
    val categoryIcon: String = "📌",
    val reminderTime: String? = null,
    val isCompletedToday: Boolean = false,

    // Streak info
    val currentStreak: Int = 0,
    val longestStreak: Int = 0,
    val weeklyProgress: Int = 0,
    val weeklyTotal: Int = 7,

    // Tab
    val selectedTab: HabitDetailTab = HabitDetailTab.HISTORY,

    // Calendar
    val calendarMonth: Int = 0,      // 0-indexed
    val calendarYear: Int = 2026,
    val calendarDays: List<CalendarDayUiModel> = emptyList(),
    val calendarTitle: String = "",

    // History tab
    val checkInLogs: List<CheckInLogUiModel> = emptyList(),

    // Stats tab
    val stats: HabitStatUiModel = HabitStatUiModel()
) : MviViewState

// ============================================================
// INTENT
// ============================================================
sealed class HabitDetailIntent : MviIntent {
    data class LoadHabit(val habitId: Int) : HabitDetailIntent()
    data class SelectTab(val tab: HabitDetailTab) : HabitDetailIntent()
    data object CheckInToday : HabitDetailIntent()
    data object PreviousMonth : HabitDetailIntent()
    data object NextMonth : HabitDetailIntent()
}

// ============================================================
// EVENT
// ============================================================
sealed class HabitDetailEvent : MviSingleEvent {
    data class ShowMessage(val message: String) : HabitDetailEvent()
}
