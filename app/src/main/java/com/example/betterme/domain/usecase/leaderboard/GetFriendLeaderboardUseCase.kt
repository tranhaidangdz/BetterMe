package com.example.betterme.domain.usecase.leaderboard

import com.example.betterme.domain.leaderboard.Season
import com.example.betterme.domain.repository.GlobalLeaderboardRepository
import com.example.betterme.domain.repository.GlobalLeaderboardRepository.FriendSnapshot

/**
 * Same shape as [GetGlobalLeaderboardUseCase] but for the Friends tab.
 * Repo handles the "pick close-score rivals" logic; this use case is
 * a delegation layer.
 */
class GetFriendLeaderboardUseCase(
    private val repository: GlobalLeaderboardRepository
) {
    suspend operator fun invoke(
        seasonKey: String = Season.current(),
        forceRefresh: Boolean = false
    ): FriendSnapshot = repository.getFriends(seasonKey, forceRefresh)
}
