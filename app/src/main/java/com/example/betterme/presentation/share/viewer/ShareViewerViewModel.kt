package com.example.betterme.presentation.share.viewer

import androidx.lifecycle.viewModelScope
import com.example.betterme.base.BaseMviViewModel
import com.example.betterme.domain.share.VerificationStatus
import com.example.betterme.domain.usecase.share.LoadSharedSnapshotUseCase
import kotlinx.coroutines.launch

class ShareViewerViewModel(
    private val loadSnapshot: LoadSharedSnapshotUseCase
) : BaseMviViewModel<ShareViewerIntent, ShareViewerState, ShareViewerEvent>() {

    override fun initState(): ShareViewerState = ShareViewerState()

    override fun processIntent(intent: ShareViewerIntent) {
        when (intent) {
            is ShareViewerIntent.Load -> load(intent.userId)
        }
    }

    private fun load(userId: String) {
        if (userId.isBlank()) return
        if (currentState.userId == userId &&
            currentState.ui is ShareViewerUi.Verified
        ) {
            return // already loaded — don't re-read on recomposition
        }
        updateState { copy(userId = userId, ui = ShareViewerUi.Loading) }
        viewModelScope.launch {
            val result = loadSnapshot(userId)
            updateState {
                copy(
                    ui = if (result.status == VerificationStatus.VALID && result.share != null) {
                        ShareViewerUi.Verified(result.share)
                    } else {
                        ShareViewerUi.Invalid(result.status)
                    }
                )
            }
        }
    }
}
