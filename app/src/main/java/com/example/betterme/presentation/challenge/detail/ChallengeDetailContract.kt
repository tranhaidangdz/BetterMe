package com.example.betterme.presentation.challenge.detail

import android.net.Uri
import com.example.betterme.base.MviIntent
import com.example.betterme.base.MviSingleEvent
import com.example.betterme.base.MviViewState
import com.example.betterme.presentation.challenge.detail.components.CheckInHistoryItemUi
import com.example.betterme.presentation.challenge.detail.components.MilestoneUi
import com.example.betterme.presentation.challenge.model.CelebrationUi
import com.example.betterme.presentation.challenge.model.DayCellUi
import com.example.betterme.presentation.challenge.shared.Difficulty
import androidx.compose.ui.graphics.Color

enum class DetailMode { Preview, Active, Completed }

enum class CheckInStep { Idle, Confirm, Success }

data class ChallengeDetailState(
    val mode: DetailMode = DetailMode.Preview,
    val isLoading: Boolean = true,
    val challengeId: Int = 0,
    val userChallengeId: Int? = null,
    val title: String = "",
    val description: String = "",
    val iconEmoji: String = "🏆",
    val accentColor: Color = Color(0xFF0077FF),
    val difficulty: Difficulty = Difficulty.EASY,
    val rewardCoins: Int = 0,
    val rewardBadgeName: String? = null,
    val durationDays: Int = 0,
    val targetStreak: Int = 0,
    val currentStreak: Int = 0,
    val progressPct: Int = 0,
    val daysRemaining: Int = 0,
    val isGroup: Boolean = false,
    val weekStrip: List<DayCellUi> = emptyList(),
    val descriptionBullets: List<String> = emptyList(),

    // Curated copy from ChallengesSeed (production catalog)
    val motivationalQuote: String = "",
    val completionMessage: String = "",

    // Detail-flow extras
    val milestones: List<MilestoneUi> = emptyList(),
    val checkInHistory: List<CheckInHistoryItemUi> = emptyList(),
    val reminderTimeLabel: String = "08:00",
    val estimatedCompletionLabel: String = "—",
    val bestStreak: Int = 0,

    // Check-in flow
    val checkInStep: CheckInStep = CheckInStep.Idle,
    val checkInPhotoUri: Uri? = null,
    val checkInNote: String = "",
    val checkInTimestamp: Long = 0,
    val checkInLatitude: Double? = null,
    val checkInLongitude: Double? = null,
    val checkInLocationName: String? = null,
    val isSavingCheckIn: Boolean = false,
    val celebration: CelebrationUi? = null,

    // True while a Join is in-flight. Drives the bottom-bar lock so a tap-storm cannot
    // trigger multiple Join coroutines before the first one finishes.
    val isJoining: Boolean = false,

    val errorMessage: String? = null
) : MviViewState

sealed class ChallengeDetailIntent : MviIntent {
    data class LoadPreview(val challengeId: Int) : ChallengeDetailIntent()
    data class LoadActive(val userChallengeId: Int) : ChallengeDetailIntent()
    data object JoinChallenge : ChallengeDetailIntent()
    data object StartCheckIn : ChallengeDetailIntent()
    data class PhotoCaptured(val uri: Uri) : ChallengeDetailIntent()
    data class UpdateNote(val text: String) : ChallengeDetailIntent()
    data class SetLocation(val lat: Double, val lng: Double, val name: String?) : ChallengeDetailIntent()
    data object ConfirmCheckIn : ChallengeDetailIntent()
    data object DismissCheckIn : ChallengeDetailIntent()
    data object DismissSuccess : ChallengeDetailIntent()
    data object DismissCelebration : ChallengeDetailIntent()
    data object LeaveChallenge : ChallengeDetailIntent()
    data object Share : ChallengeDetailIntent()
    data class ChangeReminderTime(val hour: Int, val minute: Int) : ChallengeDetailIntent()
}

sealed class ChallengeDetailEvent : MviSingleEvent {
    data object LaunchCamera : ChallengeDetailEvent()
    data object FetchLocation : ChallengeDetailEvent()
    data class ShowMessage(val text: String) : ChallengeDetailEvent()
    data class LaunchShareSheet(val message: String) : ChallengeDetailEvent()
    data object NavigateBack : ChallengeDetailEvent()
}
