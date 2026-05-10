package com.example.betterme.domain.usecase.challenge

import com.example.betterme.data.local.room.entities.UserChallengeEntity
import com.example.betterme.domain.repository.ChallengeRepository
import com.example.betterme.domain.repository.GroupTeamRepository
import com.example.betterme.domain.repository.UserChallengeRepository

/**
 * Idempotently joins a user to a challenge.
 *
 * - If a non-COMPLETED, non-ABANDONED row already exists, returns its id.
 * - Otherwise inserts a new UserChallenge with status="UPCOMING" if challenge.start_date is in
 *   the future, else "ACTIVE", and bumps participant counters.
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
        if (existing != null && (existing.status == "ACTIVE" || existing.status == "UPCOMING")) {
            return existing.id.toLong()
        }

        val now = System.currentTimeMillis()
        val startDate = challenge.start_date ?: now
        val status = if (startDate > now) "UPCOMING" else "ACTIVE"

        val newRow = UserChallengeEntity(
            user_id = userId,
            challenge_id = challengeId,
            status = status,
            start_date = startDate,
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
