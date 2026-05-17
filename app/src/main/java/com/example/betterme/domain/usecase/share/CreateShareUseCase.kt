package com.example.betterme.domain.usecase.share

import com.example.betterme.data.local.datastore.DataStoreManager
import com.example.betterme.domain.repository.ChallengeLogRepository
import com.example.betterme.domain.repository.HabitLogRepository
import com.example.betterme.domain.repository.HabitRepository
import com.example.betterme.domain.repository.ShareRepository
import com.example.betterme.domain.repository.ShareRepository.CheckInInput
import com.example.betterme.domain.repository.UserChallengeRepository
import com.example.betterme.domain.share.ShareLink
import com.example.betterme.domain.share.VerifiedCheckIn
import com.example.betterme.utils.DateUtils
import kotlinx.coroutines.flow.first

/**
 * Snapshot the user's full habit + challenge check-in history from
 * Room and publish it to Firestore.
 *
 * Always full history — the previous "type picker" (full / habit /
 * challenge) was scope cruft. The viewer screen renders ALL the
 * user's check-ins; if they want to share less, they can prune in
 * a follow-up.
 *
 * Stats are computed locally and sent as part of the snapshot so the
 * viewer doesn't need a second round trip. "Verified" still holds —
 * the data lives in Firestore, viewer reads from there.
 */
class CreateShareUseCase(
    private val dataStoreManager: DataStoreManager,
    private val habitRepository: HabitRepository,
    private val habitLogRepository: HabitLogRepository,
    private val userChallengeRepository: UserChallengeRepository,
    private val challengeLogRepository: ChallengeLogRepository,
    private val shareRepository: ShareRepository
) {

    suspend operator fun invoke(): ShareLink {
        val user = dataStoreManager.getUserInfo().first()
            ?: throw IllegalStateException("Vui lòng đăng nhập trước khi chia sẻ.")

        val checkIns = gatherFullHistory(user.id)
        if (checkIns.isEmpty()) {
            throw IllegalStateException("Chưa có check-in nào để chia sẻ.")
        }

        val streakInfo = computeStreaks(user.id)
        val challengeStats = computeChallengeStats(user.id)

        return shareRepository.publishMyProgress(
            displayName = user.name.ifBlank { "BetterMe User" },
            avatarUrl = user.photoUrl.ifBlank { null },
            currentStreakDays = streakInfo.current,
            longestStreakDays = streakInfo.longest,
            completedChallenges = challengeStats.completed,
            legendaryChallenges = challengeStats.legendary,
            checkIns = checkIns
        )
    }

    // ─── Gatherers ─────────────────────────────────────────────────

    private suspend fun gatherFullHistory(userId: String): List<CheckInInput> {
        val out = mutableListOf<CheckInInput>()
        val habits = habitRepository.getHabits(userId).first()
        for (habit in habits) {
            val logs = habitLogRepository.getLogs(habit.id).first()
                .filter { it.status == "DONE" }
            logs.forEach { log ->
                out += CheckInInput(
                    itemId = "h-${habit.id}",
                    name = habit.title,
                    kind = VerifiedCheckIn.Kind.HABIT,
                    date = log.date
                )
            }
        }
        val challenges = userChallengeRepository.observeByUser(userId).first()
        for (uc in challenges) {
            val title = userChallengeRepository.getWithDetailsById(uc.id)
                ?.challenge?.title ?: "Thử thách"
            challengeLogRepository.getDoneDates(uc.id).forEach { ts ->
                out += CheckInInput(
                    itemId = "c-${uc.id}",
                    name = title,
                    kind = VerifiedCheckIn.Kind.CHALLENGE,
                    date = ts
                )
            }
        }
        // Cap matches the repo's `MAX_CHECK_INS_PER_DOC` so we never
        // pay the cost of building rows that would be discarded.
        return out.sortedByDescending { it.date }.take(MAX_CHECK_INS)
    }

    // ─── Local stat derivation (one Firestore round trip total) ────

    private suspend fun computeStreaks(userId: String): StreakInfo {
        val habits = habitRepository.getHabits(userId).first()
        if (habits.isEmpty()) return StreakInfo(0, 0)
        val today = DateUtils.startOfDay()
        var bestCurrent = 0
        var bestLongest = 0
        for (habit in habits) {
            val dates = habitLogRepository.getLogs(habit.id).first()
                .filter { it.status == "DONE" }
                .map { DateUtils.startOfDay(it.date) }
            if (dates.isEmpty()) continue
            val current = DateUtils.currentStreak(dates, today)
            val longest = DateUtils.longestStreak(dates)
            if (current > bestCurrent) bestCurrent = current
            if (longest > bestLongest) bestLongest = longest
        }
        return StreakInfo(bestCurrent, bestLongest)
    }

    private suspend fun computeChallengeStats(userId: String): ChallengeStats {
        val completedCount = userChallengeRepository.countCompletedByUser(userId)
        val ucs = userChallengeRepository.observeByUser(userId).first()
        var legendary = 0
        for (uc in ucs) {
            if (uc.status != "COMPLETED") continue
            val details = userChallengeRepository.getWithDetailsById(uc.id) ?: continue
            if (details.challenge.difficulty.equals("LEGENDARY", ignoreCase = true)) {
                legendary++
            }
        }
        return ChallengeStats(completedCount, legendary)
    }

    private data class StreakInfo(val current: Int, val longest: Int)
    private data class ChallengeStats(val completed: Int, val legendary: Int)

    private companion object {
        /** Mirrors [com.example.betterme.data.share.ShareRepositoryImpl]. */
        const val MAX_CHECK_INS = 500
    }
}
