package com.example.betterme.presentation.challenge.achievements

import androidx.compose.ui.graphics.Color
import androidx.lifecycle.viewModelScope
import com.example.betterme.base.BaseMviViewModel
import com.example.betterme.data.local.datastore.DataStoreManager
import com.example.betterme.domain.repository.AchievementRepository
import com.example.betterme.domain.repository.ChallengeLogRepository
import com.example.betterme.domain.repository.UserAchievementRepository
import com.example.betterme.domain.repository.UserChallengeRepository
import com.example.betterme.domain.repository.UserRepository
import com.example.betterme.presentation.challenge.model.AchievementHighlightUi
import com.example.betterme.presentation.challenge.model.AchievementsHeaderUi
import com.example.betterme.presentation.challenge.model.BadgeUiModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class ChallengeAchievementsViewModel(
    private val dataStoreManager: DataStoreManager,
    private val userRepository: UserRepository,
    private val userChallengeRepository: UserChallengeRepository,
    private val challengeLogRepository: ChallengeLogRepository,
    private val achievementRepository: AchievementRepository,
    private val userAchievementRepository: UserAchievementRepository
) : BaseMviViewModel<ChallengeAchievementsIntent, ChallengeAchievementsState, ChallengeAchievementsEvent>() {

    override fun initState() = ChallengeAchievementsState()

    init {
        processIntent(ChallengeAchievementsIntent.Load)
    }

    override fun processIntent(intent: ChallengeAchievementsIntent) {
        when (intent) {
            ChallengeAchievementsIntent.Load -> load()
        }
    }

    private fun load() {
        viewModelScope.launch {
            val userId = dataStoreManager.getCurrentUserId().first().orEmpty()
            if (userId.isBlank()) {
                updateState { copy(isLoading = false) }
                return@launch
            }
            val user = userRepository.getUserById(userId)
            val badgeCount = userAchievementRepository.countByUser(userId)
            val challengeCount = userChallengeRepository.countCompletedByUser(userId)
            val totalCheckIns = challengeLogRepository.countTotalCheckInsByUser(userId)
            val maxStreak = userChallengeRepository.maxBestStreak(userId) ?: 0
            val activeCount = userChallengeRepository.countActiveByUser(userId)

            val earnedAchievements = userAchievementRepository.observeByUser(userId).first()
            val earnedById = earnedAchievements.associateBy { it.achievement_id }
            val featured = achievementRepository.observeAll().first()
                .filter { earnedById.containsKey(it.id) }
                .take(4)
                .map {
                    BadgeUiModel(
                        id = it.id,
                        name = it.title,
                        description = it.description,
                        iconRes = it.icon,
                        iconEmoji = it.icon_emoji.ifBlank { "🏅" },
                        imageUrl = it.image_url,
                        accentColor = parseColor(it.color_hex),
                        isEarned = true
                    )
                }

            val header = AchievementsHeaderUi(
                name = user?.name.orEmpty(),
                photoUrl = user?.photoUrl.orEmpty(),
                level = user?.level ?: 1,
                badgeCount = badgeCount,
                challengeCount = challengeCount,
                totalCoins = user?.coins ?: 0
            )

            val highlights = listOf(
                AchievementHighlightUi(
                    label = "Chuỗi dài nhất",
                    value = "$maxStreak ngày",
                    iconEmoji = "🔥",
                    accentColor = Color(0xFFEF4444)
                ),
                AchievementHighlightUi(
                    label = "Hoàn thành thử thách",
                    value = "$challengeCount thử thách",
                    iconEmoji = "✅",
                    accentColor = Color(0xFF10B981)
                ),
                AchievementHighlightUi(
                    label = "Tổng số check-in",
                    value = "$totalCheckIns lần",
                    iconEmoji = "📅",
                    accentColor = Color(0xFF3B82F6)
                ),
                AchievementHighlightUi(
                    label = "Đang tham gia",
                    value = "$activeCount thử thách",
                    iconEmoji = "▶",
                    accentColor = Color(0xFFF59E0B)
                )
            )

            updateState {
                copy(
                    isLoading = false,
                    header = header,
                    featuredBadges = featured,
                    highlights = highlights
                )
            }
        }
    }

    private fun parseColor(hex: String): Color = try {
        Color(android.graphics.Color.parseColor(hex))
    } catch (e: Exception) {
        Color(0xFF0077FF)
    }
}
