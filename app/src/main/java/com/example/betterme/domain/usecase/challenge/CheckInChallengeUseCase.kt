package com.example.betterme.domain.usecase.challenge

import androidx.room.withTransaction
import com.example.betterme.data.local.room.database.BetterMeDatabase
import com.example.betterme.data.local.room.entities.AchievementEntity
import com.example.betterme.data.local.room.entities.ChallengeLogEntity
import com.example.betterme.domain.challenge.UserChallengeStatus
import com.example.betterme.domain.repository.ChallengeLogRepository
import com.example.betterme.domain.repository.ChallengeRepository
import com.example.betterme.domain.repository.GroupTeamRepository
import com.example.betterme.domain.repository.UserChallengeRepository
import com.example.betterme.domain.usecase.leaderboard.SyncGlobalLeaderboardUseCase
import com.example.betterme.domain.usecase.leaderboard.SyncMyChallengeScoreUseCase
import com.example.betterme.utils.DateUtils

/**
 * Records a daily check-in for a UserChallenge.
 *
 * Strict-daily behavior (post-v12):
 *  - Before recording the log, [EvaluateChallengeStatusUseCase] runs against the row. If
 *    yesterday (or any earlier required day) lacks a DONE log, the row is permanently
 *    moved to FAILED and the result returns [Result.FailedNow] — but the new log still
 *    lands so the timeline keeps the history.
 *  - On a FAILED or COMPLETED row, check-ins continue to be accepted for tracking
 *    purposes ([Result.LoggedAfterTerminal]) — no streak update, no double awards, no
 *    silent "recovery" from FAILED back to COMPLETED.
 *  - Completion only fires via the evaluator (after the final day's log lands). The old
 *    "streak >= target_streak" shortcut is gone.
 *
 * Returns:
 *  - [Result.Progress] for normal in-window check-ins → streak/progress updated.
 *  - [Result.Completed] when this check-in closes the window with no gaps → coins + badge.
 *  - [Result.FailedNow] when a prior gap was detected on this entry → row marked FAILED;
 *    the new log was still saved.
 *  - [Result.LoggedAfterTerminal] when the row was already terminal (FAILED / COMPLETED /
 *    ABANDONED) → log saved for history, no status change.
 *  - [Result.AlreadyCheckedIn] when today already has a DONE log.
 *  - [Result.Error] for any other failure.
 *
 * All work runs inside a single Room transaction.
 */
