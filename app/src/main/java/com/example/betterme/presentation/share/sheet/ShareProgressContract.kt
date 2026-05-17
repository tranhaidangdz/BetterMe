package com.example.betterme.presentation.share.sheet

import com.example.betterme.base.MviIntent
import com.example.betterme.base.MviSingleEvent
import com.example.betterme.base.MviViewState
import com.example.betterme.domain.share.ShareLink
import com.example.betterme.domain.share.VerifiedShare

/**
 * State machine for the "Chia sẻ tiến độ" bottom sheet — the entry
 * point that snapshots the user's full progress to Firestore and
 * hands the resulting deep link to the system share chooser.
 *
 * Always full-history. After Publish succeeds the VM also re-reads the
 * snapshot so the share-card image renderer has the exact same shape
 * the viewer + profile screens see.
 */
sealed class ShareProgressUi {
    /** Initial / idle. Sheet auto-fires Publish on first composition. */
    data object Idle : ShareProgressUi()
    /** Snapshot is being written to Firestore. */
    data object Publishing : ShareProgressUi()
    /**
     * Publish succeeded. [link] always present. [share] is null only
     * during the brief window between Firestore write success and the
     * follow-up read — the image-share button is gated on its presence.
     */
    data class Ready(val link: ShareLink, val share: VerifiedShare? = null) : ShareProgressUi()
    /** Auth missing / no check-ins / Firestore unreachable. */
    data class Error(val message: String) : ShareProgressUi()
}

data class ShareProgressState(
    val ui: ShareProgressUi = ShareProgressUi.Idle
) : MviViewState

sealed class ShareProgressIntent : MviIntent {
    /** Snapshot Room → Firestore. Auto-fired on sheet open. */
    data object Publish : ShareProgressIntent()
    /** Reset back to idle so the user can retry after Error. */
    data object Reset : ShareProgressIntent()
}

sealed class ShareProgressEvent : MviSingleEvent {
    /** Fire ACTION_SEND with the rendered rich message. */
    data class LaunchShareSheet(val subject: String, val text: String) : ShareProgressEvent()
    /** Reusable error toast. */
    data class ShowMessage(val message: String) : ShareProgressEvent()
}
