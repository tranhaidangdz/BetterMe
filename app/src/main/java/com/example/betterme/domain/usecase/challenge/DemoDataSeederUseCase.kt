package com.example.betterme.domain.usecase.challenge

import androidx.room.withTransaction
import com.example.betterme.data.local.datastore.DataStoreManager
import com.example.betterme.data.local.room.database.BetterMeDatabase
import com.example.betterme.data.local.room.entities.ChallengeLogEntity
import com.example.betterme.data.local.room.entities.UserChallengeEntity
import com.example.betterme.domain.repository.AchievementRepository
import com.example.betterme.domain.repository.ChallengeLogRepository
import com.example.betterme.domain.repository.ChallengeRepository
import com.example.betterme.domain.repository.UserAchievementRepository
import com.example.betterme.domain.repository.UserChallengeRepository
import com.example.betterme.domain.repository.UserRepository

/**
 * Seeds a realistic "actively used account" worth of reward data for the current
 * user so the badge / achievement / statistics / progression surfaces have
 * something to render during a demo.
 *
 * Seeds, idempotently (guarded by [DataStoreManager.isDemoDataSeeded] + the unique
 * indices on `user_challenges` / `user_achievements`):
 *  - 5 COMPLETED challenges (starter catalog IDs 300-304) with realistic streaks
 *  - a full set of DONE check-in logs for each (drives total-check-in + streak stats)
 *  - the reward badge configured on each completed challenge, plus 3 milestone badges
 *  - a healthy coin balance + recomputed level
 *
 * It writes the *end state* directly rather than replaying the live reward flow —
 * the live flow ([AwardChallengeCompletionUseCase]) stays the single path that
 * grants rewards for genuine completions. Re-running this seeder is a no-op: the
 * DataStore flag short-circuits it, and even without the flag the unique indices
 * + `getByUserAndChallenge` / `hasEarned` guards prevent any duplicate rows.
 *
 * Catalog dependency: must run AFTER [ChallengeSeederUseCase] so the challenge +
 * badge FKs exist. The splash bootstrap calls them in that order.
 */
class DemoDataSeederUseCase(
    private val database: BetterMeDatabase,
    private val dataStoreManager: DataStoreManager,
    private val challengeRepository: ChallengeRepository,
    private val userChallengeRepository: UserChallengeRepository,
    private val userAchievementRepository: UserAchievementRepository,
    private val challengeLogRepository: ChallengeLogRepository,
    private val achievementRepository: AchievementRepository,
    private val userRepository: UserRepository
) {

    private data class DemoChallenge(val challengeId: Int, val durationDays: Int, val bestStreak: Int)

    suspend operator fun invoke(userId: String) {
        if (userId.isBlank()) return
        if (dataStoreManager.isDemoDataSeeded()) return
        // The user row must exist (addCoins / FK). If sign-in hasn't persisted it
        // yet, skip silently — the flag stays unset so the next launch retries.
        if (userRepository.getUserById(userId) == null) return

        val demo = listOf(
            DemoChallenge(challengeId = 300, durationDays = 7, bestStreak = 7),
            DemoChallenge(challengeId = 301, durationDays = 14, bestStreak = 14),
            DemoChallenge(challengeId = 302, durationDays = 7, bestStreak = 7),
            DemoChallenge(challengeId = 303, durationDays = 14, bestStreak = 12),
            DemoChallenge(challengeId = 304, durationDays = 7, bestStreak = 7)
        )

        database.withTransaction {
            val now = System.currentTimeMillis()

            demo.forEach { d ->
                // FK safety: only seed against challenges that exist in the catalog.
                val challenge = challengeRepository.getById(d.challengeId) ?: return@forEach

                // Reuse an existing join row if present (unique index), else insert COMPLETED.
                val existing = userChallengeRepository.getByUserAndChallenge(userId, d.challengeId)
                val startDate = startOfDay(now - d.durationDays.toLong() * DAY_MS)
                val endDate = startOfDay(now - DAY_MS)
                val ucId: Int = existing?.id ?: userChallengeRepository.insert(
                    UserChallengeEntity(
                        user_id = userId,
                        challenge_id = d.challengeId,
                        status = "COMPLETED",
                        start_date = startDate,
                        target_end_date = endDate,
                        end_date = endDate,
                        current_streak = d.bestStreak,
                        best_streak = d.bestStreak,
                        last_check_in_date = endDate,
                        progress_pct = 100
                    )
                ).toInt()

                // DONE log per day so total-check-in + streak stats look real. Idempotent
                // per (ucId, date) — getLogByDate guards re-runs.
                for (i in 0 until d.durationDays) {
                    val date = startOfDay(now - (d.durationDays - i).toLong() * DAY_MS)
                    if (challengeLogRepository.getLogByDate(ucId, date) == null) {
                        challengeLogRepository.addLog(
                            ChallengeLogEntity(
                                user_challenge_id = ucId,
                                date = date,
                                status = "DONE"
                            )
                        )
                    }
                }

                // Award the challenge's configured reward badge (idempotent via hasEarned).
                challenge.reward_badge_id?.let { badgeId ->
                    awardIfExists(userId, badgeId, ucId)
                }
            }

            // A few milestone badges so the collection looks lived-in.
            listOf(1, 2, 3).forEach { badgeId -> awardIfExists(userId, badgeId, null) }

            // Healthy coin balance + level. addCoins is additive but the DataStore
            // flag means this runs exactly once per install.
            userRepository.addCoins(userId, DEMO_COINS)
            userRepository.recomputeLevel(userId)
        }

        dataStoreManager.setDemoDataSeeded()
    }

    /** Award only when the badge exists in the catalog — avoids an FK violation. */
    private suspend fun awardIfExists(userId: String, badgeId: Int, sourceUcId: Int?) {
        if (achievementRepository.getById(badgeId) == null) return
        if (userAchievementRepository.hasEarned(userId, badgeId)) return
        userAchievementRepository.award(userId, badgeId, sourceUcId)
    }

    private fun startOfDay(millis: Long): Long {
        val c = java.util.Calendar.getInstance().apply {
            timeInMillis = millis
            set(java.util.Calendar.HOUR_OF_DAY, 0)
            set(java.util.Calendar.MINUTE, 0)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }
        return c.timeInMillis
    }

    private companion object {
        const val DAY_MS: Long = 24L * 60L * 60L * 1000L
        /** Lump coin balance for the demo account — roughly a few completed challenges' worth. */
        const val DEMO_COINS = 1480
    }
}
