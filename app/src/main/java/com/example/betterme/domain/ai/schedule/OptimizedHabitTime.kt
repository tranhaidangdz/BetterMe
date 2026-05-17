package com.example.betterme.domain.ai.schedule

/**
 * One "you could move this habit to ..." suggestion from the AI.
 *
 * The prompt guarantees [suggestedTime] is HH:mm 24-hour, and the parser
 * rejects anything else so an invalid value never reaches the UI or the
 * "Apply suggestions" path that calls `UpdateHabitUseCase`.
 *
 * [habit] is matched against the user's habit list by title (case-sensitive)
 * when applying. If no habit matches, the suggestion is silently skipped —
 * we'd rather drop one stale suggestion than fail the whole apply.
 */
data class OptimizedHabitTime(
    val habit: String,
    /** 24-hour "HH:mm". */
    val suggestedTime: String
)
