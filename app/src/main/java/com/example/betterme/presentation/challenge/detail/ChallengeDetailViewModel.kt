package com.example.betterme.presentation.challenge.detail

import androidx.compose.ui.graphics.Color
import androidx.lifecycle.viewModelScope
import com.example.betterme.base.BaseMviViewModel
import com.example.betterme.data.local.datastore.DataStoreManager
import com.example.betterme.data.local.room.entities.ChallengeEntity
import com.example.betterme.data.local.room.entities.UserChallengeEntity
import com.example.betterme.domain.repository.AchievementRepository
import com.example.betterme.domain.repository.ChallengeLogRepository
import com.example.betterme.domain.repository.ChallengeRepository
import com.example.betterme.domain.repository.ImageUploadRepository
import com.example.betterme.domain.repository.UserChallengeRepository
import com.example.betterme.domain.usecase.challenge.CheckInChallengeUseCase
import com.example.betterme.domain.usecase.challenge.JoinChallengeUseCase
import com.example.betterme.domain.usecase.challenge.LeaveChallengeUseCase
import com.example.betterme.domain.usecase.challenge.ScheduleChallengeReminderUseCase
import com.example.betterme.presentation.challenge.model.CelebrationUi
import com.example.betterme.presentation.challenge.model.DayCellUi
import com.example.betterme.presentation.challenge.model.DayStatus
import com.example.betterme.presentation.challenge.shared.Difficulty
import com.example.betterme.utils.DateUtils
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.Calendar

