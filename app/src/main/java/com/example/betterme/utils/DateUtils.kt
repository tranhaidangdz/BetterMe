package com.example.betterme.utils

import java.util.Calendar

object DateUtils {

    /** One calendar day in milliseconds. */
    const val DAY_MS: Long = 24L * 60L * 60L * 1000L

    /** Floor a millis timestamp to start-of-day in the local timezone. */
    fun startOfDay(millis: Long = System.currentTimeMillis()): Long {
        val cal = Calendar.getInstance().apply {
            timeInMillis = millis
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return cal.timeInMillis
    }

    /**
     * Returns the count of consecutive done days ending at [today] (inclusive).
     *
     * If [today] is in [doneDates] → streak counts today.
     * Otherwise → streak counts back from yesterday.
     *
     * Example: [..., d-2, d-1] with today=d → returns 2 (d-1 and d-2, today missing).
     */
    fun currentStreak(doneDates: Collection<Long>, today: Long = startOfDay()): Int {
        if (doneDates.isEmpty()) return 0
        val doneSet = doneDates.toHashSet()
        val cal = Calendar.getInstance().apply { timeInMillis = today }
        var streak = 0
        while (true) {
            val day = startOfDay(cal.timeInMillis)
            if (day !in doneSet) break
            streak++
            cal.add(Calendar.DAY_OF_YEAR, -1)
        }
        return streak
    }

    /** Length of the longest consecutive run of days in [doneDates]. */
    fun longestStreak(doneDates: Collection<Long>): Int {
        if (doneDates.isEmpty()) return 0
        val sorted = doneDates.map { startOfDay(it) }.distinct().sorted()
        var best = 1
        var run = 1
        for (i in 1 until sorted.size) {
            val prevPlusOne = Calendar.getInstance().apply {
                timeInMillis = sorted[i - 1]
                add(Calendar.DAY_OF_YEAR, 1)
            }.timeInMillis
            if (sorted[i] == startOfDay(prevPlusOne)) {
                run++
                if (run > best) best = run
            } else {
                run = 1
            }
        }
        return best
    }

    /** Number of whole days from `from` to `to` (positive when to > from). */
    fun daysBetween(from: Long, to: Long): Int {
        val dayMs = 24L * 60L * 60L * 1000L
        return ((startOfDay(to) - startOfDay(from)) / dayMs).toInt()
    }

    /** True when both timestamps fall on the same calendar day. */
    fun isSameDay(a: Long, b: Long): Boolean = startOfDay(a) == startOfDay(b)

    /**
     * Add `n` calendar days (positive or negative) using a Calendar so DST transitions
     * stay on the correct day. `dayMillis + DAY_MS * n` would land on the wrong day on
     * spring-forward / fall-back days; this never does.
     */
    fun plusDays(dayMillis: Long, n: Int): Long {
        val cal = Calendar.getInstance().apply {
            timeInMillis = dayMillis
            add(Calendar.DAY_OF_YEAR, n)
        }
        return startOfDay(cal.timeInMillis)
    }

    /**
     * Inclusive count of calendar days from `fromDay` (start-of-day) to `toDay`
     * (start-of-day). DST-safe. Returns 0 when fromDay > toDay.
     */
    fun daysInclusive(fromDay: Long, toDay: Long): Int {
        val from = startOfDay(fromDay)
        val to = startOfDay(toDay)
        if (from > to) return 0
        val raw = ((to - from) / DAY_MS).toInt()
        // DST can leave a ±1h sliver in the modulus — snap to nearest whole day.
        return raw + 1
    }
}
