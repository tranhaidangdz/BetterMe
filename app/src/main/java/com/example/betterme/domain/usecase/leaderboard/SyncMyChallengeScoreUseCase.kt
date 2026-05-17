package com.example.betterme.domain.usecase.leaderboard

import com.example.betterme.data.local.datastore.DataStoreManager
import com.example.betterme.domain.leaderboard.ScoreFormula
import com.example.betterme.domain.leaderboard.Season
import com.example.betterme.domain.repository.ChallengeLeaderboardRepository
import com.example.betterme.domain.repository.ChallengeLogRepository
import com.example.betterme.domain.repository.ChallengeRepository
import com.example.betterme.domain.repository.UserChallengeRepository
import kotlinx.coroutines.flow.first
import java.util.Calendar

/**
 * Computes the current user's score for one challenge in the current
 * season and writes it to Firestore via the repository.
 *
 * Invoked from [com.example.betterme.domain.usecase.challenge.CheckInChallengeUseCase]
 * after a successful Progress / Completed result. The repo throttles
 * writes per-(userId, challengeId) — see [LeaderboardSessionMemory] —
 * so rapid consecutive check-ins coalesce into one Firestore write.
 *
 * Score components are sourced locally (no Firestore READ needed
 * here):
 *  - completedTasks  = count of DONE logs THIS SEASON for the user's
 *                      UserChallenge row.
 *  - currentStreak   = the UserChallenge's running streak (already
 *                      maintained by CheckInChallengeUseCase).
 *  - earnedCoins     = proportional derivation via [ScoreFormula.coinsEarned]
 *                      from the challenge's `reward_coins` and the
 *                      season check-in count.
 *
 * Failures are swallowed (logged in the repo); the caller's check-in
 * has already succeeded locally and shouldn't be undone by a
 * Firestore hiccup.
 */
class SyncMyChallengeScoreUseCase(
    private val dataStoreManager: DataStoreManager,
    private val challengeRepository: ChallengeRepository,
    private val userChallengeRepository: UserChallengeRepository,
    private val challengeLogRepository: ChallengeLogRepository,
    private val leaderboardRepository: ChallengeLeaderboardRepository
) {

    suspend operator fun invoke(
        userChallengeId: Int,
        force: Boolean = false
    ) {
        val user = dataStoreManager.getUserInfo().first() ?: return
        val uc = userChallengeRepository.getById(userChallengeId) ?: return
        val challenge = challengeRepository.getById(uc.challenge_id) ?: return

        val seasonKey = Season.current()
        val seasonStart = startOfMonthMs()

        // Pull just the logs we need; the repo doesn't have a
        // "thisMonth" query so we filter in memory. Volume is small —
        // 30 days × O(1) docs.
        val seasonDoneDates = challengeLogRepository.getDoneDates(userChallengeId)
            .filter { it >= seasonStart }
        val completedTasks = seasonDoneDates.size

        val earnedCoins = ScoreFormula.coinsEarned(
            rewardCoins = challenge.reward_coins,
            checkInsThisSeason = completedTasks,
            targetStreak = challenge.target_streak
        )

        leaderboardRepository.upsertMyEntry(
            challengeId = uc.challenge_id,
            seasonKey = seasonKey,
            displayName = user.name.ifBlank { "BetterMe User" },
            avatarUrl = user.photoUrl.ifBlank { null },
            completedTasks = completedTasks,
            currentStreak = uc.current_streak,
            earnedCoins = earnedCoins,
            force = force
        )
    }

    /** Start-of-month timestamp in the device's local timezone. */
    private fun startOfMonthMs(): Long {
        val cal = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return cal.timeInMillis
    }
}
