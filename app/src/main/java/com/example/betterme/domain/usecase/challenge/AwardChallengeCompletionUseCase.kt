package com.example.betterme.domain.usecase.challenge

import com.example.betterme.data.local.room.entities.AchievementEntity
import com.example.betterme.data.local.room.entities.ChallengeEntity
import com.example.betterme.data.local.room.entities.UserChallengeEntity
import com.example.betterme.domain.repository.AchievementRepository
import com.example.betterme.domain.repository.ChallengeLogRepository
import com.example.betterme.domain.repository.GroupTeamRepository
import com.example.betterme.domain.repository.UserAchievementRepository
import com.example.betterme.domain.repository.UserChallengeRepository
import com.example.betterme.domain.repository.UserRepository

/**
 * Runs when a UserChallenge crosses its target streak. Marks the row COMPLETED, awards
 * coins, awards the configured reward badge (if any), auto-awards any threshold-based
 * badges (streak / total check-ins / coins), and bumps the user's group team coins.
 *
 * Caller is expected to have already inserted the final ChallengeLog row.
 */
class AwardChallengeCompletionUseCase(
    private val userChallengeRepository: UserChallengeRepository,
    private val userRepository: UserRepository,
    private val achievementRepository: AchievementRepository,
    private val userAchievementRepository: UserAchievementRepository,
    private val challengeLogRepository: ChallengeLogRepository,
    private val groupTeamRepository: GroupTeamRepository
) {

    data class Result(
        val coinsEarned: Int,
        val rewardBadge: AchievementEntity?,
        val bonusBadges: List<AchievementEntity>
    )

    suspend operator fun invoke(
        userChallenge: UserChallengeEntity,
        challenge: ChallengeEntity
    ): Result {
        val now = System.currentTimeMillis()

        // 1. Mark the user_challenge row COMPLETED.
        userChallengeRepository.markCompleted(userChallenge.id, now)

        // 2. Award coins (also bumps xp).
        val coins = challenge.reward_coins
        if (coins > 0) {
            userRepository.addCoins(userChallenge.user_id, coins)
            userRepository.recomputeLevel(userChallenge.user_id)
        }

        // 3. Award the configured reward badge (idempotent).
        val rewardBadge = challenge.reward_badge_id?.let { badgeId ->
            val badge = achievementRepository.getById(badgeId)
            if (badge != null && !userAchievementRepository.hasEarned(userChallenge.user_id, badgeId)) {
                userAchievementRepository.award(
                    userId = userChallenge.user_id,
                    achievementId = badgeId,
                    sourceUserChallengeId = userChallenge.id
                )
                badge
            } else null
        }

        // 4. Auto-award threshold badges (streak / total check-ins / coins / challenges).
        val bonusBadges = buildList {
            // Streak — best streak across all the user's challenges.
            val maxStreak = userChallengeRepository.maxBestStreak(userChallenge.user_id) ?: 0
            addAll(
                achievementRepository.findUnclaimedByThreshold(
                    userId = userChallenge.user_id,
                    type = "STREAK",
                    value = maxStreak
                )
            )

            // Total check-ins.
            val totalCheckIns = challengeLogRepository.countTotalCheckInsByUser(userChallenge.user_id)
            addAll(
                achievementRepository.findUnclaimedByThreshold(
                    userId = userChallenge.user_id,
                    type = "TOTAL_CHECKINS",
                    value = totalCheckIns
                )
            )

            // Coins.
            val coinsTotal = userRepository.getUserById(userChallenge.user_id)?.coins ?: 0
            addAll(
                achievementRepository.findUnclaimedByThreshold(
                    userId = userChallenge.user_id,
                    type = "COINS",
                    value = coinsTotal
                )
            )
        }
        bonusBadges.forEach { badge ->
            userAchievementRepository.award(
                userId = userChallenge.user_id,
                achievementId = badge.id,
                sourceUserChallengeId = userChallenge.id
            )
        }

        // 5. Group challenge — bump team's total_coins by the reward amount.
        if (challenge.is_group && userChallenge.team_id != null && coins > 0) {
            groupTeamRepository.addCoinsToTeam(userChallenge.team_id, coins)
        }

        return Result(
            coinsEarned = coins,
            rewardBadge = rewardBadge,
            bonusBadges = bonusBadges
        )
    }
}
