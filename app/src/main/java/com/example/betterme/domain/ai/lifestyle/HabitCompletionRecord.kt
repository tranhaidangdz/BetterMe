package com.example.betterme.domain.ai.lifestyle

/**
 * Per-habit rollup over the analysis window (typically last 14 days). The use
 * case computes this from `HabitLogRepository` and passes a list of these into
 * the runtime user prompt.
 *
 * [completionRate] is the percentage of *active* days (since the habit's start)
 * where a DONE log exists — 0..100. The use case caps the divisor at the
 * window size so a brand-new habit isn't penalized for the days before it
 * existed.
 *
 * [preferredTime] mirrors `HabitEntity.reminder_time` (HH:mm) or empty when
 * no reminder is set. The model uses it to reason about when in the day the
 * user is succeeding/failing.
 *
 * [difficulty] defaults to "MEDIUM" because `HabitEntity` doesn't yet store
 * a real difficulty. Once Add Habit captures this, only the use case's
 * translator changes.
 */
data class HabitCompletionRecord(
    val title: String,
    /** 0..100, percentage of active days with a DONE log. */
    val completionRate: Int,
    /** "HH:mm" 24-hour, or empty when no reminder is set. */
    val preferredTime: String,
    /** EASY | MEDIUM | HARD. */
    val difficulty: String
)
