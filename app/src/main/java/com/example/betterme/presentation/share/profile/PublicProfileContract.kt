package com.example.betterme.presentation.share.profile

import com.example.betterme.base.MviIntent
import com.example.betterme.base.MviSingleEvent
import com.example.betterme.base.MviViewState
import com.example.betterme.domain.share.VerificationStatus
import com.example.betterme.domain.share.VerifiedShare

/**
 * State machine for the public profile screen reached via
 * `betterme://profile/{userId}` or in-app navigation. Reads the same
 * `/shared_progress/{userId}` doc the share viewer uses but renders
 * it as a polished "public profile" surface (hero avatar, stat row,
 * verified badge prominently below the name).
 */
sealed class PublicProfileUi {
    data object Loading : PublicProfileUi()
    data class Loaded(val share: VerifiedShare) : PublicProfileUi()
    data class Missing(val reason: VerificationStatus) : PublicProfileUi()
}

data class PublicProfileState(
    val ui: PublicProfileUi = PublicProfileUi.Loading,
    val userId: String = ""
) : MviViewState

sealed class PublicProfileIntent : MviIntent {
    data class Load(val userId: String) : PublicProfileIntent()
}

sealed class PublicProfileEvent : MviSingleEvent
