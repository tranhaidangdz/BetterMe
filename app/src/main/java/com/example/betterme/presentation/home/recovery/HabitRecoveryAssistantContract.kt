package com.example.betterme.presentation.home.recovery

import com.example.betterme.base.MviIntent
import com.example.betterme.base.MviSingleEvent
import com.example.betterme.base.MviViewState
import com.example.betterme.domain.ai.recovery.HabitRecoveryAction
import com.example.betterme.domain.ai.recovery.HabitRecoveryAnalysis

/**
 * State machine for the Home-screen inline Adaptive Habit Recovery card.
 *
 * The card auto-loads on Home entry. When the use case returns
 * `shouldRecover = false` (healthy user, no triggers fired) the screen
 * collapses the card to nothing — the UI never shows an empty "no struggle"
 * shell.
 *
 * The "Apply" path on each recovery action surfaces as an [Applying] flag
 * keyed by action index so simultaneous taps can't overlap and the user
 * sees a per-action spinner.
 */
sealed class HabitRecoveryUi {
    data object Idle : HabitRecoveryUi()
    data object Loading : HabitRecoveryUi()
    /** No recovery needed (`shouldRecover = false`). Card is hidden. */
    data object Hidden : HabitRecoveryUi()
    data class Success(
        val analysis: HabitRecoveryAnalysis,
        val applyingIndex: Int? = null,
        val appliedIndexes: Set<Int> = emptySet()
    ) : HabitRecoveryUi()
    data class Error(val message: String) : HabitRecoveryUi()
}

data class HabitRecoveryState(
    val ui: HabitRecoveryUi = HabitRecoveryUi.Idle
) : MviViewState

sealed class HabitRecoveryIntent : MviIntent {
    /** Run the analysis (cache-first). Fired by HomeScreen on entry. */
    data class Analyze(val forceRefresh: Boolean = false) : HabitRecoveryIntent()
    /** User tapped an action's CTA. [index] keys per-action spinner state. */
    data class ApplyAction(val index: Int, val action: HabitRecoveryAction) : HabitRecoveryIntent()
    /** User tapped dismiss — collapse the card for this session. */
    data object Dismiss : HabitRecoveryIntent()
}

sealed class HabitRecoveryEvent : MviSingleEvent {
    data class ActionApplied(val title: String) : HabitRecoveryEvent()
    data class ActionFailed(val message: String) : HabitRecoveryEvent()
}
