package com.example.betterme.presentation.signin

import android.app.Activity
import com.example.betterme.base.MviIntent
import com.example.betterme.base.MviSingleEvent
import com.example.betterme.base.MviViewState

data class SignInState(
    val isLoading: Boolean = false
) : MviViewState

sealed class SignInIntent : MviIntent {
    data class SignInWithGoogle(val activity: Activity) : SignInIntent()
    data object SkipSignIn : SignInIntent()
}

sealed class SignInEvent : MviSingleEvent {
    data object NavigateToHome : SignInEvent()
    data object LoginError : SignInEvent()
}
