package com.example.betterme.presentation.challenge.model

import androidx.compose.ui.graphics.Color
import com.example.betterme.presentation.challenge.shared.Difficulty

/**
 * UI model for an active challenge row in the Overview screen.
 */
data class ChallengeProgressUiModel(
    val userChallengeId: Int,
    val challengeId: Int,
    val title: String,
    val iconEmoji: String,
    val accentColor: Color,
    val difficulty: Difficulty,
    val currentStreak: Int,
    val targetStreak: Int,
    val progressPct: Int,
    val rewardCoins: Int,
    val rewardBadgeName: String?,
    val isGroup: Boolean
)

/**
 * UI model for an upcoming challenge row.
 */
data class UpcomingChallengeUiModel(
    val challengeId: Int,
    val title: String,
    val iconEmoji: String,
    val accentColor: Color,
    val difficulty: Difficulty,
    val rewardCoins: Int,
    val daysUntilStart: Int,
    val reminderEnabled: Boolean
)

/**
 * UI model for a completed (or failed) challenge row.
 */
data class CompletedChallengeUiModel(
    val userChallengeId: Int,
    val challengeId: Int,
    val title: String,
    val iconEmoji: String,
    val accentColor: Color,
    val isCompleted: Boolean,
    val finishedDateLabel: String,
    val rewardCoins: Int,
    val rewardBadgeName: String?
)

/**
 * UI model for cards on the Discover screen.
 */
data class FeaturedChallengeUiModel(
    val challengeId: Int,
    val title: String,
    val durationDaysLabel: String,    // "21 ngày liên tiếp"
    val iconEmoji: String,
    val accentColor: Color,
    val rewardCoins: Int,
    val participantCount: Int,
    val isGroup: Boolean = false
)

data class NewChallengeUiModel(
    val challengeId: Int,
    val title: String,
    val iconEmoji: String,
    val accentColor: Color,
    val difficulty: Difficulty,
    val rewardCoins: Int,
    val participantCount: Int,
    val isGroup: Boolean = false
)

data class CategoryTileUi(
    val categoryId: Int,
    val name: String,
    val emoji: String,
    val challengeCount: Int,
    val accentColor: Color
)

/**
 * UI model for the prominent upcoming-challenge carousel on the Discover screen.
 * Includes the precomputed countdown so the card stays render-cheap.
 */
data class UpcomingFeatureUiModel(
    val challengeId: Int,
    val title: String,
    val iconEmoji: String,
    val accentColor: Color,
    val daysUntilStart: Int,
    val startLabel: String,
    val rewardCoins: Int,
    val participantCount: Int
)

/**
 * Single day cell in the 7-day streak strip on Detail screen.
 */
data class DayCellUi(
    val label: String,        // "T2", "T3", ... "CN"
    val dateLabel: String,    // "20/1"
    val status: DayStatus,
    val isToday: Boolean
)

enum class DayStatus { Done, Missed, Future, Today }

/**
 * Badge tile for the Badges screen (and the smaller Achievements collection row).
 *
 * [iconRes] is a drawable resource id (PNG art for production badges). When non-zero, the
 * tile renders the image with [androidx.compose.ui.layout.ContentScale.Fit] so the artwork
 * is never stretched. [iconEmoji] is the legacy fallback used only when iconRes == 0.
 */
data class BadgeUiModel(
    val id: Int,
    val name: String,
    val description: String,
    val iconRes: Int = 0,
    val iconEmoji: String = "🏅",
    val accentColor: Color,
    val isEarned: Boolean,
    val earnedAtLabel: String? = null
)

/**
 * Group challenge team row in the leaderboard.
 */
data class TeamUiModel(
    val id: Int,
    val name: String,
    val iconEmoji: String,
    val accentColor: Color,
    val memberCount: Int,
    val totalCoins: Int,
    val rank: Int
)

/**
 * Hero stats for the Overview screen card (28 / 16 / 84%).
 */
data class OverviewStatsUi(
    val joined: Int,
    val completed: Int,
    val completionRate: Int   // 0-100
)

/**
 * Achievements screen header card.
 */
data class AchievementsHeaderUi(
    val name: String,
    val photoUrl: String,
    val level: Int,
    val badgeCount: Int,
    val challengeCount: Int,
    val totalCoins: Int
)

/**
 * Single highlight stat row on Achievements screen.
 */
data class AchievementHighlightUi(
    val label: String,
    val value: String,
    val iconEmoji: String,
    val accentColor: Color
)

/**
 * Result payload that drives the celebration dialog.
 */
data class CelebrationUi(
    val challengeTitle: String,
    val coinsEarned: Int,
    val badgeName: String?
)

/**
 * Filter selection on the Overview screen.
 */
enum class OverviewFilter(val label: String, val storedStatus: String) {
    Active("Đang diễn ra", "ACTIVE"),
    Upcoming("Sắp diễn ra", "UPCOMING"),
    Completed("Đã hoàn thành", "COMPLETED")
}

/**
 * Sub-filter for the Completed screen.
 */
enum class CompletedFilter(val label: String) {
    All("Tất cả"),
    Done("Đã hoàn thành"),
    Failed("Chưa hoàn thành")
}
