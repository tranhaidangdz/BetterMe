package com.example.betterme.presentation.statistics.lifestyle

import com.example.betterme.base.MviIntent
import com.example.betterme.base.MviSingleEvent
import com.example.betterme.base.MviViewState
import com.example.betterme.domain.ai.lifestyle.LifestyleInsight

/**
 * State machine for the inline Lifestyle Insight coach card on the
 * Statistics screen.
 *
 * The card auto-loads on first composition (LaunchedEffect in the screen
 * fires Analyze) so the user doesn't have to tap a button. Refresh comes
 * via the "Phân tích lại" pill which sends `forceRefresh = true`.
 */
sealed class LifestyleInsightUi {
    data object Idle : LifestyleInsightUi()
    data object Loading : LifestyleInsightUi()
    data class Success(val insight: LifestyleInsight) : LifestyleInsightUi()
    data class Error(val message: String) : LifestyleInsightUi()
}

data class LifestyleInsightState(
    val ui: LifestyleInsightUi = LifestyleInsightUi.Idle
) : MviViewState

sealed class LifestyleInsightIntent : MviIntent {
    /**
     * Run the analysis (or read from cache when fresh). Fired automatically
     * by the Statistics screen on entry; can be re-fired by the user via
     * the "Phân tích lại" pill with `forceRefresh = true`.
     */
    data class Analyze(val forceRefresh: Boolean = false) : LifestyleInsightIntent()
}

sealed class LifestyleInsightEvent : MviSingleEvent
