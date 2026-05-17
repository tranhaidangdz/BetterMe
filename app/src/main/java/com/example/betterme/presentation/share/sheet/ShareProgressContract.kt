package com.example.betterme.presentation.share.sheet

import com.example.betterme.base.MviIntent
import com.example.betterme.base.MviSingleEvent
import com.example.betterme.base.MviViewState
import com.example.betterme.domain.share.ShareLink
import com.example.betterme.domain.share.ShareType

/**
 * State machine for the "Chia sẻ tiến độ" bottom sheet — the entry
 * point that lets the user pick a slice (full / habit / challenge),
 * have the server sign + store the snapshot, and hand the resulting
 * URLs to the system share chooser.
 */
sealed class ShareProgressUi {
    /** Slice picker. Default state when the sheet opens. */
    data object Picker : ShareProgressUi()
    /** Server round-trip is in flight. */
    data object Generating : ShareProgressUi()
    /** Backend returned a signed snapshot. URLs ready to hand off. */
    data class Ready(val link: ShareLink) : ShareProgressUi()
    /** Anything that went wrong — auth missing, network down, empty data. */
    data class Error(val message: String) : ShareProgressUi()
}

data class ShareProgressState(
    val ui: ShareProgressUi = ShareProgressUi.Picker
) : MviViewState

sealed class ShareProgressIntent : MviIntent {
    /** Open with a pre-selected type, e.g. from Habit Detail's share button. */
    data class CreateFor(
        val type: ShareType,
        val itemId: String? = null
    ) : ShareProgressIntent()
    /** Reset back to picker after a Ready / Error so the sheet can try again. */
    data object Reset : ShareProgressIntent()
}

sealed class ShareProgressEvent : MviSingleEvent {
    /** Fire ACTION_SEND with the rendered rich message. */
    data class LaunchShareSheet(val subject: String, val text: String) : ShareProgressEvent()
    /** Reusable error toast/snackbar. */
    data class ShowMessage(val message: String) : ShareProgressEvent()
}
