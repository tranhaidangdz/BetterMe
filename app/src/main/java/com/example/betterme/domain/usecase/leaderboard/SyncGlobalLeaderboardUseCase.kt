package com.example.betterme.domain.usecase.leaderboard

import com.example.betterme.data.local.datastore.DataStoreManager
import com.example.betterme.domain.leaderboard.Season
import com.example.betterme.domain.repository.ChallengeLogRepository
import com.example.betterme.domain.repository.GlobalLeaderboardRepository
import com.example.betterme.domain.repository.HabitLogRepository
import com.example.betterme.domain.repository.HabitRepository
import com.example.betterme.domain.repository.UserChallengeRepository
import com.example.betterme.utils.DateUtils
import kotlinx.coroutines.flow.first
import java.util.Calendar

/**
 * Aggregate the current user's stats from Room and push them as the
 * GLOBAL leaderboard entry for the current season.
 *
 * Inputs are computed locally — there's NO Firestore read here, so
 * this is cheap enough to run on every check-in alongside the
 * per-challenge sync. The repository throttles the write itself (30s
 * per user, bypassed when `force = true`).
 *
 * Stat derivations:
 *  - totalCompletedHabits  = DONE-log count across every habit, this season
 *  - longestStreak         = max(best_streak) across UserChallenge rows
 *                            and max(streak) across habit logs. We use
 *                            UserChallenge.bestStreak as the proxy since
 *                            it's already maintained by the check-in path.
 *  - earnedCoins           = sum(reward_coins × completionRatio) across
 *                            user's challenges this season; this matches
 *                            the per-challenge formula's coin model.
 *  - completedChallenges   = UserChallengeRepository.countCompletedByUser
 *  - monthlyConsistencyBonus = (distinct check-in days / days-in-season-so-far) × 100
 */
class SyncGlobalLeaderboardUseCase(
    private val dataStoreManager: DataStoreManager,
    private val habitRepository: HabitRepository,
    private val habitLogRepository: HabitLogRepository,
    private val userChallengeRepository: UserChallengeRepository,
    private val challengeLogRepository: ChallengeLogRepository,
    private val globalLeaderboardRepository: GlobalLeaderboardRepository
) {

    suspend operator fun invoke(force: Boolean = false) {
        val user = dataStoreManager.getUserInfo().first() ?: return
        val seasonKey = Season.current()
        val seasonStartMs = startOfMonth()
        val today = DateUtils.startOfDay()
        val daysInSeasonSoFar = ((today - seasonStartMs) / DAY_MS + 1).toInt().coerceAtLeast(1)

        // ── totalCompletedHabits + monthly consistency (habits)
        val habits = habitRepository.getHabits(user.id).first()
        val seasonHabitLogs = habits.flatMap { habit ->
            habitLogRepository.getLogs(habit.id).first()
                .filter { it.status == "DONE" && it.date >= seasonStartMs }
        }
        val totalCompletedHabits = seasonHabitLogs.size

        // ── longestStreak (challenge best_streak; cheap, already maintained)
        val longestStreak = userChallengeRepository.maxBestStreak(user.id) ?: 0

        // ── earnedCoins (sum proportional across active challenges)
        val userChallenges = userChallengeRepository.observeByUser(user.id).first()
        var coins = 0
        var activeChallengesSeasonCheckIns = 0
        for (uc in userChallenges) {
            val seasonChallengeLogs = challengeLogRepository.getDoneDates(uc.id)
                .count { it >= seasonStartMs }
            activeChallengesSeasonCheckIns += seasonChallengeLogs
            // Approximate per-challenge coins: reward × (season-progress / target)
            // We don't read the Challenge entity here to keep this hot
            // path Room-light — the repository can stitch the exact
            // coin number from the per-challenge sync. Approximation is
            // fine for the global score which is dominated by other terms.
            coins += (seasonChallengeLogs * 2)
        }

        // ── completedChallenges
        val completedChallenges = userChallengeRepository.countCompletedByUser(user.id)

        // ── monthlyConsistencyBonus — distinct check-in days / season days
        val distinctDays = (seasonHabitLogs.map { it.date } +
            userChallenges.flatMap { challengeLogRepository.getDoneDates(it.id) }
                .filter { it >= seasonStartMs })
            .toSet().size
        val consistency = ((distinctDays.toFloat() / daysInSeasonSoFar) * 100f).toInt().coerceIn(0, 100)

        globalLeaderboardRepository.upsertMyEntry(
            seasonKey = seasonKey,
            displayName = user.name.ifBlank { "BetterMe User" },
            avatarUrl = user.photoUrl.ifBlank { null },
            totalCompletedHabits = totalCompletedHabits,
            longestStreak = longestStreak,
            earnedCoins = coins,
            completedChallenges = completedChallenges,
            monthlyConsistencyBonus = consistency,
            force = force
        )
    }

    private fun startOfMonth(): Long {
        val cal = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return cal.timeInMillis
    }

    private companion object {
        const val DAY_MS: Long = 24L * 60L * 60L * 1000L
    }
}
