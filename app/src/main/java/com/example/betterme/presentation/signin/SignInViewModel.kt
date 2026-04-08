package com.example.betterme.presentation.signin

import android.app.Activity
import android.util.Log
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.example.betterme.base.BaseMviViewModel
import com.example.betterme.data.local.datastore.DataStoreManager
import com.example.betterme.data.provider.GoogleAuthClient
import com.example.betterme.domain.model.User
import com.example.betterme.domain.usecase.user.GetUserUseCase
import com.example.betterme.domain.usecase.user.SaveUserUseCase
import kotlinx.coroutines.launch

class SignInViewModel(
    private val googleAuthClient: GoogleAuthClient,
    private val saveUserUseCase: SaveUserUseCase,
    private val firebaseAuth: FirebaseAuth,
    private val getUserUseCase: GetUserUseCase,
    private val dataStoreManager: DataStoreManager,
) : BaseMviViewModel<SignInIntent, SignInState, SignInEvent>() {
    override fun initState(): SignInState = SignInState()

    override fun processIntent(intent: SignInIntent) {
        when (intent) {
            is SignInIntent.SignInWithGoogle -> {
                handleSignInWithGoogle(intent.activity)
            }
            is SignInIntent.SkipSignIn -> {
                sendEvent(SignInEvent.NavigateToHome)
            }
        }
    }

    private fun handleSignInWithGoogle(activity: Activity) {
        viewModelScope.launch {
            updateState { copy(isLoading = true) }
            try {
                val isSuccess = googleAuthClient.signIn(activity)
                if (!isSuccess) {
                    sendEvent(SignInEvent.LoginError)
                    return@launch
                }

                val currentUser = firebaseAuth.currentUser
                if (currentUser == null) {
                    googleAuthClient.signOut()
                    sendEvent(SignInEvent.LoginError)
                    Log.d("SignIn", "Current user is null")
                    return@launch
                }

                val result = getUserUseCase(currentUser.uid)

                result.onSuccess { existingUser ->
                    if (existingUser == null) {
                        // User mới → tạo và lưu
                        val newUser = User(
                            id = currentUser.uid,
                            name = currentUser.displayName.orEmpty(),
                            photoUrl = currentUser.photoUrl?.toString().orEmpty(),
                            email = currentUser.email.orEmpty(),
                            rankId = "a1"
                        )
                        saveUserUseCase(newUser)
                    } else {
                        // User đã tồn tại → lưu vào DataStore
                        dataStoreManager.saveUserInfo(existingUser)
                    }
                    sendEvent(SignInEvent.NavigateToHome)
                }.onFailure {
                    Log.d("SignIn", "Get user failed: ${it.message}")
                    sendEvent(SignInEvent.LoginError)
                    googleAuthClient.signOut()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                sendEvent(SignInEvent.LoginError)
                googleAuthClient.signOut()
            } finally {
                updateState { copy(isLoading = false) }
            }
        }
    }
}
