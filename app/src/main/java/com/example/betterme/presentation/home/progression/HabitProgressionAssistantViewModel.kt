package com.example.betterme.presentation.home.progression

import androidx.lifecycle.viewModelScope
import com.example.betterme.base.BaseMviViewModel
import com.example.betterme.domain.usecase.ai.AnalyzeHabitProgressionUseCase
import kotlinx.coroutines.launch

/**
 * VM for the Home-screen Smart Habit Progression card. Lives outside
 * `HomeViewModel` so the existing home pipeline stays focused on tile
 * data — this VM only handles the AI coaching surface.
 *
 * Re-entrancy: [analyze] short-circuits while Loading so a recomposition
 * of Home doesn't fan out into parallel analyses.
 *
 * No apply-action flow this iteration: HabitEntity doesn't store
 * duration / frequency / difficulty, so all progression actions are
 * advisory. The card surfaces a "Tham khảo" hint instead of an Apply CTA.
 */
class HabitProgressionAssistantViewModel(
    private val analyzeHabitProgression: AnalyzeHabitProgressionUseCase
) : BaseMviViewModel<HabitProgressionIntent, HabitProgressionState, HabitProgressionEvent>() {

    override fun initState(): HabitProgressionState = HabitProgressionState()

    override fun processIntent(intent: HabitProgressionIntent) {
        when (intent) {
            is HabitProgressionIntent.Analyze -> analyze(intent.forceRefresh)
            is HabitProgressionIntent.Dismiss -> updateState {
                copy(ui = HabitProgressionUi.Hidden)
            }
        }
    }

    private fun analyze(forceRefresh: Boolean) {
        if (currentState.ui is HabitProgressionUi.Loading) return
        viewModelScope.launch {
            updateState { copy(ui = HabitProgressionUi.Loading) }
            val result = runCatching {
                analyzeHabitProgression(forceRefresh = forceRefresh)
            }.getOrElse {
                updateState {
                    copy(ui = HabitProgressionUi.Error("Không thể phân tích lúc này. Thử lại sau."))
                }
                return@launch
            }
            if (!result.shouldProgress) {
                updateState { copy(ui = HabitProgressionUi.Hidden) }
            } else {
                updateState { copy(ui = HabitProgressionUi.Success(result)) }
            }
        }
    }
}
