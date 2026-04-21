package com.example.betterme.presentation.dailyhabits.model

data class Habit(
    val id: Int,
    val category: String,
    val title: String,
    val time: String,
    val isCompleted: Boolean,
    val icon: String
)
