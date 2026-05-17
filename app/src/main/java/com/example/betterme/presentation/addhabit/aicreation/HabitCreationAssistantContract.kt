package com.example.betterme.presentation.addhabit.aicreation

import com.example.betterme.base.MviIntent
import com.example.betterme.base.MviSingleEvent
import com.example.betterme.base.MviViewState
import com.example.betterme.domain.ai.habitcreation.HabitCreationAnalysis

/**
 * State machine for the pre-save AI assistant bottom sheet.
 *
 * Two terminal paths from the sheet:
 *  - User taps "Vẫn tạo"      → assistant emits [HabitCreationAssistantEvent.ConfirmedSave].
 *                                 Screen catches the event and dispatches the existing
 *                                 AddHabit submit intent. The assistant never inserts.
 *  - User taps "Áp dụng gợi ý" → sheet closes (Dismiss). The user remains on the form
 *                                 to manually adjust based on the displayed suggestions.
 *                                 We deliberately don't auto-mutate form state because
 *                                 most of the actionable fields (frequency, duration,
 *                                 difficulty) don't yet exist on `HabitEntity`.
 */
sealed class HabitCreationAssistantUi {
    data object Idle : HabitCreationAssistantUi()
    data object Loading : HabitCreationAssistantUi()
    data class Success(val analysis: HabitCreationAnalysis) : HabitCreationAssistantUi()
    data class Error(val message: String) : HabitCreationAssistantUi()
}

data class HabitCreationAssistantState(
    val ui: HabitCreationAssistantUi = HabitCreationAssistantUi.Idle
) : MviViewState

sealed class HabitCreationAssistantIntent : MviIntent {
    /** Trigger analysis of the form state about to be saved. */
    data class Analyze(
        val title: String,
        val categoryId: Int?,
        val reminderTime: String,
        val durationMinutes: Int = 30,
        val difficulty: String = "MEDIUM",
        val frequency: String = "daily",
        val forceRefresh: Boolean = false
    ) : HabitCreationAssistantIntent()

    /** User picked "Vẫn tạo". Assistant emits ConfirmedSave + closes itself. */
    data object ConfirmSave : HabitCreationAssistantIntent()

    /** Close the sheet (user wants to adjust the form manually). */
    data object Dismiss : HabitCreationAssistantIntent()
}

sealed class HabitCreationAssistantEvent : MviSingleEvent {
    /**
     * Fired when the user confirms save after reviewing the analysis (or
     * skips review entirely on a happy-path no-warnings analysis). Screen
     * catches this and dispatches `AddHabitIntent.SubmitHabit`.
     */
    data object ConfirmedSave : HabitCreationAssistantEvent()
}
