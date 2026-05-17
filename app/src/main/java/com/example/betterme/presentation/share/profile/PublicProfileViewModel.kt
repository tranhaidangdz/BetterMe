package com.example.betterme.presentation.share.profile

import androidx.lifecycle.viewModelScope
import com.example.betterme.base.BaseMviViewModel
import com.example.betterme.domain.share.VerificationStatus
import com.example.betterme.domain.usecase.share.LoadSharedSnapshotUseCase
import kotlinx.coroutines.launch

class PublicProfileViewModel(
    private val loadSnapshot: LoadSharedSnapshotUseCase
) : BaseMviViewModel<PublicProfileIntent, PublicProfileState, PublicProfileEvent>() {

    override fun initState(): PublicProfileState = PublicProfileState()

    override fun processIntent(intent: PublicProfileIntent) {
        when (intent) {
            is PublicProfileIntent.Load -> load(intent.userId)
        }
    }

    private fun load(userId: String) {
        if (userId.isBlank()) return
        if (currentState.userId == userId &&
            currentState.ui is PublicProfileUi.Loaded
        ) {
            return
        }
        updateState { copy(userId = userId, ui = PublicProfileUi.Loading) }
        viewModelScope.launch {
            val result = loadSnapshot(userId)
            updateState {
                copy(
                    ui = if (result.status == VerificationStatus.VALID && result.share != null) {
                        PublicProfileUi.Loaded(result.share)
                    } else {
                        PublicProfileUi.Missing(result.status)
                    }
                )
            }
        }
    }
}
