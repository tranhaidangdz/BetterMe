package com.example.betterme.domain.usecase.challenge

import com.example.betterme.domain.repository.UserChallengeRepository

/**
 * Marks a UserChallenge as ABANDONED. The row is preserved (so history & logs survive).
 */
class LeaveChallengeUseCase(
    private val userChallengeRepository: UserChallengeRepository
) {

    suspend operator fun invoke(userChallengeId: Int) {
        userChallengeRepository.markAbandoned(
            id = userChallengeId,
            endDate = System.currentTimeMillis()
        )
    }
}
