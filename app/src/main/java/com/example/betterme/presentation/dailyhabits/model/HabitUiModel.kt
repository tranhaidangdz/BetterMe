package com.example.betterme.presentation.dailyhabits.model

/**
 * Row model shown on the Daily Habits / Tasks tab.
 *
 * The two boolean flags model a deliberately distinct pair of states:
 * - [isCheckedInToday] — was this habit checked in for the *currently selected date*.
 *   Tells the row whether to show the daily green tick.
 * - [isJourneyComplete] — has the habit's overall journey finished, i.e. the user has
 *   accumulated enough DONE check-ins to satisfy the habit's planned duration.
 *   Drives the "Đã hoàn thành" filter and lets us route a finished habit out of
 *   "Đang thực hiện" the moment its last check-in lands.
 */
data class HabitUiModel(
    val id: Int,
    val category: String,
    val title: String,
    val time: String,
    val statusLabel: String,
    val isCheckedInToday: Boolean,
    val isJourneyComplete: Boolean,
    val icon: String
)
