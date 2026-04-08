package com.example.betterme.presentation.onboarding.habitselection

import com.example.betterme.base.MviIntent
import com.example.betterme.base.MviSingleEvent
import com.example.betterme.base.MviViewState
import com.example.betterme.presentation.onboarding.model.CategoryUiModel

/**
 * =========================
 * STATE
 * =========================
 */
data class HabitSelectionState(
    val isLoading: Boolean = false,
    val categories: List<CategoryUiModel> = emptyList(),
    val selectedCount: Int = 0
) : MviViewState

/**
 * =========================
 * INTENT (UI -> ViewModel)
 * =========================
 */
sealed class HabitSelectionIntent : MviIntent {

    data class ToggleCategory(val id: Int) : HabitSelectionIntent()

    data object Continue : HabitSelectionIntent()
}

/**
 * =========================
 * EVENT (ViewModel -> UI)
 * =========================
 */
sealed class HabitSelectionEvent : MviSingleEvent {

    data class ShowError(val message: String) : HabitSelectionEvent()

    data class NavigateNext(val selectedIds: Set<Int>) : HabitSelectionEvent()
}