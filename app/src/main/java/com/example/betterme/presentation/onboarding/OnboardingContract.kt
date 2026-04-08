package com.example.betterme.presentation.onboarding

import com.example.betterme.base.MviIntent
import com.example.betterme.base.MviSingleEvent
import com.example.betterme.base.MviViewState

data class OnboardingState(
    val isLoading: Boolean = false
) : MviViewState

sealed class OnboardingIntent : MviIntent {
    data object NavigateToHabitSelection : OnboardingIntent()
}

sealed class OnboardingEvent : MviSingleEvent {
    data object NavigateToHabitSelection : OnboardingEvent()
}