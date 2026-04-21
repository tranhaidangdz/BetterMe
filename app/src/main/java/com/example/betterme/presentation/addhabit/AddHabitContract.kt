package com.example.betterme.presentation.addhabit

import com.example.betterme.base.MviIntent
import com.example.betterme.base.MviSingleEvent
import com.example.betterme.base.MviViewState

data class AddHabitCategoryUiModel(
    val id: Int,
    val name: String,
    val icon: String
)

data class AddHabitState(
    val isLoading: Boolean = false,
    val categories: List<AddHabitCategoryUiModel> = emptyList(),
    val selectedCategoryId: Int? = null,
    val selectedCategoryName: String = "",
    val title: String = "",
    val description: String = "",
    val startDateMillis: Long = System.currentTimeMillis(),
    val endDateMillis: Long = System.currentTimeMillis(),
    val reminderHour: Int = 7,
    val reminderMinute: Int = 0,
    val repeatPattern: String = "Hàng ngày"
) : MviViewState {
    val reminderTimeFormatted: String
        get() = String.format("%02d:%02d", reminderHour, reminderMinute)
}

sealed class AddHabitIntent : MviIntent {
    data object LoadData : AddHabitIntent()
    data class SelectCategory(val id: Int) : AddHabitIntent()
    data class ChangeTitle(val value: String) : AddHabitIntent()
    data class ChangeDescription(val value: String) : AddHabitIntent()
    data class SetStartDate(val millis: Long) : AddHabitIntent()
    data class SetEndDate(val millis: Long) : AddHabitIntent()
    data class SetReminderTime(val hour: Int, val minute: Int) : AddHabitIntent()
    data class SetRepeatPattern(val value: String) : AddHabitIntent()
    data object Submit : AddHabitIntent()
}

sealed class AddHabitEvent : MviSingleEvent {
    data class ShowError(val message: String) : AddHabitEvent()
    data class ShowSuccess(val message: String) : AddHabitEvent()
}
