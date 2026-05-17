package com.example.betterme.presentation.addhabit.aicreation

import com.example.betterme.base.MviIntent
import com.example.betterme.base.MviSingleEvent
import com.example.betterme.base.MviViewState
import com.example.betterme.domain.ai.habitcreation.HabitCreationAnalysis
import com.example.betterme.domain.ai.habitcreation.HabitCreationSuggestion

/**
 * State machine for the pre-save AI assistant bottom sheet.
 *
 * Three terminal paths from the sheet:
 *  - User taps "Vẫn tạo"         → assistant emits [HabitCreationAssistantEvent.ConfirmedSave].
 *                                    Screen catches the event and dispatches the existing
 *                                    AddHabit submit intent. The assistant never inserts.
 *  - User taps "Áp dụng" (per-card or global) → assistant emits
 *                                    [HabitCreationAssistantEvent.ApplySuggestions] with the
 *                                    typed payload. Screen forwards to AddHabit VM which
 *                                    patches form state. The sheet closes after apply so the
 *                                    user can see the now-corrected form.
 *  - User taps the close icon    → sheet closes (Dismiss) with no mutations.
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

    /**
     * User tapped an "Áp dụng" CTA. Carries either a single suggestion
     * (per-card pill) or every applicable suggestion (global button). VM
     * emits [HabitCreationAssistantEvent.ApplySuggestions] and closes.
     */
    data class ApplySuggestions(val suggestions: List<HabitCreationSuggestion>) : HabitCreationAssistantIntent()

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

    /**
     * Fired when the user accepts one or more AI suggestions. Screen
     * forwards to `AddHabitIntent.ApplyAiSuggestions` which mutates form
     * state. The sheet closes itself; the screen surfaces a snackbar from
     * the AddHabit VM's own `AppliedSuggestions` event.
     */
    data class ApplySuggestions(val suggestions: List<HabitCreationSuggestion>) : HabitCreationAssistantEvent()
}
