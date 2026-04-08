package com.example.betterme.presentation.splash

import com.example.betterme.base.MviIntent
import com.example.betterme.base.MviSingleEvent
import com.example.betterme.base.MviViewState
import com.example.betterme.presentation.splash.model.NextScreen


data class SplashState(
    val isSignedIn: Boolean = false,
    val nextScreen: NextScreen? = null
) : MviViewState

sealed class SplashIntent : MviIntent {
    data object CheckFirstLaunch : SplashIntent()
}

sealed class SplashEvent : MviSingleEvent {
    data object NavigateToWelcome : SplashEvent()
    data object NavigateToMain : SplashEvent()
    data object NavigateToSignIn : SplashEvent()
}