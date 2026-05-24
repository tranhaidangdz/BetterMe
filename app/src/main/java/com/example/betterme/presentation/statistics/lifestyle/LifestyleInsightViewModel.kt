package com.example.betterme.presentation.statistics.lifestyle

import androidx.lifecycle.viewModelScope
import com.example.betterme.base.BaseMviViewModel
import com.example.betterme.domain.ai.AiHomeSessionMemory
import com.example.betterme.domain.ai.AiUnavailableException
import com.example.betterme.domain.usecase.ai.AnalyzeLifestyleUseCase
import kotlinx.coroutines.launch

/**
 * Self-contained VM for the Lifestyle Insight coach card. Lives outside
 * `StatisticsViewModel` so the existing analytics dashboard pipeline stays
 * focused on numeric stats — this VM only handles the AI coaching surface.
 *
 * Re-entrancy: [analyze] short-circuits while Loading so a tap-storm on
 * the "Phân tích lại" pill doesn't fan out into parallel network calls.
 */
class LifestyleInsightViewModel(
    private val analyzeLifestyle: AnalyzeLifestyleUseCase,
    private val sessionMemory: AiHomeSessionMemory
) : BaseMviViewModel<LifestyleInsightIntent, LifestyleInsightState, LifestyleInsightEvent>() {

    override fun initState(): LifestyleInsightState = LifestyleInsightState()

    override fun processIntent(intent: LifestyleInsightIntent) {
        when (intent) {
            is LifestyleInsightIntent.Analyze -> analyze(intent.forceRefresh)
        }
    }

    /**
     * Same two-layer throttle as the Home AI VMs — see
     * [com.example.betterme.domain.ai.AiHomeSessionMemory] for the rules.
     * Statistics screen entry runs through the session-memory gate so
     * tab-toggling doesn't burn quota; the user's manual "Phân tích lại"
     * pill always passes `forceRefresh = true`.
     */
    private fun analyze(forceRefresh: Boolean) {
        if (currentState.ui is LifestyleInsightUi.Loading) return
        if (!forceRefresh && !sessionMemory.shouldAutoAnalyze(AiHomeSessionMemory.Surface.LIFESTYLE_INSIGHT)) {
            return
        }
        viewModelScope.launch {
            updateState { copy(ui = LifestyleInsightUi.Loading) }
            val result = runCatching {
                analyzeLifestyle(forceRefresh = forceRefresh)
            }.getOrElse { e ->
                // Surface the AI repo's category-specific Vietnamese message when
                // available; fall back to a generic line for unexpected throwables.
                val message = (e as? AiUnavailableException)?.let {
                    AiUnavailableException.userMessage(it.category, it.message)
                } ?: "Không thể phân tích lúc này. Thử lại sau."
                updateState { copy(ui = LifestyleInsightUi.Error(message)) }
                return@launch
            }
            sessionMemory.markAnalyzed(AiHomeSessionMemory.Surface.LIFESTYLE_INSIGHT)
            updateState { copy(ui = LifestyleInsightUi.Success(result)) }
        }
    }
}
