package com.example.betterme.presentation.statistics.lifestyle

import androidx.lifecycle.viewModelScope
import com.example.betterme.base.BaseMviViewModel
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
    private val analyzeLifestyle: AnalyzeLifestyleUseCase
) : BaseMviViewModel<LifestyleInsightIntent, LifestyleInsightState, LifestyleInsightEvent>() {

    override fun initState(): LifestyleInsightState = LifestyleInsightState()

    override fun processIntent(intent: LifestyleInsightIntent) {
        when (intent) {
            is LifestyleInsightIntent.Analyze -> analyze(intent.forceRefresh)
        }
    }

    private fun analyze(forceRefresh: Boolean) {
        if (currentState.ui is LifestyleInsightUi.Loading) return
        viewModelScope.launch {
            updateState { copy(ui = LifestyleInsightUi.Loading) }
            val result = runCatching {
                analyzeLifestyle(forceRefresh = forceRefresh)
            }.getOrElse {
                // Repo already maps every known failure to a canned insight;
                // landing here means something unexpected leaked through.
                updateState {
                    copy(ui = LifestyleInsightUi.Error("Không thể phân tích lúc này. Thử lại sau."))
                }
                return@launch
            }
            updateState { copy(ui = LifestyleInsightUi.Success(result)) }
        }
    }
}
