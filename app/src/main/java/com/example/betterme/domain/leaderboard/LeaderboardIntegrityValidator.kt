package com.example.betterme.domain.leaderboard

import java.util.Calendar

/**
 * Pure validator that clamps internally inconsistent leaderboard
 * entries before they're shown. Defends against:
 *
 *   1. Streak inflation     — current streak > days since challenge
 *                              start (or > days since season start).
 *   2. Task overcount       — completedTasks > days since season start.
 *   3. Score drift          — totalScore deviates from the formula's
 *                              actual computation by more than a small
 *                              tolerance (e.g. someone wrote a tampered
 *                              entry directly to Firestore).
 *
 * The validator NEVER throws. Suspicious entries get their fields
 * clamped to plausible values AND their [LeaderboardEntry.isSuspicious]
 * flag flipped. The UI uses the flag to surface a tiny "?" marker
 * without hiding the row — preserves the leaderboard's visual rhythm
 * even on weird data.
 *
 * @param now           Current wall-clock for deriving the season's
 *                      day-count. Injectable for tests.
 * @param challengeStartMs The Room-side `start_date` for the user's
 *                      UserChallenge row, if known. Used to clamp the
 *                      CURRENT user's row only — seeded rivals don't
 *                      have a `start_date`, so we cap their streak by
 *                      the season day count instead.
 */
object LeaderboardIntegrityValidator {

    /** Drift tolerance for totalScore — a few points of float rounding is fine. */
    private const val SCORE_DRIFT_TOLERANCE = 5

    /**
     * Validate one entry. Returns either the original entry or a
     * sanitized copy with `isSuspicious = true`.
     */
    fun validate(
        entry: LeaderboardEntry,
        seasonKey: String,
        currentUserChallengeStartMs: Long? = null,
        now: Long = System.currentTimeMillis()
    ): LeaderboardEntry {
        val seasonDays = daysInSeasonSoFar(seasonKey, now).coerceAtLeast(1)

        // Streak cap depends on whether this is the current user (we
        // have a real start_date) or a seeded rival (we don't).
        val streakCap = if (entry.isCurrentUser && currentUserChallengeStartMs != null) {
            val challengeDays = daysSince(currentUserChallengeStartMs, now).coerceAtLeast(1)
            minOf(challengeDays, seasonDays)
        } else {
            seasonDays
        }

        var suspicious = false
        var clampedStreak = entry.currentStreak
        if (clampedStreak > streakCap) {
            suspicious = true
            clampedStreak = streakCap
        }
        if (clampedStreak < 0) {
            suspicious = true
            clampedStreak = 0
        }

        var clampedTasks = entry.completedTasks
        if (clampedTasks > seasonDays) {
            suspicious = true
            clampedTasks = seasonDays
        }
        if (clampedTasks < 0) {
            suspicious = true
            clampedTasks = 0
        }

        // Recompute score from the clamped components and compare to
        // the entry's stored totalScore. If the deviation is bigger than
        // the tolerance, the score was either tampered with or computed
        // by an older formula — re-derive it and flag.
        val expected = ScoreFormula.compute(
            completedTasks = clampedTasks,
            currentStreak = clampedStreak,
            earnedCoins = entry.earnedCoins.coerceAtLeast(0)
        )
        var clampedScore = entry.totalScore
        if (kotlin.math.abs(entry.totalScore - expected) > SCORE_DRIFT_TOLERANCE) {
            suspicious = true
            clampedScore = expected
        }

        return if (!suspicious) {
            entry
        } else {
            entry.copy(
                completedTasks = clampedTasks,
                currentStreak = clampedStreak,
                totalScore = clampedScore,
                earnedCoins = entry.earnedCoins.coerceAtLeast(0),
                isSuspicious = true
            )
        }
    }

    /** Days from the 1st of the month named by [seasonKey] to [now]. */
    private fun daysInSeasonSoFar(seasonKey: String, now: Long): Int {
        val parts = seasonKey.split("-")
        if (parts.size != 2) return 30
        val year = parts[0].toIntOrNull() ?: return 30
        val month = (parts[1].toIntOrNull() ?: return 30) - 1
        val cal = Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month)
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val start = cal.timeInMillis
        if (now < start) return 1
        return daysSince(start, now)
    }

    private fun daysSince(fromMs: Long, now: Long): Int =
        (((now - fromMs) / DAY_MS) + 1).toInt().coerceAtLeast(1)

    private const val DAY_MS: Long = 24L * 60L * 60L * 1000L
}
