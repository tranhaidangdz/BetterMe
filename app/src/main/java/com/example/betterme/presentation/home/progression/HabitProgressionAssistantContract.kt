package com.example.betterme.presentation.home.progression

import com.example.betterme.base.MviIntent
import com.example.betterme.base.MviSingleEvent
import com.example.betterme.base.MviViewState
import com.example.betterme.domain.ai.progression.HabitProgressionAnalysis

/**
 * State machine for the Home-screen inline Smart Habit Progression card —
 * upbeat mirror of the Recovery contract.
 *
 * The card auto-loads on Home entry. When the use case returns
 * `shouldProgress = false` (gates didn't all pass) the screen collapses
 * the card to [Hidden] — the UI never lectures a stable user.
 */
sealed class HabitProgressionUi {
    data object Idle : HabitProgressionUi()
    data object Loading : HabitProgressionUi()
    /** No progression needed. Card is hidden. */
    data object Hidden : HabitProgressionUi()
    data class Success(val analysis: HabitProgressionAnalysis) : HabitProgressionUi()
    data class Error(val message: String) : HabitProgressionUi()
}

data class HabitProgressionState(
    val ui: HabitProgressionUi = HabitProgressionUi.Idle
) : MviViewState

sealed class HabitProgressionIntent : MviIntent {
    /** Run the analysis (cache-first). Fired by HomeScreen on entry. */
    data class Analyze(val forceRefresh: Boolean = false) : HabitProgressionIntent()
    /** User tapped dismiss — collapse the card for this session. */
    data object Dismiss : HabitProgressionIntent()
}

sealed class HabitProgressionEvent : MviSingleEvent
