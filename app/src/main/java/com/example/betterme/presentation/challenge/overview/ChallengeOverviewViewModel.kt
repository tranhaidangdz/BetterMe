package com.example.betterme.presentation.challenge.overview

import androidx.compose.ui.graphics.Color
import androidx.lifecycle.viewModelScope
import com.example.betterme.base.BaseMviViewModel
import com.example.betterme.data.local.datastore.DataStoreManager
import com.example.betterme.data.local.room.entities.ChallengeEntity
import com.example.betterme.data.local.room.entities.UserChallengeEntity
import com.example.betterme.data.local.room.relation.UserChallengeWithDetails
import com.example.betterme.domain.repository.AchievementRepository
import com.example.betterme.domain.repository.ChallengeLogRepository
import com.example.betterme.domain.repository.ChallengeRepository
import com.example.betterme.domain.repository.ReminderRepository
import com.example.betterme.domain.repository.UserChallengeRepository
import com.example.betterme.domain.usecase.challenge.ToggleStartReminderUseCase
import com.example.betterme.presentation.challenge.model.ChallengeProgressUiModel
import com.example.betterme.presentation.challenge.model.CompletedChallengeUiModel
import com.example.betterme.presentation.challenge.model.OverviewFilter
import com.example.betterme.presentation.challenge.model.OverviewStatsUi
import com.example.betterme.presentation.challenge.model.UpcomingChallengeUiModel
import com.example.betterme.presentation.challenge.shared.Difficulty
import com.example.betterme.utils.DateUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ChallengeOverviewViewModel(
    private val dataStoreManager: DataStoreManager,
    private val userChallengeRepository: UserChallengeRepository,
    private val challengeRepository: ChallengeRepository,
    private val achievementRepository: AchievementRepository,
    private val challengeLogRepository: ChallengeLogRepository,
    private val reminderRepository: ReminderRepository,
    private val toggleStartReminderUseCase: ToggleStartReminderUseCase
) : BaseMviViewModel<ChallengeOverviewIntent, ChallengeOverviewState, ChallengeOverviewEvent>() {

    private val reminderEnabledIds = MutableStateFlow<Set<Int>>(emptySet())

    override fun initState() = ChallengeOverviewState()

    init {
        processIntent(ChallengeOverviewIntent.Load)
    }

    override fun processIntent(intent: ChallengeOverviewIntent) {
        when (intent) {
            ChallengeOverviewIntent.Load -> load()
            is ChallengeOverviewIntent.SelectFilter -> updateState { copy(selectedFilter = intent.filter) }
            is ChallengeOverviewIntent.ToggleStartReminder -> toggleReminder(intent.challengeId)
        }
    }

    private fun load() {
        viewModelScope.launch {
            val userId = dataStoreManager.getCurrentUserId().first().orEmpty()
            if (userId.isBlank()) {
                updateState { copy(isLoading = false) }
                return@launch
            }

            // Hydrate the in-memory reminder-enabled set from Room so the bell on each
            // upcoming row reflects the persisted state on screen entry. Without this,
            // a user who armed a reminder in a previous session would see the bell as
            // "off" until they re-tapped it.
            reminderEnabledIds.value = reminderRepository
                .getAllActiveOfType("CHALLENGE_START")
                .map { it.target_id }
                .toSet()

            val joinedFlow = userChallengeRepository.observeWithDetails(userId)
            val upcomingChallengesFlow = challengeRepository.observeAll().map { all ->
                val now = System.currentTimeMillis()
                all.filter { (it.start_date ?: 0L) > now }
            }

            combine(joinedFlow, upcomingChallengesFlow, reminderEnabledIds) { joined, upcomingPool, reminderIds ->
                Triple(joined, upcomingPool, reminderIds)
            }.collect { (joined, upcomingPool, reminderIds) ->
                val today = DateUtils.startOfDay()
                val active = joined.filter { it.userChallenge.status == "ACTIVE" }
                    .map { details ->
                        val log = challengeLogRepository.getLogByDate(details.userChallenge.id, today)
                        details.toProgressUi(isCheckedInToday = log?.status == "DONE")
                    }
                val completed = joined.filter {
                    it.userChallenge.status == "COMPLETED" || it.userChallenge.status == "ABANDONED"
                }.map { it.toCompletedUi(achievementRepository) }
                val upcoming = upcomingPool.map { it.toUpcomingUi(reminderIds.contains(it.id)) }

                val joinedCount = joined.size
                val completedCount = joined.count { it.userChallenge.status == "COMPLETED" }
                val rate = if (joinedCount > 0) (completedCount * 100) / joinedCount else 0

                updateState {
                    copy(
                        isLoading = false,
                        active = active,
                        upcoming = upcoming,
                        completed = completed,
                        stats = OverviewStatsUi(joinedCount, completedCount, rate)
                    )
                }
            }
        }
    }

    private fun toggleReminder(challengeId: Int) {
        viewModelScope.launch {
            val newState = toggleStartReminderUseCase(challengeId)
            reminderEnabledIds.value = if (newState) {
                reminderEnabledIds.value + challengeId
            } else {
                reminderEnabledIds.value - challengeId
            }
            sendEvent(
                ChallengeOverviewEvent.ShowMessage(
                    if (newState) "Đã bật nhắc thử thách" else "Đã tắt nhắc thử thách"
                )
            )
        }
    }

    // ---- mappers ----

    private fun UserChallengeWithDetails.toProgressUi(isCheckedInToday: Boolean): ChallengeProgressUiModel {
        val c = challenge
        val daysRemaining = (c.target_streak - userChallenge.current_streak).coerceAtLeast(0)
        return ChallengeProgressUiModel(
            userChallengeId = userChallenge.id,
            challengeId = c.id,
            title = c.title,
            iconEmoji = c.icon_emoji,
            accentColor = parseColor(c.color_hex),
            difficulty = Difficulty.fromRaw(c.difficulty),
            currentStreak = userChallenge.current_streak,
            targetStreak = c.target_streak,
            progressPct = userChallenge.progress_pct,
            rewardCoins = c.reward_coins,
            rewardBadgeName = null, // resolved by detail screen if needed
            isGroup = c.is_group,
            daysRemaining = daysRemaining,
            isCheckedInToday = isCheckedInToday
        )
    }

    private suspend fun UserChallengeWithDetails.toCompletedUi(
        achievementRepo: AchievementRepository
    ): CompletedChallengeUiModel {
        val c = challenge
        val finishedDate = userChallenge.end_date ?: System.currentTimeMillis()
        val isCompleted = userChallenge.status == "COMPLETED"
        val rewardBadgeName = c.reward_badge_id?.let { id ->
            achievementRepo.getById(id)?.title
        }
        return CompletedChallengeUiModel(
            userChallengeId = userChallenge.id,
            challengeId = c.id,
            title = c.title,
            iconEmoji = c.icon_emoji,
            accentColor = parseColor(c.color_hex),
            isCompleted = isCompleted,
            finishedDateLabel = SimpleDateFormat("dd/MM/yyyy", Locale("vi")).format(Date(finishedDate)),
            rewardCoins = if (isCompleted) c.reward_coins else 0,
            rewardBadgeName = if (isCompleted) rewardBadgeName else null
        )
    }

    private fun ChallengeEntity.toUpcomingUi(reminderEnabled: Boolean): UpcomingChallengeUiModel {
        val now = System.currentTimeMillis()
        val days = DateUtils.daysBetween(now, start_date ?: now).coerceAtLeast(0)
        return UpcomingChallengeUiModel(
            challengeId = id,
            title = title,
            iconEmoji = icon_emoji,
            accentColor = parseColor(color_hex),
            difficulty = Difficulty.fromRaw(difficulty),
            rewardCoins = reward_coins,
            daysUntilStart = days,
            reminderEnabled = reminderEnabled
        )
    }

    private fun parseColor(hex: String): Color = try {
        Color(android.graphics.Color.parseColor(hex))
    } catch (e: Exception) {
        Color(0xFF0077FF)
    }
}
