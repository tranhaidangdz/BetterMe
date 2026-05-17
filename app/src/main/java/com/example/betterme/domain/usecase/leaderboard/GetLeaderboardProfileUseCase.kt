package com.example.betterme.domain.usecase.leaderboard

import com.example.betterme.domain.leaderboard.LeaderboardProfile
import com.example.betterme.domain.leaderboard.Season
import com.example.betterme.domain.repository.GlobalLeaderboardRepository

/**
 * Loads the profile sheet shown when a leaderboard row is tapped.
 * Repository synthesizes the profile from the current snapshot, so a
 * profile read is essentially a cache lookup.
 */
class GetLeaderboardProfileUseCase(
    private val repository: GlobalLeaderboardRepository
) {
    suspend operator fun invoke(
        userId: String,
        seasonKey: String = Season.current()
    ): LeaderboardProfile? = repository.getProfile(userId, seasonKey)
}
