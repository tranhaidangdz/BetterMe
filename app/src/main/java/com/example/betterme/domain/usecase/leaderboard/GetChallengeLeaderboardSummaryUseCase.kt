package com.example.betterme.domain.usecase.leaderboard

import com.example.betterme.domain.leaderboard.Season
import com.example.betterme.domain.repository.ChallengeLeaderboardRepository
import com.example.betterme.domain.repository.ChallengeLeaderboardRepository.LeaderboardSnapshot

/**
 * Lightweight leaderboard load for embedded cards on the Challenge
 * Overview / Detail screens — top 3 + the current user's rank, no full
 * paging. Same session cache as the full read, so opening the full
 * screen after the summary is instant.
 */
class GetChallengeLeaderboardSummaryUseCase(
    private val repository: ChallengeLeaderboardRepository
) {
    suspend operator fun invoke(
        challengeId: Int,
        seasonKey: String = Season.current(),
        forceRefresh: Boolean = false
    ): LeaderboardSnapshot = repository.getSummary(challengeId, seasonKey, forceRefresh)
}
