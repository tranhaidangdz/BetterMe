package com.example.betterme.presentation.onboarding

import com.example.betterme.base.MviIntent
import com.example.betterme.base.MviSingleEvent
import com.example.betterme.base.MviViewState

// =========================
// MODEL
// =========================
data class SuggestedHabitUiModel(
    val id: Int,
    val title: String,
    val isChecked: Boolean = true
)

data class CategoryWithHabits(
    val categoryName: String,
    val categoryIcon: String,
    val habits: List<SuggestedHabitUiModel>
)

// =========================
// STATE
// =========================
data class HabitSuggestionState(
    val isLoading: Boolean = false,
    val categoryHabits: List<CategoryWithHabits> = emptyList(),
    val reminderHour: Int = 7,
    val reminderMinute: Int = 0,
    val repeatLabel: String = "Hàng ngày",
    val showReminderPicker: Boolean = false,
    val showRepeatPicker: Boolean = false
) : MviViewState {
    val reminderTimeFormatted: String
        get() = String.format("%02d:%02d", reminderHour, reminderMinute)

    val selectedHabitCount: Int
        get() = categoryHabits.sumOf { cat -> cat.habits.count { it.isChecked } }
}

// =========================
// INTENT
// =========================
sealed class HabitSuggestionIntent : MviIntent {
    data class ToggleHabit(val id: Int) : HabitSuggestionIntent()
    data class SetReminderTime(val hour: Int, val minute: Int) : HabitSuggestionIntent()
    data class SetRepeat(val label: String) : HabitSuggestionIntent()
    data object ShowReminderPicker : HabitSuggestionIntent()
    data object DismissReminderPicker : HabitSuggestionIntent()
    data object ShowRepeatPicker : HabitSuggestionIntent()
    data object DismissRepeatPicker : HabitSuggestionIntent()
    data object StartJourney : HabitSuggestionIntent()
}

// =========================
// EVENT
// =========================
sealed class HabitSuggestionEvent : MviSingleEvent {
    data object NavigateToSignIn : HabitSuggestionEvent()
    data class ShowError(val message: String) : HabitSuggestionEvent()
}
