package com.example.betterme.presentation.share.sheet

import androidx.lifecycle.viewModelScope
import com.example.betterme.base.BaseMviViewModel
import com.example.betterme.domain.share.VerificationStatus
import com.example.betterme.domain.usecase.share.CreateShareUseCase
import com.example.betterme.domain.usecase.share.LoadSharedSnapshotUseCase
import kotlinx.coroutines.launch

/**
 * VM for the simplified Share Progress sheet. Re-entrancy-guarded by
 * checking for [ShareProgressUi.Publishing] before firing again.
 *
 * After a successful publish, the VM also re-reads the snapshot so
 * the share-card image renderer has the exact same data the public
 * viewer + profile screens see. One extra Firestore read per share —
 * keeps the image card honest without inventing data client-side.
 */
class ShareProgressViewModel(
    private val createShare: CreateShareUseCase,
    private val loadSnapshot: LoadSharedSnapshotUseCase
) : BaseMviViewModel<ShareProgressIntent, ShareProgressState, ShareProgressEvent>() {

    override fun initState(): ShareProgressState = ShareProgressState()

    override fun processIntent(intent: ShareProgressIntent) {
        when (intent) {
            ShareProgressIntent.Publish -> publish()
            ShareProgressIntent.Reset -> updateState {
                copy(ui = ShareProgressUi.Idle)
            }
        }
    }

    private fun publish() {
        if (currentState.ui is ShareProgressUi.Publishing) return
        viewModelScope.launch {
            updateState { copy(ui = ShareProgressUi.Publishing) }
            val link = runCatching { createShare() }.getOrElse { error ->
                val msg = error.message?.takeIf { it.isNotBlank() }
                    ?: "Không thể chia sẻ tiến độ. Vui lòng thử lại."
                updateState { copy(ui = ShareProgressUi.Error(msg)) }
                sendEvent(ShareProgressEvent.ShowMessage(msg))
                return@launch
            }
            // Surface Ready immediately so the link + text-share are
            // available; the image-share button stays disabled until
            // the snapshot read completes.
            updateState { copy(ui = ShareProgressUi.Ready(link = link, share = null)) }
            sendEvent(
                ShareProgressEvent.LaunchShareSheet(
                    subject = "Tiến độ BetterMe của tôi",
                    text = link.richMessage
                )
            )
            // Follow-up read so the image renderer has identical data
            // to the public viewer. viewModelScope cancels this if the
            // sheet closes mid-fetch.
            val result = runCatching { loadSnapshot(link.userId) }.getOrNull()
            if (result?.status == VerificationStatus.VALID && result.share != null) {
                val current = currentState.ui
                if (current is ShareProgressUi.Ready && current.link.userId == link.userId) {
                    updateState { copy(ui = current.copy(share = result.share)) }
                }
            }
        }
    }
}
