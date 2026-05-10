package com.example.betterme.presentation.home.model

data class HomeProgress(
    val totalHabits: Int = 0,
    val completedHabits: Int = 0,
    val percentage: Int = 0,
    // Today's challenge check-in progress (active challenges only)
    val totalChallenges: Int = 0,
    val checkedInChallenges: Int = 0
) {
    val challengePercentage: Int
        get() = if (totalChallenges == 0) 0
        else ((checkedInChallenges.toFloat() / totalChallenges) * 100f).toInt()

    companion object {
        fun fake() = HomeProgress(
            totalHabits = 10,
            completedHabits = 8,
            percentage = 85,
            totalChallenges = 3,
            checkedInChallenges = 2
        )
    }
}
