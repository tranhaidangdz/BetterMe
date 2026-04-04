package com.example.betterme.presentation.onboarding

import androidx.lifecycle.viewModelScope
import com.example.betterme.base.BaseMviViewModel
import com.example.betterme.data.local.datastore.DataStoreManager
import kotlinx.coroutines.launch

class OnboardingViewModel(
    private val dataStoreManager: DataStoreManager
) : BaseMviViewModel<OnboardingIntent, OnboardingState, OnboardingEvent>() {
    override fun initState(): OnboardingState {
        return OnboardingState()
    }

    override fun processIntent(intent: OnboardingIntent) {
        when (intent) {
            OnboardingIntent.NavigateToHabitSelection -> handleNavigateToHabitSelection()
        }
    }

    private fun handleNavigateToHabitSelection() {
        viewModelScope.launch {
            dataStoreManager.setDoneFirstTime()
            sendEvent(OnboardingEvent.NavigateToHabitSelection)
        }
    }
}