package com.example.betterme.presentation.onboarding.ai

import androidx.lifecycle.viewModelScope
import com.example.betterme.base.BaseMviViewModel
import com.example.betterme.domain.ai.onboarding.OnboardingProfile
import com.example.betterme.domain.usecase.ai.SuggestOnboardingHabitsUseCase
import kotlinx.coroutines.launch

/**
 * Self-contained VM for the AI Onboarding bottom sheet. Sits beside (not
 * inside) `HabitSuggestionViewModel` so the onboarding screen's main MVI
 * pipeline stays focused on the static habit picker.
 *
 * Re-entrancy: [analyze] short-circuits while Loading; [acceptAllInternal]
 * short-circuits while [OnboardingAiState.isApplyingBulk] is true. Tap-storms
 * can't fan out parallel inserts.
 */
class OnboardingAiViewModel(
    private val suggestOnboarding: SuggestOnboardingHabitsUseCase
) : BaseMviViewModel<OnboardingAiIntent, OnboardingAiState, OnboardingAiEvent>() {

    override fun initState(): OnboardingAiState = OnboardingAiState()

    override fun processIntent(intent: OnboardingAiIntent) {
        when (intent) {
            is OnboardingAiIntent.Analyze -> analyze(intent.profile, intent.forceRefresh)
            is OnboardingAiIntent.Accept -> acceptOne(intent.suggestion)
            OnboardingAiIntent.AcceptAll -> acceptAllInternal()
            OnboardingAiIntent.Dismiss -> updateState {
                copy(ui = OnboardingAiUi.Idle, isApplyingBulk = false)
            }
            OnboardingAiIntent.ClearBulkAppliedToast -> {
                val current = currentState.ui
                if (current is OnboardingAiUi.Success && current.bulkAppliedCount != null) {
                    updateState { copy(ui = current.copy(bulkAppliedCount = null)) }
                }
            }
        }
    }

    private fun analyze(profile: OnboardingProfile, forceRefresh: Boolean) {
        if (currentState.ui is OnboardingAiUi.Loading) return
        viewModelScope.launch {
            updateState { copy(ui = OnboardingAiUi.Loading, isApplyingBulk = false) }
            val result = runCatching { suggestOnboarding(profile, forceRefresh = forceRefresh) }
                .getOrElse {
                    updateState {
                        copy(ui = OnboardingAiUi.Error("Không thể tạo gợi ý lúc này. Thử lại sau."))
                    }
                    return@launch
                }
            updateState { copy(ui = OnboardingAiUi.Success(suggestion = result)) }
        }
    }

    private fun acceptOne(suggestion: com.example.betterme.domain.ai.onboarding.OnboardingSuggestedHabit) {
        val current = currentState.ui as? OnboardingAiUi.Success ?: return
        if (suggestion.title in current.acceptedTitles) return
        viewModelScope.launch {
            val inserted = runCatching { suggestOnboarding.accept(suggestion) }.getOrNull()
            if (inserted != null) {
                val latest = currentState.ui as? OnboardingAiUi.Success ?: return@launch
                updateState {
                    copy(ui = latest.copy(acceptedTitles = latest.acceptedTitles + suggestion.title))
                }
                sendEvent(OnboardingAiEvent.HabitAccepted(suggestion.title))
            }
        }
    }

    private fun acceptAllInternal() {
        val current = currentState.ui as? OnboardingAiUi.Success ?: return
        if (currentState.isApplyingBulk) return
        val remaining = current.suggestion.habits.filter { it.title !in current.acceptedTitles }
        if (remaining.isEmpty()) return
        viewModelScope.launch {
            updateState { copy(isApplyingBulk = true) }
            val applied = runCatching { suggestOnboarding.acceptAll(remaining) }.getOrDefault(0)
            val nowAccepted = current.acceptedTitles + remaining.map { it.title }
            updateState {
                copy(
                    isApplyingBulk = false,
                    ui = (ui as? OnboardingAiUi.Success)?.copy(
                        acceptedTitles = nowAccepted,
                        bulkAppliedCount = applied
                    ) ?: ui
                )
            }
        }
    }
}
