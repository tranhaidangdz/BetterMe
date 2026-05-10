package com.example.betterme.presentation.dailyhabits

import com.example.betterme.base.MviIntent
import com.example.betterme.base.MviSingleEvent
import com.example.betterme.base.MviViewState
import com.example.betterme.presentation.dailyhabits.model.HabitUiModel

enum class DailyHabitFilter(val label: String) {
    ALL("Tất cả"),
    IN_PROGRESS("Đang thực hiện"),
    DONE("Đã hoàn thành")
}

data class DateUiModel(
    val month: String,
    val day: String,
    val weekDay: String,
    val dateMillis: Long,
    val isToday: Boolean = false
)

data class DailyHabitsState(
    val isLoading: Boolean = false,
    val selectedDateIndex: Int = 0,
    val todayIndex: Int = 0,
    val selectedFilter: DailyHabitFilter = DailyHabitFilter.ALL,
    val dates: List<DateUiModel> = emptyList(),
    val allHabits: List<HabitUiModel> = emptyList(),
    val visibleHabits: List<HabitUiModel> = emptyList()
) : MviViewState

sealed class DailyHabitsIntent : MviIntent {
    data object LoadData : DailyHabitsIntent()
    data class SelectDate(val index: Int) : DailyHabitsIntent()
    data class SelectFilter(val filter: DailyHabitFilter) : DailyHabitsIntent()
}

sealed class DailyHabitsEvent : MviSingleEvent
