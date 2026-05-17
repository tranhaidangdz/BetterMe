package com.example.betterme.domain.usecase.leaderboard

import com.example.betterme.domain.leaderboard.MonthlyWinner
import com.example.betterme.domain.repository.GlobalLeaderboardRepository

/**
 * Pulls top-3 winners across the last N finished seasons. Defaults to
 * 6 seasons (half a year of history) — keeps the UI's "previous
 * champions" section browsable without paging.
 */
class GetMonthlyWinnersUseCase(
    private val repository: GlobalLeaderboardRepository
) {
    suspend operator fun invoke(recentSeasonCount: Int = 6): List<MonthlyWinner> =
        repository.getMonthlyWinners(recentSeasonCount)
}
