package com.example.betterme.presentation.onboarding.ai

import com.example.betterme.base.MviIntent
import com.example.betterme.base.MviSingleEvent
import com.example.betterme.base.MviViewState
import com.example.betterme.domain.ai.onboarding.OnboardingProfile
import com.example.betterme.domain.ai.onboarding.OnboardingSuggestedHabit
import com.example.betterme.domain.ai.onboarding.OnboardingSuggestion

/**
 * State machine for the AI Onboarding bottom sheet. Mirrors the schedule
 * analyzer pattern: Idle / Loading / Success / Error, plus tracking of which
 * suggestion rows the user has already applied (to flip their card to a
 * "✓ Đã thêm" state instantly while the DB insert resolves).
 */
sealed class OnboardingAiUi {
    data object Idle : OnboardingAiUi()
    data object Loading : OnboardingAiUi()
    data class Success(
        val suggestion: OnboardingSuggestion,
        /** Titles of habits the user has already accepted in this session. */
        val acceptedTitles: Set<String> = emptySet(),
        /** Set briefly after Apply All; UI shows a snackbar then clears. */
        val bulkAppliedCount: Int? = null
    ) : OnboardingAiUi()
    data class Error(val message: String) : OnboardingAiUi()
}

data class OnboardingAiState(
    val ui: OnboardingAiUi = OnboardingAiUi.Idle,
    val isApplyingBulk: Boolean = false
) : MviViewState

sealed class OnboardingAiIntent : MviIntent {
    /**
     * Open the sheet and run the suggester. `forceRefresh = true` bypasses
     * the 24h cache (used by the "Tạo lại" pill).
     */
    data class Analyze(
        val profile: OnboardingProfile,
        val forceRefresh: Boolean = false
    ) : OnboardingAiIntent()

    /** Accept a single suggestion into the user's habits. */
    data class Accept(val suggestion: OnboardingSuggestedHabit) : OnboardingAiIntent()

    /** Accept every suggestion the user hasn't accepted yet. */
    data object AcceptAll : OnboardingAiIntent()

    /** Close the sheet (resets to Idle). */
    data object Dismiss : OnboardingAiIntent()

    /** Clear the post-bulk-apply snackbar marker without dismissing. */
    data object ClearBulkAppliedToast : OnboardingAiIntent()
}

sealed class OnboardingAiEvent : MviSingleEvent {
    /** Single-shot: one habit added. The screen can show a small toast. */
    data class HabitAccepted(val title: String) : OnboardingAiEvent()
}
