package com.example.betterme.presentation.onboarding.habitsuggestion

import com.example.betterme.base.MviIntent
import com.example.betterme.base.MviSingleEvent
import com.example.betterme.base.MviViewState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// =========================
// MODEL
// =========================
data class SuggestedHabitUiModel(
    val id: Int,
    val title: String,
    val isChecked: Boolean = false,
    val reminderHour: Int = 7,
    val reminderMinute: Int = 0,
    val repeatLabel: String = "Hàng ngày",
    val startDateMillis: Long = System.currentTimeMillis(),
    val endDateMillis: Long = System.currentTimeMillis() + 30L * 24 * 60 * 60 * 1000 // mặc định 30 ngày
) {
    val reminderTimeFormatted: String
        get() = String.format("%02d:%02d", reminderHour, reminderMinute)

    val startDateFormatted: String
        get() = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(startDateMillis))

    val endDateFormatted: String
        get() = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(endDateMillis))
}

data class CategoryWithHabits(
    val categoryName: String,
    val categoryIcon: String,
    val categoryId: Int,
    val habits: List<SuggestedHabitUiModel>
)

// =========================
// STATE
// =========================
data class HabitSuggestionState(
    val isLoading: Boolean = false,
    val categoryHabits: List<CategoryWithHabits> = emptyList(),
    // Dialog state cho habit đang được chỉnh sửa
    val editingHabitId: Int? = null,
    val showReminderPicker: Boolean = false,
    val showRepeatPicker: Boolean = false,
    val showStartDatePicker: Boolean = false,
    val showEndDatePicker: Boolean = false
) : MviViewState {
    val selectedHabitCount: Int
        get() = categoryHabits.sumOf { cat -> cat.habits.count { it.isChecked } }

    val editingHabit: SuggestedHabitUiModel?
        get() = categoryHabits.flatMap { it.habits }.find { it.id == editingHabitId }
}

// =========================
// INTENT
// =========================
sealed class HabitSuggestionIntent : MviIntent {
    data class ToggleHabit(val id: Int) : HabitSuggestionIntent()
    data class SetReminderTime(val habitId: Int, val hour: Int, val minute: Int) : HabitSuggestionIntent()
    data class SetRepeat(val habitId: Int, val label: String) : HabitSuggestionIntent()
    data class ShowReminderPicker(val habitId: Int) : HabitSuggestionIntent()
    data object DismissReminderPicker : HabitSuggestionIntent()
    data class ShowRepeatPicker(val habitId: Int) : HabitSuggestionIntent()
    data object DismissRepeatPicker : HabitSuggestionIntent()
    // Ngày bắt đầu / kết thúc
    data class ShowStartDatePicker(val habitId: Int) : HabitSuggestionIntent()
    data class ShowEndDatePicker(val habitId: Int) : HabitSuggestionIntent()
    data class SetStartDate(val habitId: Int, val millis: Long) : HabitSuggestionIntent()
    data class SetEndDate(val habitId: Int, val millis: Long) : HabitSuggestionIntent()
    data object DismissDatePicker : HabitSuggestionIntent()
    // Dialog confirm/dismiss
    data class ConfirmHabitSettings(val habitId: Int) : HabitSuggestionIntent()
    data class DismissHabitSettings(val habitId: Int) : HabitSuggestionIntent()
    data object StartJourney : HabitSuggestionIntent()
}

// =========================
// EVENT
// =========================
sealed class HabitSuggestionEvent : MviSingleEvent {
    data object NavigateToMain : HabitSuggestionEvent()
    data class ShowError(val message: String) : HabitSuggestionEvent()
}
