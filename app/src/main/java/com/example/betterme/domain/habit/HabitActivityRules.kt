package com.example.betterme.domain.habit

/**
 * Pure, side-effect-free rules deciding whether a habit belongs in the Home
 * screen "Đang thực hiện" (Active Habits) section.
 *
 * Extracted out of the ViewModel so the policy is unit-testable in isolation
 * (no Room, no Flows, no Android) and so every surface that needs the same
 * notion of "active" can reuse one source of truth.
 *
 * A habit is ACTIVE iff ALL hold:
 *  - not soft-deleted (abandoned)                         → [isDeleted] == false
 *  - has already started                                  → today >= startDate
 *  - its fixed-duration window hasn't fully elapsed       → today <= endDate
 *  - its journey isn't complete                           → doneCount < durationDays
 *
 * "Failed" / "expired" both collapse to the window-elapsed rule: a fixed-
 * duration habit whose end date has passed leaves the active list regardless of
 * how many days were completed. Open-ended habits (endDate == null) never expire
 * and never auto-complete.
 *
 * All timestamps are epoch-millis. Callers pass day-aligned values (start-of-day)
 * for [today], [startDate], [endDate]; the comparisons are therefore robust
 * across time-of-day and DST — expiration flips exactly at the local midnight
 * boundary, never mid-day.
 */
object HabitActivityRules {

    const val DAY_MS: Long = 24L * 60L * 60L * 1000L

    /** Sentinel duration for open-ended habits — never auto-completes. */
    const val OPEN_ENDED_DURATION: Int = Int.MAX_VALUE

    /**
     * Planned journey length in days, inclusive of both endpoints. A habit that
     * starts and ends on the same day is a 1-day journey. Open-ended habits
     * (null [endDate]) return [OPEN_ENDED_DURATION].
     */
    fun durationDays(startDate: Long, endDate: Long?): Int {
        if (endDate == null) return OPEN_ENDED_DURATION
        return (((endDate - startDate) / DAY_MS) + 1).toInt().coerceAtLeast(1)
    }

    /**
     * True when the fixed-duration window has fully elapsed — i.e. [today]
     * (start-of-day) is strictly past [endDate] (start-of-day of the final
     * active day). The habit stays active through the whole of its end day and
     * expires only once the calendar rolls to the next day. Open-ended habits
     * never expire.
     */
    fun isExpired(endDate: Long?, today: Long): Boolean {
        if (endDate == null) return false
        return today > endDate
    }

    /**
     * True when the journey is complete — the user has logged at least as many
     * DONE days as the planned duration. Open-ended habits never auto-complete.
     */
    fun isComplete(doneCount: Int, startDate: Long, endDate: Long?): Boolean {
        val duration = durationDays(startDate, endDate)
        if (duration == OPEN_ENDED_DURATION) return false
        return doneCount >= duration
    }

    /**
     * True when the habit has not begun yet — [startDate] (start-of-day) is in
     * the future relative to [today]. A habit created to start today is already
     * active (today == startDate).
     */
    fun isNotStarted(startDate: Long, today: Long): Boolean = startDate > today

    /**
     * The single gate the Home screen uses. See class docs for the full rule set.
     */
    fun isActive(
        startDate: Long,
        endDate: Long?,
        doneCount: Int,
        today: Long,
        isDeleted: Boolean
    ): Boolean {
        if (isDeleted) return false
        if (isNotStarted(startDate, today)) return false
        if (isExpired(endDate, today)) return false
        if (isComplete(doneCount, startDate, endDate)) return false
        return true
    }

    /**
     * Completion percentage for the active-habit card. For fixed-duration habits
     * it's doneCount / duration; for open-ended it's doneCount / days-elapsed so
     * the ring still reflects recent consistency. Always clamped to 0..100.
     */
    fun progressPercent(
        startDate: Long,
        endDate: Long?,
        doneCount: Int,
        today: Long
    ): Int {
        val duration = durationDays(startDate, endDate)
        val denominator = if (duration == OPEN_ENDED_DURATION) {
            (((today - startDate) / DAY_MS) + 1).toInt().coerceAtLeast(1)
        } else {
            duration
        }
        return ((doneCount.toFloat() / denominator) * 100).toInt().coerceIn(0, 100)
    }
}
