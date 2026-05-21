package com.example.betterme.domain.usecase.challenge

import com.example.betterme.data.local.room.entities.UserChallengeEntity
import com.example.betterme.domain.challenge.UserChallengeStatus
import com.example.betterme.domain.repository.ChallengeRepository
import com.example.betterme.domain.repository.GroupTeamRepository
import com.example.betterme.domain.repository.UserChallengeRepository
import com.example.betterme.utils.DateUtils

/**
 * Idempotently joins a user to a challenge.
 *
 * - If a non-terminal row already exists, returns its id.
 * - Otherwise inserts a new UserChallenge with:
 *   - `start_date` normalized to start-of-day (local TZ)
 *   - `target_end_date = start_date + (duration_days - 1) days` (calendar-day stepped so
 *     DST transitions stay aligned)
 *   - status="UPCOMING" if start_date is in the future, else "ACTIVE"
 *   - participant counters bumped
 *
 * The `target_end_date` here is load-bearing — strict-daily validation depends on it.
 * Without it, [EvaluateChallengeStatusUseCase] falls back to deriving from duration_days,
 * but that breaks if the challenge template's duration changes after the join.
 */
class JoinChallengeUseCase(
    private val challengeRepository: ChallengeRepository,
    private val userChallengeRepository: UserChallengeRepository,
    private val groupTeamRepository: GroupTeamRepository
) {

    suspend operator fun invoke(
        userId: String,
        challengeId: Int,
        teamId: Int? = null
    ): Long {
        val challenge = challengeRepository.getById(challengeId)
            ?: error("Challenge $challengeId not found")

        val existing = userChallengeRepository.getByUserAndChallenge(userId, challengeId)
        if (existing != null && !UserChallengeStatus.isTerminal(existing.status)) {
            // Idempotent: already joined and not yet completed/failed/abandoned.
            // Backfill target_end_date if a legacy row is missing it.
            if (existing.target_end_date == null) {
                val backfilled = DateUtils.plusDays(
                    DateUtils.startOfDay(existing.start_date),
                    (challenge.duration_days.coerceAtLeast(1)) - 1
                )
                userChallengeRepository.updateTargetEndDate(existing.id, backfilled)
            }
            return existing.id.toLong()
        }

        val now = System.currentTimeMillis()
        val rawStart = challenge.start_date ?: now
        // start_date and target_end_date are day-precise. A challenge that starts "today"
        // means the entire local day from 00:00 is part of the window.
        val startDay = DateUtils.startOfDay(rawStart)
        val duration = challenge.duration_days.coerceAtLeast(1)
        val targetEndDay = DateUtils.plusDays(startDay, duration - 1)
        val today = DateUtils.startOfDay(now)
        val status = if (startDay > today) UserChallengeStatus.UPCOMING else UserChallengeStatus.ACTIVE

        val newRow = UserChallengeEntity(
            user_id = userId,
            challenge_id = challengeId,
            status = status,
            start_date = startDay,
            target_end_date = targetEndDay,
            team_id = teamId,
            joined_at = now
        )
        val newId = userChallengeRepository.insert(newRow)

        challengeRepository.incrementParticipantCount(challengeId)
        if (teamId != null) {
            groupTeamRepository.incrementMemberCount(teamId)
        }

        return newId
    }
}
