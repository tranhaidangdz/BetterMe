package com.example.betterme.presentation.addhabit.aicreation

import androidx.lifecycle.viewModelScope
import com.example.betterme.base.BaseMviViewModel
import com.example.betterme.domain.usecase.ai.AnalyzeHabitCreationUseCase
import kotlinx.coroutines.launch

/**
 * Self-contained VM for the AI Habit Creation Assistant bottom sheet.
 *
 * Sits next to (not inside) `AddHabitViewModel` so the existing submit
 * pipeline stays untouched — this VM only orchestrates the pre-save review.
 *
 * Re-entrancy: [analyze] short-circuits while Loading and [confirmSave]
 * short-circuits if a save is already in flight (UI side keeps a guard too).
 */
class HabitCreationAssistantViewModel(
    private val analyzeHabitCreation: AnalyzeHabitCreationUseCase
) : BaseMviViewModel<
    HabitCreationAssistantIntent,
    HabitCreationAssistantState,
    HabitCreationAssistantEvent
>() {

    override fun initState(): HabitCreationAssistantState = HabitCreationAssistantState()

    override fun processIntent(intent: HabitCreationAssistantIntent) {
        when (intent) {
            is HabitCreationAssistantIntent.Analyze -> analyze(intent)
            HabitCreationAssistantIntent.ConfirmSave -> confirmSave()
            is HabitCreationAssistantIntent.ApplySuggestions -> applySuggestions(intent.suggestions)
            HabitCreationAssistantIntent.Dismiss -> updateState {
                copy(ui = HabitCreationAssistantUi.Idle)
            }
        }
    }

    /**
     * Forward to the screen via an event so AddHabit VM can mutate form
     * state. The sheet closes itself immediately so the user sees the
     * patched form behind it. Filtering to applicable-only happens here
     * — passing a list with no actionable fields is a no-op + close.
     */
    private fun applySuggestions(suggestions: List<com.example.betterme.domain.ai.habitcreation.HabitCreationSuggestion>) {
        val applicable = suggestions.filter { it.hasApplicableMutation }
        if (applicable.isNotEmpty()) {
            sendEvent(HabitCreationAssistantEvent.ApplySuggestions(applicable))
        }
        updateState { copy(ui = HabitCreationAssistantUi.Idle) }
    }

    private fun analyze(intent: HabitCreationAssistantIntent.Analyze) {
        if (currentState.ui is HabitCreationAssistantUi.Loading) return
        viewModelScope.launch {
            updateState { copy(ui = HabitCreationAssistantUi.Loading) }
            val result = runCatching {
                analyzeHabitCreation(
                    newTitle = intent.title,
                    newCategoryId = intent.categoryId,
                    newReminderTime = intent.reminderTime,
                    newDurationMinutes = intent.durationMinutes,
                    newDifficulty = intent.difficulty,
                    newFrequency = intent.frequency,
                    forceRefresh = intent.forceRefresh
                )
            }.getOrElse { e ->
                // Show the AI repo's category-specific Vietnamese message when
                // available. Habit-creation is advisory, so even on failure the
                // user can still proceed via "Vẫn tạo" — the message just
                // explains why no AI advisory shows up.
                val message = (e as? com.example.betterme.domain.ai.AiUnavailableException)?.let {
                    com.example.betterme.domain.ai.AiUnavailableException.userMessage(it.category, it.message)
                } ?: "Không thể phân tích lúc này — bạn vẫn có thể tạo thói quen."
                updateState { copy(ui = HabitCreationAssistantUi.Error(message)) }
                return@launch
            }
            updateState { copy(ui = HabitCreationAssistantUi.Success(result)) }
        }
    }

    private fun confirmSave() {
        viewModelScope.launch {
            sendEvent(HabitCreationAssistantEvent.ConfirmedSave)
            updateState { copy(ui = HabitCreationAssistantUi.Idle) }
        }
    }
}
