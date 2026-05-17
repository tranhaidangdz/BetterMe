package com.example.betterme.domain.leaderboard

/**
 * Pure formula for the cross-challenge GLOBAL monthly score.
 *
 *   score = (totalCompletedHabits × 5)
 *         + (longestStreak × 10)
 *         + earnedCoins
 *         + (completedChallenges × 50)
 *         + monthlyConsistencyBonus
 *
 * Design rationale (mirrors the per-challenge [ScoreFormula] philosophy
 * — no multiplicative bonuses, no time-of-day rules, no badge
 * multipliers):
 *
 *  - **completedHabits × 5** — engagement volume, capped by actual
 *    DONE-log count. Can't be inflated by retroactive entries because
 *    the source-of-truth is the Room log table.
 *  - **longestStreak × 10** — disproportionate weight rewards
 *    sustained consistency. We use the *longest* streak across all the
 *    user's habits because surfacing the user's best discipline beat
 *    matches the leaderboard's coaching tone.
 *  - **earnedCoins** — direct pass-through; coins themselves are
 *    derived deterministically by [ScoreFormula.coinsEarned] in the
 *    per-challenge layer.
 *  - **completedChallenges × 50** — the headline "trophies-this-month"
 *    boost. Finishing a challenge is the rarest action a user takes,
 *    so it carries the largest fixed bonus.
 *  - **monthlyConsistencyBonus** — repository-computed 0-100 bonus from
 *    daily check-in regularity inside the season. Provides a smooth
 *    score curve so two users with similar raw stats are differentiated
 *    by whether their effort was even or bursty.
 *
 * No clamps inside the formula — sanitization happens in
 * [LeaderboardIntegrityValidator] so the formula stays a pure
 * arithmetic function the rest of the code can unit-test cheaply.
 */
object GlobalScoreFormula {

    fun compute(
        totalCompletedHabits: Int,
        longestStreak: Int,
        earnedCoins: Int,
        completedChallenges: Int,
        monthlyConsistencyBonus: Int
    ): Int =
        totalCompletedHabits.coerceAtLeast(0) * HABIT_WEIGHT +
            longestStreak.coerceAtLeast(0) * STREAK_WEIGHT +
            earnedCoins.coerceAtLeast(0) +
            completedChallenges.coerceAtLeast(0) * CHALLENGE_WEIGHT +
            monthlyConsistencyBonus.coerceIn(0, 100)

    private const val HABIT_WEIGHT = 5
    private const val STREAK_WEIGHT = 10
    private const val CHALLENGE_WEIGHT = 50
}
