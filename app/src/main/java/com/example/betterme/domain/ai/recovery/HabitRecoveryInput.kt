package com.example.betterme.domain.ai.recovery

/**
 * Input bundle the recovery engine receives. The use case computes per-habit
 * struggle stats from `HabitLogRepository` and passes the bundle here; the
 * AI only ever sees these pre-rolled numbers, never raw log rows.
 *
 * Important fields:
 *  - [strugglingTitles] — the subset of [allHabitStats] that hit at least one
 *    trigger threshold, surfaced separately so the model focuses its
 *    suggestions on the actual problem habits.
 *  - [detectedTriggers] — the trigger reasons the use case already detected
 *    deterministically. Passing these in lets the model write coaching that
 *    matches the actual signal instead of inventing reasons.
 */
data class HabitRecoveryInput(
    val allHabitStats: List<HabitStatRow>,
    val strugglingTitles: List<String>,
    val detectedTriggers: List<RecoveryTrigger>,
    val activeHabitCount: Int,
    val lateNightHabitTitles: List<String>,
    val lifestyle: LifestyleAnchors
)

data class HabitStatRow(
    val title: String,
    /** "HH:mm" or empty when no reminder is set. */
    val reminderTime: String,
    /** EASY | MEDIUM | HARD. Defaults to MEDIUM until HabitEntity stores difficulty. */
    val difficulty: String,
    /** Completion rate over the last 7 days (i.e. "this week"). */
    val completionRate7d: Int,
    /** Rolling completion rate over the last 14 days. */
    val completionRate14d: Int,
    /**
     * Completion rate over days 7-14 ago — "last week" relative to today.
     * Combined with [completionRate7d] this gives a coarse week-over-week
     * delta the prompt can describe ("better than last week", "drifting",
     * "stable"). Defaults to 0 for binary-compat with cached payloads
     * written before this field existed.
     */
    val previousWeekCompletionRate: Int = 0,
    val missStreak: Int
)

data class LifestyleAnchors(
    val sleepStart: String,
    val sleepEnd: String,
    val workStart: String,
    val workEnd: String
)
