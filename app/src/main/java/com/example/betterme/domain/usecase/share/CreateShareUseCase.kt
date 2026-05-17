package com.example.betterme.domain.usecase.share

import com.example.betterme.data.local.datastore.DataStoreManager
import com.example.betterme.domain.repository.HabitLogRepository
import com.example.betterme.domain.repository.HabitRepository
import com.example.betterme.domain.repository.ShareRepository
import com.example.betterme.domain.repository.ShareRepository.CheckInInput
import com.example.betterme.domain.repository.UserChallengeRepository
import com.example.betterme.domain.repository.ChallengeLogRepository
import com.example.betterme.domain.share.ShareLink
import com.example.betterme.domain.share.ShareType
import com.example.betterme.domain.share.VerifiedCheckIn
import com.example.betterme.utils.DateUtils
import kotlinx.coroutines.flow.first

/**
 * Builds a snapshot from local Room data and pushes it to the share
 * Cloud Function. The function re-derives every stat server-side
 * before signing — so this client-side computation only feeds the
 * function the raw inputs; it doesn't determine the displayed
 * numbers on the viewer.
 *
 * Three slices supported:
 *  - [ShareType.FULL_HISTORY] — all habit + challenge check-ins.
 *  - [ShareType.HABIT]        — one habit's check-ins. [itemId] must
 *                               be the habit's Room id (numeric string).
 *  - [ShareType.CHALLENGE]    — one user-challenge's check-ins.
 *                               [itemId] is the UserChallenge.id.
 */
class CreateShareUseCase(
    private val dataStoreManager: DataStoreManager,
    private val habitRepository: HabitRepository,
    private val habitLogRepository: HabitLogRepository,
    private val userChallengeRepository: UserChallengeRepository,
    private val challengeLogRepository: ChallengeLogRepository,
    private val shareRepository: ShareRepository
) {

    suspend operator fun invoke(
        type: ShareType,
        itemId: String? = null
    ): ShareLink {
        val user = dataStoreManager.getUserInfo().first()
            ?: throw IllegalStateException("Vui lòng đăng nhập trước khi chia sẻ.")

        val checkIns: List<CheckInInput> = when (type) {
            ShareType.FULL_HISTORY -> gatherFullHistory(user.id)
            ShareType.HABIT -> {
                val habitId = itemId?.toIntOrNull()
                    ?: throw IllegalArgumentException("HABIT share needs a habit id.")
                gatherHabit(habitId)
            }
            ShareType.CHALLENGE -> {
                val ucId = itemId?.toIntOrNull()
                    ?: throw IllegalArgumentException("CHALLENGE share needs a user-challenge id.")
                gatherChallenge(ucId)
            }
        }

        // Local-view stats — the server recomputes; these are passed
        // through purely so the share message rendered in Messenger
        // can include rough numbers immediately (before the receiver
        // opens the link). The viewer ALWAYS uses the server numbers.
        val streakInfo = computeRoughStreak(user.id)
        val challengeStats = computeChallengeStats(user.id)

        return shareRepository.createShare(
            type = type,
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
                    itemTitle = habit.title,
                    timestamp = log.date,
                    kind = VerifiedCheckIn.Kind.HABIT,
                    note = log.note?.take(280)
                )
            }
        }
        val challenges = userChallengeRepository.observeByUser(userId).first()
        for (uc in challenges) {
            val titled = userChallengeRepository.getWithDetailsById(uc.id)
                ?.challenge?.title ?: "Thử thách"
            val dates = challengeLogRepository.getDoneDates(uc.id)
            dates.forEach { ts ->
                out += CheckInInput(
                    itemId = "c-${uc.id}",
                    itemTitle = titled,
                    timestamp = ts,
                    kind = VerifiedCheckIn.Kind.CHALLENGE,
                    note = null
                )
            }
        }
        // Server caps at 500. We pick the most recent 500 client-side
        // to avoid wasting an HTTP round-trip on a 400.
        return out.sortedByDescending { it.timestamp }.take(SERVER_MAX_CHECKINS)
    }

    private suspend fun gatherHabit(habitId: Int): List<CheckInInput> {
        val habit = habitRepository.getHabitById(habitId)
            ?: throw IllegalArgumentException("Habit $habitId not found.")
        val logs = habitLogRepository.getLogs(habitId).first()
            .filter { it.status == "DONE" }
            .sortedByDescending { it.date }
            .take(SERVER_MAX_CHECKINS)
        return logs.map { log ->
            CheckInInput(
                itemId = "h-${habit.id}",
                itemTitle = habit.title,
                timestamp = log.date,
                kind = VerifiedCheckIn.Kind.HABIT,
                note = log.note?.take(280)
            )
        }
    }

    private suspend fun gatherChallenge(userChallengeId: Int): List<CheckInInput> {
        val details = userChallengeRepository.getWithDetailsById(userChallengeId)
            ?: throw IllegalArgumentException("UserChallenge $userChallengeId not found.")
        val title = details.challenge.title
        val dates = challengeLogRepository.getDoneDates(userChallengeId)
            .sortedDescending()
            .take(SERVER_MAX_CHECKINS)
        return dates.map { ts ->
            CheckInInput(
                itemId = "c-$userChallengeId",
                itemTitle = title,
                timestamp = ts,
                kind = VerifiedCheckIn.Kind.CHALLENGE,
                note = null
            )
        }
    }

    // ─── Rough stats for the rich-share message ────────────────────

    private suspend fun computeRoughStreak(userId: String): StreakInfo {
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
            val withDetails = userChallengeRepository.getWithDetailsById(uc.id) ?: continue
            if (withDetails.challenge.difficulty.equals("LEGENDARY", ignoreCase = true)) {
                legendary++
            }
        }
        return ChallengeStats(completedCount, legendary)
    }

    private data class StreakInfo(val current: Int, val longest: Int)
    private data class ChallengeStats(val completed: Int, val legendary: Int)

    private companion object {
        /** Mirrors the cap in `functions/index.js`. */
        const val SERVER_MAX_CHECKINS = 500
    }
}
