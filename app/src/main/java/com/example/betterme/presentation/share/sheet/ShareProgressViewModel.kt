package com.example.betterme.presentation.share.sheet

import androidx.lifecycle.viewModelScope
import com.example.betterme.base.BaseMviViewModel
import com.example.betterme.domain.usecase.share.CreateShareUseCase
import kotlinx.coroutines.launch

/**
 * Lives at-screen scope: created on sheet open, destroyed on close,
 * so a brand-new pick re-renders Picker without manual reset. The
 * single in-flight create call is re-entrancy-guarded by checking for
 * [ShareProgressUi.Generating] before firing again.
 */
class ShareProgressViewModel(
    private val createShare: CreateShareUseCase
) : BaseMviViewModel<ShareProgressIntent, ShareProgressState, ShareProgressEvent>() {

    override fun initState(): ShareProgressState = ShareProgressState()

    override fun processIntent(intent: ShareProgressIntent) {
        when (intent) {
            is ShareProgressIntent.CreateFor -> create(intent)
            ShareProgressIntent.Reset -> updateState {
                copy(ui = ShareProgressUi.Picker)
            }
        }
    }

    private fun create(intent: ShareProgressIntent.CreateFor) {
        if (currentState.ui is ShareProgressUi.Generating) return
        viewModelScope.launch {
            updateState { copy(ui = ShareProgressUi.Generating) }
            val link = runCatching { createShare(type = intent.type, itemId = intent.itemId) }
                .getOrElse { error ->
                    val msg = error.message?.takeIf { it.isNotBlank() }
                        ?: "Không thể tạo link chia sẻ. Vui lòng thử lại."
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
