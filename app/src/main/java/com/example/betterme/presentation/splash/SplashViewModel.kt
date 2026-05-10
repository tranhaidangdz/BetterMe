package com.example.betterme.presentation.splash

import android.util.Log
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.example.betterme.base.BaseMviViewModel
import com.example.betterme.data.local.datastore.DataStoreManager
import com.example.betterme.domain.usecase.challenge.ChallengeSeederUseCase
import com.example.betterme.presentation.splash.model.NextScreen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class SplashViewModel(
    private val dataStoreManager: DataStoreManager,
    private val firebaseAuth: FirebaseAuth,
    private val challengeSeederUseCase: ChallengeSeederUseCase
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

            // Seed challenges + badges + group teams on first launch (idempotent).
            try {
                challengeSeederUseCase()
            } catch (e: Exception) {
                Log.e("SplashVM", "Challenge seed failed", e)
            }

            val isFirstLaunch = dataStoreManager.isFirstTime().first()
            val currentUserId = dataStoreManager.getCurrentUserId().first()
            val hasSelectedHabits = dataStoreManager.hasSelectedHabits().first()
            val isGuestUser = dataStoreManager.isGuestUser().first()
            val hasGoogleSession = firebaseAuth.currentUser != null

            Log.d(
                "SplashVM",
                "isFirstLaunch=$isFirstLaunch, userId=$currentUserId, hasHabits=$hasSelectedHabits, isGuest=$isGuestUser, firebaseUser=${firebaseAuth.currentUser?.uid}"
            )

            val nextScreen = when {
                // Lần đầu mở app → Welcome
                isFirstLaunch -> NextScreen.WELCOME

                // Đã đăng nhập Google thì vào Main luôn (không ép chọn habits lại)
                hasGoogleSession -> NextScreen.MAIN

                // Đã có userId và đã chọn habits → Main
                currentUserId != null && hasSelectedHabits -> NextScreen.MAIN

                // Guest user chưa chọn habits thì vẫn vào bước chọn habits
                isGuestUser && currentUserId != null && !hasSelectedHabits -> NextScreen.HABIT_SELECTION

                // User local chưa đăng nhập Google và chưa đủ dữ liệu habits thì vào SignIn
                currentUserId != null && !hasSelectedHabits -> NextScreen.SIGN_IN

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