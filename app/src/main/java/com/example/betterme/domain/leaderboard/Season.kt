package com.example.betterme.domain.leaderboard

import java.util.Calendar
import java.util.Locale

/**
 * Helpers for the monthly leaderboard season key.
 *
 * A season is a calendar month in the device's local timezone. The key
 * format `yyyy-MM` is chosen for natural string ordering — "2026-05"
 * sorts before "2026-06" alphabetically, which makes Firestore queries
 * + UI sorting trivial.
 *
 * Why local timezone (not UTC): users expect "this month" to match their
 * calendar. A user checking in at 23:30 on May 31 in Vietnam should not
 * find their entry on June's leaderboard just because UTC has rolled
 * over. Cross-timezone consistency matters less than user-perceived
 * "this month".
 */
object Season {

    /** Current season key in the device's local timezone, "yyyy-MM". */
    fun current(now: Long = System.currentTimeMillis()): String = format(now)

    /** Format an arbitrary timestamp into a season key. */
    fun format(timeMs: Long): String {
        val cal = Calendar.getInstance().apply { timeInMillis = timeMs }
        val year = cal.get(Calendar.YEAR)
        val month = cal.get(Calendar.MONTH) + 1
        return String.format(Locale.US, "%04d-%02d", year, month)
    }

    /**
     * The list of [count] most recent season keys ending at the current
     * season, newest first. Useful for the leaderboard screen's monthly
     * filter pill row. count = 6 → "2026-05", "2026-04", … back 6 months.
     */
    fun recent(count: Int, now: Long = System.currentTimeMillis()): List<String> {
        require(count >= 1) { "count must be ≥ 1" }
        val cal = Calendar.getInstance().apply { timeInMillis = now }
        return (0 until count).map {
            val key = String.format(
                Locale.US,
                "%04d-%02d",
                cal.get(Calendar.YEAR),
                cal.get(Calendar.MONTH) + 1
            )
            cal.add(Calendar.MONTH, -1)
            key
        }
    }

    /**
     * Best-effort label for a season key — "Tháng 5, 2026" in Vietnamese.
     * Returns the raw key if parsing fails so the UI never breaks on
     * malformed input.
     */
    fun displayLabel(seasonKey: String): String {
        val parts = seasonKey.split("-")
        if (parts.size != 2) return seasonKey
        val year = parts[0].toIntOrNull() ?: return seasonKey
        val month = parts[1].toIntOrNull() ?: return seasonKey
        if (month !in 1..12) return seasonKey
        return "Tháng $month, $year"
    }

    /** True when [key] equals the current season. */
    fun isCurrent(key: String, now: Long = System.currentTimeMillis()): Boolean =
        key == current(now)
}
