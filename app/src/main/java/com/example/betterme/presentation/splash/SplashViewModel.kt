package com.example.betterme.presentation.splash

import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.example.betterme.base.BaseMviViewModel
import com.example.betterme.data.local.datastore.DataStoreManager
import com.example.betterme.presentation.splash.model.NextScreen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class SplashViewModel(
    private val dataStoreManager: DataStoreManager,
    private val firebaseAuth: FirebaseAuth
) : BaseMviViewModel<SplashIntent, SplashState, SplashEvent>() {

    override fun initState(): SplashState {
        return SplashState()
    }

    override fun processIntent(intent: SplashIntent) {
        when (intent) {
            SplashIntent.CheckFirstLaunch -> handleCheckFirstLaunch()
        }
    }

    private fun handleCheckFirstLaunch() {
        viewModelScope.launch(Dispatchers.IO) {
            delay(1500) // Fake load data 1.5s


            val isFirstLaunch = dataStoreManager.isFirstTime().first()

            val nextScreen = when {
                isFirstLaunch -> NextScreen.WELCOME
                firebaseAuth.currentUser != null -> NextScreen.MAIN
                else -> NextScreen.SIGN_IN
            }

            updateState { copy(nextScreen = nextScreen) }

            sendEvent(
                when (nextScreen) {
                    NextScreen.WELCOME -> SplashEvent.NavigateToWelcome
                    NextScreen.MAIN -> SplashEvent.NavigateToMain
                    NextScreen.SIGN_IN -> SplashEvent.NavigateToSignIn
                }
            )
        }
    }
}