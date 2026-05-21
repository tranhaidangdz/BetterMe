package com.example.betterme.domain.usecase.challenge

import com.example.betterme.data.local.room.entities.AchievementEntity
import com.example.betterme.data.local.room.entities.ChallengeEntity
import com.example.betterme.data.local.room.entities.UserChallengeEntity
import com.example.betterme.domain.challenge.UserChallengeStatus
import com.example.betterme.domain.repository.ChallengeLogRepository
import com.example.betterme.domain.repository.ChallengeRepository
import com.example.betterme.domain.repository.UserChallengeRepository
import com.example.betterme.utils.DateUtils

/**
 * Strict-daily challenge validator. Single source of truth for whether a UserChallenge
 * should remain ACTIVE, transition to FAILED, or transition to COMPLETED.
 *
 * Strict-daily rule:
 *  - The window is `[start_date, target_end_date]` inclusive, calendar-day-precise (local TZ).
 *  - Every day in the window must have at least one DONE log.
 *  - Multiple DONE logs on the same day collapse to one (Set semantics).
 *  - On any required day in `[start_date, yesterday-or-target_end_date]` with NO DONE log:
 *    → permanently FAILED.
 *  - When `today >= target_end_date` and every day in the window has a DONE log:
 *    → COMPLETED.
 *  - Otherwise → ACTIVE (or UPCOMING flips to ACTIVE once today >= start_date).
 *
 * Permanence guarantees:
 *  - Terminal statuses (COMPLETED, FAILED, ABANDONED) are never overwritten by this
 *    evaluator. The DAO update queries also guard against this.
 *  - FAILED never recovers to COMPLETED even if the user later checks in every remaining
 *    day — the gap is recorded historically.
 *
 * Timezone safety:
 *  - All comparisons go through [DateUtils.startOfDay], which floors to the local
 *    timezone's midnight. Day stepping uses [DateUtils.plusDays] (Calendar-backed) so
 *    DST transitions stay on the correct calendar day.
 *
 * Testability:
 *  - The pure decision step lives in [decide]. It takes only primitive inputs (no repo
 *    dependencies) and returns a [Decision]. Side effects (DAO writes, award flow) are
 *    applied separately in [apply]. Unit tests target [decide] directly.
 */
