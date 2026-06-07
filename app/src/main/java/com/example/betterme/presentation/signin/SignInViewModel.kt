package com.example.betterme.presentation.signin

import android.app.Activity
import android.util.Log
import androidx.lifecycle.viewModelScope
import androidx.work.ExistingWorkPolicy
import androidx.work.WorkManager
import com.google.firebase.auth.FirebaseAuth
import com.example.betterme.base.BaseMviViewModel
import com.example.betterme.data.local.datastore.DataStoreManager
import com.example.betterme.data.provider.GoogleAuthClient
import com.example.betterme.data.worker.SyncWorker
import com.example.betterme.domain.model.User
import com.example.betterme.domain.usecase.user.GetUserUseCase
import com.example.betterme.domain.usecase.user.SaveUserUseCase
import com.example.betterme.data.local.room.entities.UserEntity
import com.example.betterme.domain.repository.UserRepository
import kotlinx.coroutines.launch
import java.util.UUID

class SignInViewModel(
    private val googleAuthClient: GoogleAuthClient,
    private val saveUserUseCase: SaveUserUseCase,
    private val firebaseAuth: FirebaseAuth,
    private val getUserUseCase: GetUserUseCase,
    private val dataStoreManager: DataStoreManager,
    private val userRepository: UserRepository,
    private val workManager: WorkManager,
) : BaseMviViewModel<SignInIntent, SignInState, SignInEvent>() {
    override fun initState(): SignInState = SignInState()

    override fun processIntent(intent: SignInIntent) {
        when (intent) {
            is SignInIntent.SignInWithGoogle -> {
                handleSignInWithGoogle(intent.activity)
            }
            is SignInIntent.SkipSignIn -> {
                handleSkipSignIn()
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
                        // User mới → tạo và lưu → navigate tới HabitSelection
                        val newUser = User(
                            id = currentUser.uid,
                            name = currentUser.displayName.orEmpty(),
                            photoUrl = currentUser.photoUrl?.toString().orEmpty(),
                            email = currentUser.email.orEmpty()
                        )
                        saveUserUseCase(newUser)
                        triggerSyncAfterLogin()
                        sendEvent(SignInEvent.NavigateToHabitSelection)
                    } else {
                        // User đã tồn tại → lưu vào DataStore → navigate tới Home
                        dataStoreManager.saveUserInfo(existingUser)
                        triggerSyncAfterLogin()
                        sendEvent(SignInEvent.NavigateToHome)
                    }
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

    /**
     * Kick the sync layer the moment the user enters the app post-login. The
     * KoinApp.scheduleSync() one-shot already ran at process launch (before the
     * uid was known), so without this call the first pull/push would have to
     * wait up to 30 minutes for the periodic SyncWorker. REPLACE policy
     * preempts any in-flight launch one-shot — the now-signed-in pass supersedes
     * it. WorkManager enforces the network constraint, so if the device is
     * offline the worker queues for connectivity-resume and we fall back to
     * Firestore's local cache in the meantime.
     */
    private fun triggerSyncAfterLogin() {
        try {
            workManager.enqueueUniqueWork(
                SyncWorker.ONE_SHOT_UNIQUE_NAME,
                ExistingWorkPolicy.REPLACE,
                SyncWorker.oneShotRequest()
            )
        } catch (e: Exception) {
            // Failing to enqueue a worker must never block sign-in; the periodic
            // worker will still catch up within 30 minutes.
            Log.w("SignIn", "Failed to enqueue post-login sync", e)
        }
    }

    private fun handleSkipSignIn() {
        viewModelScope.launch {
            updateState { copy(isLoading = true) }
            try {
                // Tạo guest userId riêng
                val guestId = "guest_${UUID.randomUUID()}"

                // Tạo record trong bảng users để đảm bảo ForeignKey constraint
                val guestEntity = UserEntity(
                    id = guestId,
                    name = "Guest",
                    email = "",
                    photoUrl = "",
                    created_at = System.currentTimeMillis()
                )
                userRepository.insertUser(guestEntity)

                // Lưu guest info vào DataStore
                dataStoreManager.saveGuestUser(guestId)

                // Navigate tới HabitSelection
                sendEvent(SignInEvent.NavigateToHabitSelection)
            } catch (e: Exception) {
                e.printStackTrace()
                sendEvent(SignInEvent.LoginError)
            } finally {
                updateState { copy(isLoading = false) }
            }
        }
    }
}
