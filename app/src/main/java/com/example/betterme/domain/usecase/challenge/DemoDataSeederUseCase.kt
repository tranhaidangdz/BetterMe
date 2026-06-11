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
 *  - 5 COMPLETED personal challenges with realistic streaks and per-day DONE logs
 *  - 3 ACTIVE in-progress challenges that show up under "Đang tham gia"
 *  - 1 UPCOMING challenge that starts in a few days
 *  - the reward badge configured on each completed challenge + 4 milestone badges
 *  - a healthy coin / XP balance and recomputed level
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

    /**
     * One seeded challenge entry. [doneDays] is how many DONE check-in logs to
     * write (∈ [0, durationDays]). For COMPLETED rows this equals durationDays;
     * for ACTIVE rows it's the partial progress.
     */
    private data class DemoChallenge(
        val challengeId: Int,
        val status: String,                 // COMPLETED | ACTIVE | UPCOMING
        val startOffsetDays: Int,           // negative = in the past, positive = in the future
        val durationDays: Int,
        val streak: Int,
        val doneDays: Int
    )

    suspend operator fun invoke(userId: String) {
        if (userId.isBlank()) return
        if (dataStoreManager.isDemoDataSeeded()) return
        // The user row must exist (addCoins / FK). If sign-in hasn't persisted it
        // yet, skip silently — the flag stays unset so the next launch retries.
        if (userRepository.getUserById(userId) == null) return

        // COMPLETED — pick a mix of easy / medium / hard so the badge collection looks lived-in.
        // Each picks a real catalog challenge so reward_badge_id FKs resolve.
        val completed = listOf(
            DemoChallenge(challengeId = 1,  status = "COMPLETED", startOffsetDays = -60, durationDays = 7,  streak = 7,  doneDays = 7),  // Uống đủ nước
            DemoChallenge(challengeId = 2,  status = "COMPLETED", startOffsetDays = -45, durationDays = 14, streak = 14, doneDays = 14), // Dậy trước 7h
            DemoChallenge(challengeId = 4,  status = "COMPLETED", startOffsetDays = -30, durationDays = 14, streak = 14, doneDays = 14), // Đi bộ buổi sáng
            DemoChallenge(challengeId = 7,  status = "COMPLETED", startOffsetDays = -22, durationDays = 14, streak = 14, doneDays = 14), // Giãn cơ
            DemoChallenge(challengeId = 11, status = "COMPLETED", startOffsetDays = -16, durationDays = 14, streak = 14, doneDays = 14)  // Không ăn đường (HARD)
        )

        // ACTIVE — partially-progressed challenges at different stages.  durationDays
        // is the configured length; doneDays is how far along the user currently is.
        val active = listOf(
            DemoChallenge(challengeId = 6,  status = "ACTIVE", startOffsetDays = -21, durationDays = 30, streak = 21, doneDays = 21), // 8 ly nước — day 21/30
            DemoChallenge(challengeId = 8,  status = "ACTIVE", startOffsetDays = -14, durationDays = 30, streak = 14, doneDays = 14), // Ngủ trước 11h — day 14/30
            DemoChallenge(challengeId = 9,  status = "ACTIVE", startOffsetDays = -7,  durationDays = 30, streak = 7,  doneDays = 7)   // 10k bước — day 7/30
        )

        // UPCOMING — joined but not started yet.
        val upcoming = listOf(
            DemoChallenge(challengeId = 10, status = "UPCOMING", startOffsetDays = 3,  durationDays = 21, streak = 0, doneDays = 0)   // Tập luyện đều đặn
        )

        val all = completed + active + upcoming
        var coinsFromCompletions = 0
        val awardedBadgeIds = mutableSetOf<Int>()

        database.withTransaction {
            val now = System.currentTimeMillis()

            all.forEach { d ->
                // FK safety: only seed against challenges that actually exist in the catalog.
                val challenge = challengeRepository.getById(d.challengeId) ?: return@forEach
                val startDate = startOfDay(now + d.startOffsetDays.toLong() * DAY_MS)
                val targetEndDate = startOfDay(startDate + (d.durationDays - 1).toLong() * DAY_MS)
                val endDate = if (d.status == "COMPLETED") targetEndDate else null
                val progressPct = when (d.status) {
                    "COMPLETED" -> 100
                    "ACTIVE" -> (100 * d.doneDays / d.durationDays).coerceIn(0, 99)
                    else -> 0
                }
                val lastCheckIn = if (d.doneDays > 0) {
                    startOfDay(startDate + (d.doneDays - 1).toLong() * DAY_MS)
                } else null

                // Reuse an existing join row if present (unique index), else insert.
                val existing = userChallengeRepository.getByUserAndChallenge(userId, d.challengeId)
                val ucId: Int = existing?.id ?: userChallengeRepository.insert(
                    UserChallengeEntity(
                        user_id = userId,
                        challenge_id = d.challengeId,
                        status = d.status,
                        start_date = startDate,
                        target_end_date = targetEndDate,
                        end_date = endDate,
                        current_streak = d.streak,
                        best_streak = d.streak,
                        last_check_in_date = lastCheckIn,
                        progress_pct = progressPct
                    )
                ).toInt()

                // DONE log per progressed day so total-check-in + streak stats look real.
                // Idempotent per (ucId, date) — getLogByDate guards re-runs.
                for (i in 0 until d.doneDays) {
                    val date = startOfDay(startDate + i.toLong() * DAY_MS)
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

                // Award reward badge + tally coins ONLY for completed entries.  Active /
                // upcoming rows would normally grant rewards through the live flow on
                // genuine completion — we don't pre-award those.
                if (d.status == "COMPLETED") {
                    challenge.reward_badge_id?.let { badgeId ->
                        if (awardIfExists(userId, badgeId, ucId)) {
                            awardedBadgeIds += badgeId
                        }
                    }
                    coinsFromCompletions += challenge.reward_coins
                }
            }

            // Milestone badges so the collection looks lived-in even when several
            // completed challenges share a reward badge.  Mix of basic (1, 2, 3, 5),
            // health (6), and learning (11) so multiple sections show progress.
            listOf(1, 2, 3, 5, 6, 11).forEach { badgeId ->
                if (awardIfExists(userId, badgeId, null)) {
                    awardedBadgeIds += badgeId
                }
            }

            // Lump coin balance.  addCoins is additive (and increments xp at the
            // same time — see UserDao.addCoins), but the DataStore flag ensures
            // this runs exactly once per install.  Total = realistic completions
            // (≈ 530 coins from challenges 1+2+4+7+11) + a top-up so the user feels
            // they've been active for a while.
            userRepository.addCoins(userId, DEMO_COINS_TOPUP + coinsFromCompletions)
            userRepository.recomputeLevel(userId)
        }

        dataStoreManager.setDemoDataSeeded()
    }

    /**
     * Award only when the badge exists in the catalog (avoids an FK violation)
     * and isn't already earned.  Returns true if a NEW award row was inserted.
     */
    private suspend fun awardIfExists(userId: String, badgeId: Int, sourceUcId: Int?): Boolean {
        if (achievementRepository.getById(badgeId) == null) return false
        if (userAchievementRepository.hasEarned(userId, badgeId)) return false
        userAchievementRepository.award(userId, badgeId, sourceUcId)
        return true
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
        /** Static top-up added on top of the actual challenge-reward coin total. */
        const val DEMO_COINS_TOPUP = 2500
    }
}