class EvaluateChallengeStatusUseCase(
    private val userChallengeRepository: UserChallengeRepository,
    private val challengeRepository: ChallengeRepository,
    private val challengeLogRepository: ChallengeLogRepository,
    private val awardCompletionUseCase: AwardChallengeCompletionUseCase
) {

    sealed class Outcome {
        /** No status change; row stays ACTIVE / UPCOMING. */
        data object NoChange : Outcome()
        /** Status was just transitioned to FAILED. */
        data class FailedNow(val firstMissedDate: Long) : Outcome()
        /** Status was just transitioned to COMPLETED; awards (coins, badges) were granted. */
        data class CompletedNow(
            val coinsEarned: Int,
            val rewardBadge: AchievementEntity?,
            val bonusBadges: List<AchievementEntity>
        ) : Outcome()
        /** Row was already terminal (COMPLETED / FAILED / ABANDONED) — no-op. */
        data object AlreadyTerminal : Outcome()
    }

    /**
     * Pure decision result. Carries enough information to apply the right side effect
     * but performs no IO itself. Lives outside [Outcome] so tests can pattern-match on
     * it without constructing AchievementEntity instances.
     */
    sealed class Decision {
        data object KeepActive : Decision()
        data object AlreadyTerminal : Decision()
        /** Transition to FAILED. `endDate` is the day to stamp on the row. */
        data class TransitionFailed(val endDate: Long, val firstMissedDate: Long) : Decision()
        /** Transition to COMPLETED. `endDate` is the day to stamp on the row. */
        data class TransitionCompleted(val endDate: Long) : Decision()
    }

    suspend operator fun invoke(
        userChallengeId: Int,
        now: Long = System.currentTimeMillis()
    ): Outcome {
        val uc = userChallengeRepository.getById(userChallengeId) ?: return Outcome.NoChange
        val challenge = challengeRepository.getById(uc.challenge_id) ?: return Outcome.NoChange
        val doneDates = challengeLogRepository.getDoneDates(userChallengeId)
            .map { DateUtils.startOfDay(it) }
            .toSet()
        return invoke(uc, challenge, doneDates, now)
    }

    suspend operator fun invoke(
        uc: UserChallengeEntity,
        challenge: ChallengeEntity,
        doneDates: Set<Long>,
        now: Long = System.currentTimeMillis()
    ): Outcome {
        val decision = decide(
            status = uc.status,
            startDate = uc.start_date,
            targetEndDate = uc.target_end_date,
            durationDays = challenge.duration_days,
            doneDates = doneDates,
            now = now
        )
        return apply(uc, challenge, decision)
    }

    private suspend fun apply(
        uc: UserChallengeEntity,
        challenge: ChallengeEntity,
        decision: Decision
    ): Outcome = when (decision) {
        Decision.KeepActive -> Outcome.NoChange
        Decision.AlreadyTerminal -> Outcome.AlreadyTerminal
        is Decision.TransitionFailed -> {
            val flipped = userChallengeRepository.markFailed(uc.id, decision.endDate)
            if (flipped) Outcome.FailedNow(decision.firstMissedDate) else Outcome.AlreadyTerminal
        }
        is Decision.TransitionCompleted -> {
            val flipped = userChallengeRepository.markCompleted(uc.id, decision.endDate)
            if (flipped) {
                val reloaded = userChallengeRepository.getById(uc.id) ?: uc
                val award = awardCompletionUseCase(reloaded, challenge)
                Outcome.CompletedNow(
                    coinsEarned = award.coinsEarned,
                    rewardBadge = award.rewardBadge,
                    bonusBadges = award.bonusBadges
                )
            } else {
                Outcome.AlreadyTerminal
            }
        }
    }

    companion object {

        /**
         * Pure decision over the strict-daily rule. No IO — safe to call from unit
         * tests with hand-rolled input sets.
         *
         * @param status          Current row status (one of [UserChallengeStatus]).
         * @param startDate       Row start (any millis on the start calendar day).
         * @param targetEndDate   Row deadline (any millis on the deadline calendar day);
         *                        null → derived from `durationDays` (legacy rows).
         * @param durationDays    Challenge length in days; ≥1.
         * @param doneDates       Set of `startOfDay` millis for days with ≥1 DONE log.
         * @param now             Wall-clock for "today".
         */
        fun decide(
            status: String,
            startDate: Long,
            targetEndDate: Long?,
            durationDays: Int,
            doneDates: Set<Long>,
            now: Long
        ): Decision {
            if (UserChallengeStatus.isTerminal(status)) return Decision.AlreadyTerminal

            val today = DateUtils.startOfDay(now)
            val startDay = DateUtils.startOfDay(startDate)
            if (today < startDay) return Decision.KeepActive

            val targetEnd = targetEndDate?.let { DateUtils.startOfDay(it) }
                ?: DateUtils.plusDays(startDay, durationDays.coerceAtLeast(1) - 1)

            // Required-by-yesterday days: every day from start through min(yesterday,
            // targetEnd) must have a DONE log. "Today" is not yet required.
            val yesterday = DateUtils.plusDays(today, -1)
            val mustHaveDoneUpper = minOf(yesterday, targetEnd)
            if (mustHaveDoneUpper >= startDay) {
                var cursor = startDay
                while (cursor <= mustHaveDoneUpper) {
                    if (cursor !in doneDates) {
                        return Decision.TransitionFailed(
                            endDate = cursor,
                            firstMissedDate = cursor
                        )
                    }
                    cursor = DateUtils.plusDays(cursor, 1)
                }
            }

            // On the deadline day itself: if today's check-in has already landed, complete
            // immediately. Otherwise stay ACTIVE — the user has the rest of the day.
            if (today == targetEnd && targetEnd in doneDates) {
                return Decision.TransitionCompleted(endDate = today)
            }

            // Past the deadline: check the full window and either complete (every day
            // including the deadline has a DONE) or fail on the deadline day.
            if (today > targetEnd) {
                var c = startDay
                var allDone = true
                while (c <= targetEnd) {
                    if (c !in doneDates) { allDone = false; break }
                    c = DateUtils.plusDays(c, 1)
                }
                return if (allDone) {
                    Decision.TransitionCompleted(endDate = targetEnd)
                } else {
                    Decision.TransitionFailed(endDate = targetEnd, firstMissedDate = targetEnd)
                }
            }

            return Decision.KeepActive
        }
    }
}
