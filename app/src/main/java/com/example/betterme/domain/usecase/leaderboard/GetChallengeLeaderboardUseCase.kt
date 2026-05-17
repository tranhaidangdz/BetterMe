package com.example.betterme.domain.usecase.leaderboard

import com.example.betterme.domain.leaderboard.Season
import com.example.betterme.domain.repository.ChallengeLeaderboardRepository
import com.example.betterme.domain.repository.ChallengeLeaderboardRepository.LeaderboardSnapshot

/**
 * Loads the monthly leaderboard for a challenge. Thin pass-through to
 * [ChallengeLeaderboardRepository.getLeaderboard]; lives here so the VM
 * never imports a repository directly + the season-key default can
 * evolve without VM churn.
 */
class GetChallengeLeaderboardUseCase(
    private val repository: ChallengeLeaderboardRepository
) {
    suspend operator fun invoke(
        challengeId: Int,
        seasonKey: String = Season.current(),
        limit: Int = 50,
        forceRefresh: Boolean = false
    ): LeaderboardSnapshot = repository.getLeaderboard(challengeId, seasonKey, limit, forceRefresh)
}
