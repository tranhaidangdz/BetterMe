package com.example.betterme.presentation.addhabit

import com.example.betterme.base.MviIntent
import com.example.betterme.base.MviSingleEvent
import com.example.betterme.base.MviViewState
import com.example.betterme.data.local.room.entities.CategoryEntity

// ============================================================
// STATE
// ============================================================
data class AddHabitState(
    val isLoading: Boolean = false,
    val title: String = "",
    val description: String = "",
    val selectedCategoryId: Int? = null,
    val selectedCategoryName: String = "",
    val reminderTime: String = "",
    val startDate: Long = System.currentTimeMillis(),
    val endDate: Long? = null,
    val categories: List<CategoryEntity> = emptyList(),
    val showCategorySelector: Boolean = false,
    val titleError: String? = null,
) : MviViewState

// ============================================================
// INTENT
// ============================================================
sealed class AddHabitIntent : MviIntent {
    data object LoadCategories : AddHabitIntent()
    data class InputTitle(val title: String) : AddHabitIntent()
    data class InputDescription(val description: String) : AddHabitIntent()
    data class SelectCategory(val categoryId: Int, val categoryName: String) : AddHabitIntent()
    data class InputReminderTime(val time: String) : AddHabitIntent()
    data class InputStartDate(val dateMillis: Long) : AddHabitIntent()
    data class InputEndDate(val dateMillis: Long?) : AddHabitIntent()
    data object ToggleCategorySelector : AddHabitIntent()
    data object Submit : AddHabitIntent()
}

// ============================================================
// EVENT
// ============================================================
sealed class AddHabitEvent : MviSingleEvent {
    data object SaveSuccess : AddHabitEvent()
    data class ShowError(val message: String) : AddHabitEvent()
}
