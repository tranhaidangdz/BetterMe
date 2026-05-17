package com.example.betterme.presentation.home.recovery

import androidx.lifecycle.viewModelScope
import com.example.betterme.base.BaseMviViewModel
import com.example.betterme.domain.ai.AiHomeSessionMemory
import com.example.betterme.domain.usecase.ai.AnalyzeHabitRecoveryUseCase
import kotlinx.coroutines.launch

/**
 * VM for the Home-screen Adaptive Habit Recovery card. Lives outside
 * `HomeViewModel` so the existing home pipeline stays focused on tile
 * data — this VM only handles the AI coaching surface.
 *
 * Re-entrancy: [analyze] short-circuits while Loading so a re-composition
 * of Home doesn't fan out into parallel analyses.
 *
 * Apply-action concurrency: only one action can be in-flight at a time
 * (gated by `applyingIndex != null`). The use case persists the mutation
 * itself; on success the action moves into `appliedIndexes` and the card
 * keeps the other suggestions visible.
 */
class HabitRecoveryAssistantViewModel(
    private val analyzeHabitRecovery: AnalyzeHabitRecoveryUseCase,
    private val sessionMemory: AiHomeSessionMemory
) : BaseMviViewModel<HabitRecoveryIntent, HabitRecoveryState, HabitRecoveryEvent>() {

    override fun initState(): HabitRecoveryState = HabitRecoveryState()

    override fun processIntent(intent: HabitRecoveryIntent) {
        when (intent) {
            is HabitRecoveryIntent.Analyze -> analyze(intent.forceRefresh)
            is HabitRecoveryIntent.ApplyAction -> applyAction(intent.index, intent.action)
            is HabitRecoveryIntent.Dismiss -> {
                sessionMemory.markDismissed(AiHomeSessionMemory.Surface.RECOVERY)
                updateState { copy(ui = HabitRecoveryUi.Hidden) }
            }
        }
    }

    /**
     * Two-layer throttle:
     *  1. Session memory — skip analysis entirely when the Home auto-launch
     *     was already served recently (default 4h) OR the user dismissed
     *     this card today. Keeps the previous Success/Hidden state visible
     *     instead of flashing the Loading skeleton on every Home re-entry.
     *  2. Re-entrancy — skip while already Loading.
     *
     * `forceRefresh = true` (user tapped "Phân tích lại") bypasses the
     * session-memory gate but still skips the in-flight Loading state.
     */
    private fun analyze(forceRefresh: Boolean) {
        if (currentState.ui is HabitRecoveryUi.Loading) return
        if (!forceRefresh && !sessionMemory.shouldAutoAnalyze(AiHomeSessionMemory.Surface.RECOVERY)) {
            // Honor the throttle. If we already have a terminal result in
            // state (Success / Hidden / Error), keep showing it — the user
            // sees their last analysis instantly with no Loading flicker.
            // If state is Idle (fresh process), leaving it Idle means the
            // card stays hidden until the next eligible window.
            return
        }
        viewModelScope.launch {
            updateState { copy(ui = HabitRecoveryUi.Loading) }
            val result = runCatching {
                analyzeHabitRecovery(forceRefresh = forceRefresh)
            }.getOrElse {
                // Repo's canned path should catch all known failures; if we
                // land here something exotic leaked through.
                updateState {
                    copy(ui = HabitRecoveryUi.Error("Không thể phân tích lúc này. Thử lại sau."))
                }
                return@launch
            }
            sessionMemory.markAnalyzed(AiHomeSessionMemory.Surface.RECOVERY)
            if (!result.shouldRecover) {
                updateState { copy(ui = HabitRecoveryUi.Hidden) }
            } else {
                updateState { copy(ui = HabitRecoveryUi.Success(result)) }
            }
        }
    }

    private fun applyAction(index: Int, action: com.example.betterme.domain.ai.recovery.HabitRecoveryAction) {
        val current = currentState.ui as? HabitRecoveryUi.Success ?: return
        if (current.applyingIndex != null) return
        if (index in current.appliedIndexes) return

        viewModelScope.launch {
            updateState { copy(ui = current.copy(applyingIndex = index)) }
            val landed = runCatching { analyzeHabitRecovery.applyAction(action) }
                .getOrDefault(false)
            val refreshed = (currentState.ui as? HabitRecoveryUi.Success) ?: current
            if (landed) {
                updateState {
                    copy(
                        ui = refreshed.copy(
                            applyingIndex = null,
                            appliedIndexes = refreshed.appliedIndexes + index
                        )
                    )
                }
                sendEvent(HabitRecoveryEvent.ActionApplied(action.title))
            } else {
                updateState { copy(ui = refreshed.copy(applyingIndex = null)) }
                sendEvent(
                    HabitRecoveryEvent.ActionFailed(
                        "Gợi ý này chỉ mang tính tham khảo — hãy điều chỉnh trong phần Thói quen."
                    )
                )
            }
        }
    }
}
