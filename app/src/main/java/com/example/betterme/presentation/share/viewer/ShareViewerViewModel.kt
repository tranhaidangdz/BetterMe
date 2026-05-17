package com.example.betterme.presentation.share.viewer

import androidx.lifecycle.viewModelScope
import com.example.betterme.base.BaseMviViewModel
import com.example.betterme.domain.repository.ShareRepository
import com.example.betterme.domain.share.VerificationStatus
import com.example.betterme.domain.usecase.share.LoadSharedSnapshotUseCase
import kotlinx.coroutines.launch

class ShareViewerViewModel(
    private val loadSnapshot: LoadSharedSnapshotUseCase,
    private val shareRepository: ShareRepository
) : BaseMviViewModel<ShareViewerIntent, ShareViewerState, ShareViewerEvent>() {

    override fun initState(): ShareViewerState = ShareViewerState()

    override fun processIntent(intent: ShareViewerIntent) {
        when (intent) {
            is ShareViewerIntent.Load -> load(intent.shareId)
            ShareViewerIntent.ReVerify -> reverify()
        }
    }

    private fun load(shareId: String) {
        if (shareId.isBlank()) return
        if (currentState.shareId == shareId &&
            currentState.ui is ShareViewerUi.Verified
        ) {
            return // already loaded — don't re-network on recomposition
        }
        updateState { copy(shareId = shareId, ui = ShareViewerUi.Loading) }
        viewModelScope.launch {
            val result = loadSnapshot(shareId)
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

    private fun reverify() {
        val shareId = currentState.shareId
        if (shareId.isBlank() || currentState.isReverifying) return
        updateState { copy(isReverifying = true) }
        viewModelScope.launch {
            val status = shareRepository.verifyShare(shareId)
            updateState { copy(isReverifying = false) }
            val msg = when (status) {
                VerificationStatus.VALID -> "✔ Vẫn hợp lệ — đã xác minh lại."
                VerificationStatus.INVALID -> "❌ Dữ liệu đã bị thay đổi."
                VerificationStatus.NOT_FOUND -> "Link không còn tồn tại."
                VerificationStatus.NETWORK -> "Không thể kết nối — thử lại sau."
            }
            sendEvent(ShareViewerEvent.ShowMessage(msg))
        }
    }
}
