package com.example.betterme.presentation.share.sheet

import androidx.lifecycle.viewModelScope
import com.example.betterme.base.BaseMviViewModel
import com.example.betterme.domain.usecase.share.CreateShareUseCase
import kotlinx.coroutines.launch

/**
 * VM for the simplified Share Progress sheet. Re-entrancy-guarded by
 * checking for [ShareProgressUi.Publishing] before firing again.
 */
class ShareProgressViewModel(
    private val createShare: CreateShareUseCase
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
            updateState { copy(ui = ShareProgressUi.Ready(link)) }
            sendEvent(
                ShareProgressEvent.LaunchShareSheet(
                    subject = "Tiến độ BetterMe của tôi",
                    text = link.richMessage
                )
            )
        }
    }
}
