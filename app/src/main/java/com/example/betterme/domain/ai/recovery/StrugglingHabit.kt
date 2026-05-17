package com.example.betterme.domain.ai.recovery

/**
 * One habit the engine has flagged as struggling. The use case computes the
 * raw stats (completion rates over 7d / 14d, current miss streak); the AI
 * fills in [recoveryReason] with a 1-sentence Vietnamese narrative line.
 *
 * [completionRate7d] / [completionRate14d] are 0..100. The use case caps
 * the denominator at the days since the habit's start_date so a 3-day-old
 * habit isn't judged against 14 days.
 *
 * [missStreak] = consecutive days without a DONE log ending at yesterday
 * (or today if the user hasn't checked in yet). Acts as both "skip streak"
 * and "consecutive failures" — they're the same signal under BetterMe's
 * MISSED-by-absence log model.
 */
data class StrugglingHabit(
    val title: String,
    val completionRate7d: Int,
    val completionRate14d: Int,
    val missStreak: Int,
    /** AI-filled. 1-sentence Vietnamese explanation. */
    val recoveryReason: String
)