class ChallengeDetailViewModel(
    private val dataStoreManager: DataStoreManager,
    private val challengeRepository: ChallengeRepository,
    private val userChallengeRepository: UserChallengeRepository,
    private val challengeLogRepository: ChallengeLogRepository,
    private val achievementRepository: AchievementRepository,
    private val joinChallengeUseCase: JoinChallengeUseCase,
    private val leaveChallengeUseCase: LeaveChallengeUseCase,
    private val checkInChallengeUseCase: CheckInChallengeUseCase,
    private val scheduleReminderUseCase: ScheduleChallengeReminderUseCase,
    private val imageUploadRepository: ImageUploadRepository
) : BaseMviViewModel<ChallengeDetailIntent, ChallengeDetailState, ChallengeDetailEvent>() {

    override fun initState() = ChallengeDetailState()

    override fun processIntent(intent: ChallengeDetailIntent) {
        when (intent) {
            is ChallengeDetailIntent.LoadPreview -> loadPreview(intent.challengeId)
            is ChallengeDetailIntent.LoadActive -> loadActive(intent.userChallengeId)
            ChallengeDetailIntent.JoinChallenge -> join()
            ChallengeDetailIntent.StartCheckIn -> {
                updateState { copy(checkInStep = CheckInStep.Confirm, checkInTimestamp = System.currentTimeMillis()) }
                sendEvent(ChallengeDetailEvent.LaunchCamera)
                sendEvent(ChallengeDetailEvent.FetchLocation)
            }
            is ChallengeDetailIntent.PhotoCaptured -> updateState { copy(checkInPhotoUri = intent.uri) }
            is ChallengeDetailIntent.UpdateNote -> updateState { copy(checkInNote = intent.text.take(200)) }
            is ChallengeDetailIntent.SetLocation -> updateState {
                copy(
                    checkInLatitude = intent.lat,
                    checkInLongitude = intent.lng,
                    checkInLocationName = intent.name
                )
            }
            ChallengeDetailIntent.ConfirmCheckIn -> confirmCheckIn()
            ChallengeDetailIntent.DismissCheckIn -> updateState {
                copy(
                    checkInStep = CheckInStep.Idle,
                    checkInPhotoUri = null,
                    checkInNote = "",
                    checkInTimestamp = 0,
                    checkInLatitude = null,
                    checkInLongitude = null,
                    checkInLocationName = null
                )
            }
            ChallengeDetailIntent.DismissSuccess -> {
                updateState {
                    copy(
                        checkInStep = CheckInStep.Idle,
                        checkInPhotoUri = null,
                        checkInNote = "",
                        checkInTimestamp = 0
                    )
                }
                currentState.userChallengeId?.let { reloadActive(it) }
            }
            ChallengeDetailIntent.DismissCelebration -> {
                updateState { copy(celebration = null, mode = DetailMode.Completed) }
                currentState.userChallengeId?.let { reloadActive(it) }
            }
            ChallengeDetailIntent.LeaveChallenge -> leave()
            ChallengeDetailIntent.Share -> {
                val s = currentState
                val message = "Tôi vừa hoàn thành thử thách \"${s.title}\" và nhận được " +
                    "${s.rewardCoins} xu" +
                    if (s.rewardBadgeName != null) " + huy hiệu ${s.rewardBadgeName}!" else "!"
                sendEvent(ChallengeDetailEvent.LaunchShareSheet(message))
            }
        }
    }

    private fun loadPreview(challengeId: Int) {
        viewModelScope.launch { loadPreviewSuspending(challengeId) }
    }

    /** Suspending sibling of [loadPreview] used by callers that need to await completion. */
    private suspend fun loadPreviewSuspending(challengeId: Int) {
        updateState { copy(isLoading = true, challengeId = challengeId) }
        val challenge = challengeRepository.getById(challengeId)
        if (challenge == null) {
            updateState { copy(isLoading = false, errorMessage = "Không tìm thấy thử thách") }
            return
        }
        val userId = dataStoreManager.getCurrentUserId().first().orEmpty()
        // If already joined, prefer ACTIVE detail.
        val existing = if (userId.isNotBlank()) {
            userChallengeRepository.getByUserAndChallenge(userId, challengeId)
        } else null
        if (existing != null && existing.status == "ACTIVE") {
            loadActiveSuspending(existing.id)
            return
        }
        val rewardBadgeName = challenge.reward_badge_id?.let { achievementRepository.getById(it)?.title }
        updateState {
            copy(
                mode = DetailMode.Preview,
                isLoading = false,
                challengeId = challenge.id,
                userChallengeId = null,
                title = challenge.title,
                description = challenge.description,
                shortDescription = challenge.short_description,
                iconEmoji = challenge.icon_emoji,
                accentColor = parseColor(challenge.color_hex),
                difficulty = Difficulty.fromRaw(challenge.difficulty),
                rewardCoins = challenge.reward_coins,
                rewardBadgeName = rewardBadgeName,
                durationDays = challenge.duration_days,
                targetStreak = challenge.target_streak,
                descriptionBullets = challenge.toBullets(),
                isGroup = challenge.is_group,
                weekStrip = emptyWeek(),
                motivationalQuote = challenge.motivational_quote,
                completionMessage = challenge.completion_message
            )
        }
    }

    private fun loadActive(userChallengeId: Int) {
        viewModelScope.launch { loadActiveSuspending(userChallengeId) }
    }

    /**
     * Suspending sibling of [loadActive]. Lets callers like `join()` await the new state
     * so the bottom-bar lock stays held until the detail screen has fully transitioned to
     * Active mode — no flicker through "still in Preview" mid-recomposition.
     */
    private suspend fun loadActiveSuspending(userChallengeId: Int) {
        updateState { copy(isLoading = true, userChallengeId = userChallengeId) }
        val uc = userChallengeRepository.getById(userChallengeId)
        val challenge = uc?.challenge_id?.let { challengeRepository.getById(it) }
        if (uc == null || challenge == null) {
            updateState { copy(isLoading = false, errorMessage = "Không tìm thấy thử thách") }
            return
        }
        val rewardBadgeName = challenge.reward_badge_id?.let { achievementRepository.getById(it)?.title }
        val doneDates = challengeLogRepository.getDoneDates(userChallengeId)
        val weekStrip = buildWeekStrip(doneDates.toSet(), uc.start_date, challenge.target_streak)
        val daysRemaining = (challenge.target_streak - uc.current_streak).coerceAtLeast(0)
        val mode = when (uc.status) {
            "COMPLETED", "ABANDONED" -> DetailMode.Completed
            else -> DetailMode.Active
        }

        updateState {
            copy(
                mode = mode,
                isLoading = false,
                challengeId = challenge.id,
                userChallengeId = uc.id,
                title = challenge.title,
                description = challenge.description,
                shortDescription = challenge.short_description,
                iconEmoji = challenge.icon_emoji,
                accentColor = parseColor(challenge.color_hex),
                difficulty = Difficulty.fromRaw(challenge.difficulty),
                rewardCoins = challenge.reward_coins,
                rewardBadgeName = rewardBadgeName,
                durationDays = challenge.duration_days,
                targetStreak = challenge.target_streak,
                currentStreak = uc.current_streak,
                progressPct = uc.progress_pct,
                daysRemaining = daysRemaining,
                isGroup = challenge.is_group,
                weekStrip = weekStrip,
                descriptionBullets = challenge.toBullets(),
                motivationalQuote = challenge.motivational_quote,
                completionMessage = challenge.completion_message
            )
        }
    }

    private fun reloadActive(id: Int) {
        loadActive(id)
    }

    private fun join() {
        // Re-entrancy guard: ignore taps that arrive while a join is already in flight.
        if (currentState.isJoining) return

        viewModelScope.launch {
            val userId = dataStoreManager.getCurrentUserId().first().orEmpty()
            if (userId.isBlank()) {
                sendEvent(ChallengeDetailEvent.ShowMessage("Vui lòng đăng nhập"))
                return@launch
            }
            if (currentState.challengeId <= 0) {
                sendEvent(ChallengeDetailEvent.ShowMessage("Không tìm thấy thử thách"))
                return@launch
            }

            updateState { copy(isJoining = true) }
            try {
                val ucId = joinChallengeUseCase(userId, currentState.challengeId).toInt()
                // Reminder scheduling is best-effort — never block the join transition if
                // WorkManager refuses (e.g., on devices with restricted power policies).
                runCatching { scheduleReminderUseCase(ucId, currentState.title) }
                // Await the suspending variant so the lock stays held until state.mode
                // has fully transitioned to Active — no flash of "still in Preview" CTA.
                loadActiveSuspending(ucId)
                sendEvent(ChallengeDetailEvent.ShowMessage("Đã tham gia thử thách"))
            } catch (e: Exception) {
                android.util.Log.e("ChallengeDetailVM", "Join failed", e)
                sendEvent(
                    ChallengeDetailEvent.ShowMessage(
                        "Tham gia thử thách thất bại: ${e.message ?: "lỗi không xác định"}"
                    )
                )
            } finally {
                // Always release the lock, including paths where loadActive errored
                // after the row was already inserted.
                updateState { copy(isJoining = false) }
            }
        }
    }

    private fun confirmCheckIn() {
        val ucId = currentState.userChallengeId ?: return
        viewModelScope.launch {
            updateState { copy(isSavingCheckIn = true) }
            // Upload the photo to Cloudinary first (passthrough when not configured) so
            // the value we persist on the log is the durable URL, not a transient
            // FileProvider URI that becomes useless after the cache rotates.
            val resolvedImage = currentState.checkInPhotoUri?.let { uri ->
                imageUploadRepository.upload(
                    localUri = uri,
                    folder = ImageUploadRepository.Folder.ChallengeCheckIn
                )
            }
            val res = checkInChallengeUseCase(
                userChallengeId = ucId,
                note = currentState.checkInNote.ifBlank { null },
                imageUri = resolvedImage,
                latitude = currentState.checkInLatitude,
                longitude = currentState.checkInLongitude
            )
            updateState { copy(isSavingCheckIn = false) }
            when (res) {
                is CheckInChallengeUseCase.Result.Progress -> {
                    // Flip today's strip cell to Done immediately so the calendar reflects
                    // the new state without waiting for the success-sheet dismissal to
                    // trigger a full reload.
                    val refreshedStrip = currentState.weekStrip.map { day ->
                        if (day.isToday) day.copy(status = com.example.betterme.presentation.challenge.model.DayStatus.Done)
                        else day
                    }
                    val newRemaining = (currentState.targetStreak - res.newStreak).coerceAtLeast(0)
                    updateState {
                        copy(
                            currentStreak = res.newStreak,
                            progressPct = res.progressPct,
                            daysRemaining = newRemaining,
                            weekStrip = refreshedStrip,
                            checkInStep = CheckInStep.Success
                        )
                    }
                }
                is CheckInChallengeUseCase.Result.Completed -> {
                    updateState {
                        copy(
                            checkInStep = CheckInStep.Idle,
                            celebration = CelebrationUi(
                                challengeTitle = title,
                                coinsEarned = res.coinsEarned,
                                badgeName = res.rewardBadge?.title ?: rewardBadgeName
                            )
                        )
                    }
                }
                CheckInChallengeUseCase.Result.AlreadyCheckedIn -> {
                    updateState { copy(checkInStep = CheckInStep.Idle) }
                    sendEvent(ChallengeDetailEvent.ShowMessage("Hôm nay bạn đã check-in rồi"))
                }
                is CheckInChallengeUseCase.Result.Error -> {
                    updateState { copy(checkInStep = CheckInStep.Idle) }
                    sendEvent(ChallengeDetailEvent.ShowMessage(res.message))
                }
            }
        }
    }

    private fun leave() {
        val ucId = currentState.userChallengeId ?: return
        viewModelScope.launch {
            leaveChallengeUseCase(ucId)
            sendEvent(ChallengeDetailEvent.NavigateBack)
        }
    }

    // ---- helpers ----

    private fun ChallengeEntity.toBullets(): List<String> {
        // Generate 3 default bullets if no specific list is stored.
        return listOf(
            "Hoàn thành mỗi ngày trong $duration_days ngày liên tiếp",
            "Ghi lại check-in mỗi lần thực hiện",
            "Giữ chuỗi không bị gián đoạn"
        )
    }

    private fun buildWeekStrip(
        doneDates: Set<Long>,
        startDate: Long,
        targetStreak: Int
    ): List<DayCellUi> {
        val today = DateUtils.startOfDay()
        val labels = listOf("T2", "T3", "T4", "T5", "T6", "T7", "CN")
        // Build the 7 days centered around today (3 before, today, 3 after) for visual clarity.
        val cal = Calendar.getInstance().apply { timeInMillis = today; add(Calendar.DAY_OF_YEAR, -3) }
        return List(7) { i ->
            val day = DateUtils.startOfDay(cal.timeInMillis)
            val labelIdx = (cal.get(Calendar.DAY_OF_WEEK) + 5) % 7 // Mon=0..Sun=6
            val label = labels[labelIdx]
            val dateLabel = "${cal.get(Calendar.DAY_OF_MONTH)}/${cal.get(Calendar.MONTH) + 1}"
            val status = when {
                day == today -> if (day in doneDates) DayStatus.Done else DayStatus.Today
                day < today -> if (day in doneDates) DayStatus.Done else DayStatus.Missed
                else -> DayStatus.Future
            }
            cal.add(Calendar.DAY_OF_YEAR, 1)
            DayCellUi(
                label = label,
                dateLabel = dateLabel,
                status = status,
                isToday = day == today
            )
        }
    }

    private fun emptyWeek(): List<DayCellUi> {
        val labels = listOf("T2", "T3", "T4", "T5", "T6", "T7", "CN")
        return labels.map { DayCellUi(it, "", DayStatus.Future, false) }
    }

    private fun parseColor(hex: String): Color = try {
        Color(android.graphics.Color.parseColor(hex))
    } catch (e: Exception) {
        Color(0xFF0077FF)
    }
}