class CheckInChallengeUseCase(
    private val database: BetterMeDatabase,
    private val challengeRepository: ChallengeRepository,
    private val userChallengeRepository: UserChallengeRepository,
    private val challengeLogRepository: ChallengeLogRepository,
    private val groupTeamRepository: GroupTeamRepository,
    private val awardCompletionUseCase: AwardChallengeCompletionUseCase,
    private val evaluateStatusUseCase: EvaluateChallengeStatusUseCase,
    private val syncMyChallengeScore: SyncMyChallengeScoreUseCase,
    private val syncGlobalLeaderboard: SyncGlobalLeaderboardUseCase
) {

    sealed class Result {
        data class Progress(
            val newStreak: Int,
            val progressPct: Int
        ) : Result()

        data class Completed(
            val coinsEarned: Int,
            val rewardBadge: AchievementEntity?,
            val bonusBadges: List<AchievementEntity>
        ) : Result()

        /** Row was just transitioned to FAILED during this call. The log was still saved. */
        data class FailedNow(val firstMissedDate: Long) : Result()

        /** Row was already terminal; log saved for history, no status change. */
        data class LoggedAfterTerminal(val status: String) : Result()

        data object AlreadyCheckedIn : Result()
        data class Error(val message: String) : Result()
    }

    suspend operator fun invoke(
        userChallengeId: Int,
        note: String? = null,
        imageUri: String? = null,
        latitude: Double? = null,
        longitude: Double? = null
    ): Result {
        val txResult: Result = try {
            database.withTransaction {
                val uc0 = userChallengeRepository.getById(userChallengeId)
                    ?: return@withTransaction Result.Error("UserChallenge $userChallengeId not found")
                val challenge = challengeRepository.getById(uc0.challenge_id)
                    ?: return@withTransaction Result.Error("Challenge ${uc0.challenge_id} not found")

                val today = DateUtils.startOfDay()

                // Idempotent day-collapse: multiple check-ins per day count as one.
                val existing = challengeLogRepository.getLogByDate(userChallengeId, today)
                if (existing != null && existing.status == "DONE") {
                    return@withTransaction Result.AlreadyCheckedIn
                }

                // Insert today's log first so the evaluator sees it and the timeline
                // captures the user's effort even if the challenge has already failed.
                val log = ChallengeLogEntity(
                    user_challenge_id = userChallengeId,
                    date = today,
                    status = "DONE",
                    note = note,
                    image = imageUri,
                    latitude = latitude,
                    longitude = longitude
                )
                challengeLogRepository.addLog(log)

                // If the row was already terminal (FAILED / COMPLETED / ABANDONED), stop
                // here: history captured, but no streak/award/status mutation. Streak
                // and progress fields are frozen at their terminal-time values.
                if (UserChallengeStatus.isTerminal(uc0.status)) {
                    return@withTransaction Result.LoggedAfterTerminal(uc0.status)
                }

                // Update progress metrics from the full DONE-set so the detail screen
                // shows the new "days completed" count even when this check-in
                // simultaneously triggers a FAIL (the gap was before today). We compute
                // window-progress: count of distinct DONE days in [start, today] over
                // duration_days, instead of the legacy backwards-only streak.
                val doneDates = challengeLogRepository.getDoneDates(userChallengeId)
                    .map { DateUtils.startOfDay(it) }
                    .toSet()
                val newStreak = DateUtils.currentStreak(doneDates, today)
                val newBest = maxOf(uc0.best_streak, newStreak)
                val duration = challenge.duration_days.coerceAtLeast(1)
                val daysDoneInWindow = countDoneInWindow(uc0, doneDates, today)
                val pct = ((daysDoneInWindow.toLong() * 100L) / duration)
                    .toInt().coerceIn(0, 100)
                userChallengeRepository.updateProgress(
                    id = userChallengeId,
                    currentStreak = newStreak,
                    bestStreak = newBest,
                    progressPct = pct,
                    lastCheckIn = today
                )

                if (challenge.is_group && uc0.team_id != null) {
                    groupTeamRepository.addCoinsToTeam(uc0.team_id, 1)
                }

                // Run the strict-daily evaluator. It is the only place that flips
                // ACTIVE → COMPLETED / FAILED. Even if newStreak >= target_streak, we
                // do NOT shortcut to COMPLETED here — the evaluator enforces the calendar
                // window strictly and refuses to complete a row that has any gap.
                when (val outcome = evaluateStatusUseCase(userChallengeId)) {
                    is EvaluateChallengeStatusUseCase.Outcome.CompletedNow -> Result.Completed(
                        coinsEarned = outcome.coinsEarned,
                        rewardBadge = outcome.rewardBadge,
                        bonusBadges = outcome.bonusBadges
                    )
                    is EvaluateChallengeStatusUseCase.Outcome.FailedNow ->
                        Result.FailedNow(outcome.firstMissedDate)
                    EvaluateChallengeStatusUseCase.Outcome.AlreadyTerminal ->
                        Result.LoggedAfterTerminal(uc0.status)
                    EvaluateChallengeStatusUseCase.Outcome.NoChange ->
                        Result.Progress(newStreak = daysDoneInWindow, progressPct = pct)
                }
            }
        } catch (e: Exception) {
            Result.Error(e.message ?: "Đã xảy ra lỗi không xác định")
        }

        // Fire-and-forget Firestore sync after a successful local commit. Skip on
        // terminal-history and failure paths: there's nothing new for the leaderboard.
        when (txResult) {
            is Result.Progress -> {
                runCatching { syncMyChallengeScore(userChallengeId, force = false) }
                runCatching { syncGlobalLeaderboard(force = false) }
            }
            is Result.Completed -> {
                runCatching { syncMyChallengeScore(userChallengeId, force = true) }
                runCatching { syncGlobalLeaderboard(force = true) }
            }
            else -> Unit
        }
        return txResult
    }

    /**
     * Count of distinct DONE days inside the strict window `[start_date, today]`. Used
     * for progress_pct. Days the user checked in *outside* the window (e.g., post-FAIL
     * history check-ins from the future) don't bump the meter — that would mislead.
     */
    private fun countDoneInWindow(
        uc: com.example.betterme.data.local.room.entities.UserChallengeEntity,
        doneDates: Set<Long>,
        today: Long
    ): Int {
        val startDay = DateUtils.startOfDay(uc.start_date)
        val targetEnd = uc.target_end_date?.let { DateUtils.startOfDay(it) } ?: today
        val upper = minOf(today, targetEnd)
        if (upper < startDay) return 0
        var cursor = startDay
        var count = 0
        while (cursor <= upper) {
            if (cursor in doneDates) count++
            cursor = DateUtils.plusDays(cursor, 1)
        }
        return count
    }
}
