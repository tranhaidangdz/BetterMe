package com.example.betterme.presentation.share.viewer

import com.example.betterme.base.MviIntent
import com.example.betterme.base.MviSingleEvent
import com.example.betterme.base.MviViewState
import com.example.betterme.domain.share.VerificationStatus
import com.example.betterme.domain.share.VerifiedShare

/**
 * State machine for the public viewer screen. Reached two ways:
 *   - Deep link `betterme://share/{userId}`.
 *   - In-app preview right after the user publishes their snapshot.
 *
 * "Verified" here means the snapshot doc exists at
 * `/shared_progress/{userId}` in Firestore. The viewer never renders
 * local Room data — everything on screen comes from the Firestore
 * round trip.
 */
sealed class ShareViewerUi {
    data object Loading : ShareViewerUi()
    data class Verified(val share: VerifiedShare) : ShareViewerUi()
    data class Invalid(val reason: VerificationStatus) : ShareViewerUi()
}

data class ShareViewerState(
    val ui: ShareViewerUi = ShareViewerUi.Loading,
    val userId: String = ""
) : MviViewState

sealed class ShareViewerIntent : MviIntent {
    data class Load(val userId: String) : ShareViewerIntent()
}

sealed class ShareViewerEvent : MviSingleEvent {
    data class ShowMessage(val message: String) : ShareViewerEvent()
}
