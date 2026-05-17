package com.example.betterme.domain.usecase.leaderboard

import com.example.betterme.domain.leaderboard.Season
import com.example.betterme.domain.repository.GlobalLeaderboardRepository
import com.example.betterme.domain.repository.GlobalLeaderboardRepository.GlobalSnapshot

/**
 * Thin facade for VMs to read the GLOBAL leaderboard without ever
 * importing the repository directly. Defaults to the current season
 * key so callers don't have to compute it.
 */
class GetGlobalLeaderboardUseCase(
    private val repository: GlobalLeaderboardRepository
) {
    suspend operator fun invoke(
        seasonKey: String = Season.current(),
        limit: Int = 100,
        forceRefresh: Boolean = false
    ): GlobalSnapshot = repository.getGlobal(seasonKey, limit, forceRefresh)
}
