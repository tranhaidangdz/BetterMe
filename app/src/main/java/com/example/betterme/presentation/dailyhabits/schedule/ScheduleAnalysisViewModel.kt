package com.example.betterme.presentation.dailyhabits.schedule

import androidx.lifecycle.viewModelScope
import com.example.betterme.base.BaseMviViewModel
import com.example.betterme.domain.usecase.ai.AnalyzeScheduleUseCase
import com.example.betterme.domain.usecase.ai.ApplyScheduleSuggestionsUseCase
import kotlinx.coroutines.launch

/**
 * Self-contained VM for the Schedule Conflict Analyzer bottom sheet. Owns just
 * the analyzer state — kept out of [com.example.betterme.presentation.dailyhabits.DailyHabitsViewModel]
 * so the Tasks tab's reactive pipeline stays focused on the habit list itself.
 *
 * Re-entrancy: both [analyze] and [apply] check the existing [ScheduleAnalysisUi] /
 * [ScheduleAnalysisState.isApplying] flags before spawning a coroutine, so a
 * tap-storm on the buttons doesn't fan out into parallel network calls.
 */
class ScheduleAnalysisViewModel(
    private val analyzeSchedule: AnalyzeScheduleUseCase,
    private val applyScheduleSuggestions: ApplyScheduleSuggestionsUseCase
) : BaseMviViewModel<ScheduleAnalysisIntent, ScheduleAnalysisState, ScheduleAnalysisEvent>() {

    override fun initState(): ScheduleAnalysisState = ScheduleAnalysisState()

    override fun processIntent(intent: ScheduleAnalysisIntent) {
        when (intent) {
            is ScheduleAnalysisIntent.Analyze -> analyze(intent.forceRefresh)
            ScheduleAnalysisIntent.ApplySuggestions -> apply()
            ScheduleAnalysisIntent.Dismiss -> updateState {
                copy(ui = ScheduleAnalysisUi.Idle, isApplying = false)
            }
            ScheduleAnalysisIntent.ClearAppliedToast -> {
                val current = currentState.ui
                if (current is ScheduleAnalysisUi.Success && current.appliedCount != null) {
                    updateState { copy(ui = current.copy(appliedCount = null)) }
                }
            }
        }
    }

    private fun analyze(forceRefresh: Boolean) {
        if (currentState.ui is ScheduleAnalysisUi.Loading) return
        viewModelScope.launch {
            updateState { copy(ui = ScheduleAnalysisUi.Loading, isApplying = false) }
            val analysis = runCatching {
                analyzeSchedule(forceRefresh = forceRefresh)
            }.getOrElse { e ->
                val message = (e as? com.example.betterme.domain.ai.AiUnavailableException)?.let {
                    com.example.betterme.domain.ai.AiUnavailableException.userMessage(it.category, it.message)
                } ?: "Không thể phân tích lịch trình lúc này. Thử lại sau."
                updateState { copy(ui = ScheduleAnalysisUi.Error(message)) }
                return@launch
            }
            updateState { copy(ui = ScheduleAnalysisUi.Success(analysis)) }
        }
    }

    private fun apply() {
        val current = currentState.ui as? ScheduleAnalysisUi.Success ?: return
        if (currentState.isApplying) return
        val suggestions = current.analysis.optimizedSchedule
        if (suggestions.isEmpty()) return
        viewModelScope.launch {
            updateState { copy(isApplying = true) }
            val applied = runCatching { applyScheduleSuggestions(suggestions) }
                .getOrDefault(0)
            updateState {
                copy(
                    isApplying = false,
                    ui = (ui as? ScheduleAnalysisUi.Success)?.copy(appliedCount = applied)
                        ?: ui
                )
            }
        }
    }
}
