package com.example.betterme.presentation.splash

import android.util.Log
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
            delay(1500)

            val isFirstLaunch = dataStoreManager.isFirstTime().first()
            val currentUserId = dataStoreManager.getCurrentUserId().first()
            val hasSelectedHabits = dataStoreManager.hasSelectedHabits().first()

            Log.d("SplashVM", "isFirstLaunch=$isFirstLaunch, userId=$currentUserId, hasHabits=$hasSelectedHabits, firebaseUser=${firebaseAuth.currentUser?.uid}")

            val nextScreen = when {
                // Lần đầu mở app → Welcome
                isFirstLaunch -> NextScreen.WELCOME

                // Đã có userId và đã chọn habits → Main
                currentUserId != null && hasSelectedHabits -> NextScreen.MAIN

                // Đã có userId nhưng chưa chọn habits → HabitSelection
                currentUserId != null && !hasSelectedHabits -> NextScreen.HABIT_SELECTION

                // Đã qua onboarding nhưng chưa có userId → SignIn
                firebaseAuth.currentUser != null -> NextScreen.MAIN

                // Không có dữ liệu user nào (có thể do Auto Backup restore IS_FIRST_TIME=false)
                // → Coi như lần đầu, hiện Welcome/Onboarding lại
                else -> NextScreen.WELCOME
            }

            Log.d("SplashVM", "nextScreen=$nextScreen")

            updateState { copy(nextScreen = nextScreen) }

            sendEvent(
                when (nextScreen) {
                    NextScreen.WELCOME -> SplashEvent.NavigateToWelcome
                    NextScreen.MAIN -> SplashEvent.NavigateToMain
                    NextScreen.SIGN_IN -> SplashEvent.NavigateToSignIn
                    NextScreen.HABIT_SELECTION -> SplashEvent.NavigateToHabitSelection
                }
            )
        }
    }
}