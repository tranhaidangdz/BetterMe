package com.example.betterme.presentation.home.model

data class HomeProgress(
    val totalHabits: Int = 0,
    val completedHabits: Int = 0,
    val percentage: Int = 0
) {
    companion object {
        fun fake() = HomeProgress(
            totalHabits = 10,
            completedHabits = 8,
            percentage = 85
        )
    }
}
