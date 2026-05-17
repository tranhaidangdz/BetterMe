package com.example.betterme.utils

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * App-wide one-shot deep-link bus. The activity's intent handlers emit into this; UI layers
 * (MainScreen) collect and react.
 *
 * Used to wire notification taps → ChallengeDetail / ChallengePreview overlays without
 * having to thread the intent through composable params.
 *
 * Registered as a Koin singleton in `appModule`.
 */
class DeepLinkBus {

    sealed class Event {
        data class OpenUserChallenge(val userChallengeId: Int) : Event()
        data class OpenChallengePreview(val challengeId: Int) : Event()
        /** Notification tap → open Habit Detail for the given habitId. */
        data class OpenHabitDetail(val habitId: Int) : Event()
        /** External deep link `betterme://share/{userId}` — opens the
         *  verified share viewer overlay. */
        data class OpenShareViewer(val userId: String) : Event()
        /** External deep link `betterme://profile/{userId}` — opens
         *  the public profile overlay (same data, profile layout). */
        data class OpenPublicProfile(val userId: String) : Event()
    }

    private val _events = MutableSharedFlow<Event>(extraBufferCapacity = 4)
    val events = _events.asSharedFlow()

    /** Non-suspend emit using tryEmit (buffer is sized to handle bursts). */
    fun publish(event: Event) {
        _events.tryEmit(event)
    }
}
