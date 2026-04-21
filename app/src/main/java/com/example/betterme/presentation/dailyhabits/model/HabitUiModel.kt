package com.example.betterme.presentation.dailyhabits.model

data class HabitUiModel(
    val id: Int,
    val category: String,
    val title: String,
    val time: String,
    val statusLabel: String,
    val isCompleted: Boolean,
    val icon: String,
    val startDateMillis: Long,
    val endDateMillis: Long?,
    val daysRemaining: Int?
)
