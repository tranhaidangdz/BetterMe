package com.example.betterme.domain.ai.schedule

/**
 * One detected scheduling problem. The model emits up to 3 of these per analysis.
 *
 * [habitB] is nullable because OVERLOAD, POOR_SLEEP and LATE_NIGHT diagnoses
 * aren't necessarily about a *pair* of habits — they can describe a single
 * habit or a window. The prompt explicitly tells the model to set habitB to
 * null in those cases instead of inventing a second habit name.
 */
data class ScheduleConflict(
    val type: ConflictType,
    val habitA: String,
    val habitB: String?,
    /** 1-sentence Vietnamese. */
    val issue: String,
    /** 1-sentence Vietnamese, encouraging tone. */
    val suggestion: String
)

enum class ConflictType {
    OVERLAP,
    OVERLOAD,
    POOR_SLEEP,
    LATE_NIGHT,
    TRANSITION
}
