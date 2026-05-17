package com.example.betterme.presentation.share.viewer

import com.example.betterme.base.MviIntent
import com.example.betterme.base.MviSingleEvent
import com.example.betterme.base.MviViewState
import com.example.betterme.domain.share.VerificationStatus
import com.example.betterme.domain.share.VerifiedShare

/**
 * State machine for the public viewer screen. Reached two ways:
 *   - Deep link `betterme://share/{shareId}` (most external traffic).
 *   - In-app preview right after the user creates a share.
 *
 * The viewer's invariant: render NOTHING from local state. Every
 * field on screen comes from the server-verified [VerifiedShare].
 */
sealed class ShareViewerUi {
    data object Loading : ShareViewerUi()
    data class Verified(val share: VerifiedShare) : ShareViewerUi()
    data class Invalid(val reason: VerificationStatus) : ShareViewerUi()
}

data class ShareViewerState(
    val ui: ShareViewerUi = ShareViewerUi.Loading,
    val shareId: String = "",
    val isReverifying: Boolean = false
) : MviViewState

sealed class ShareViewerIntent : MviIntent {
    data class Load(val shareId: String) : ShareViewerIntent()
    /** User tapped "Xác minh lại" — re-checks the signature without re-downloading the snapshot. */
    data object ReVerify : ShareViewerIntent()
}

sealed class ShareViewerEvent : MviSingleEvent {
    data class ShowMessage(val message: String) : ShareViewerEvent()
}
