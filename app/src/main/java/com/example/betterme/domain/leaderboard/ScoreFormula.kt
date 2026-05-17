package com.example.betterme.domain.leaderboard

/**
 * Pure formula for the monthly challenge leaderboard score.
 *
 *   score = (completedTasks * 10) + (currentStreak * 5) + earnedCoins
 *
 * Why this shape:
 *  - completedTasks * 10  — rewards engagement volume, but capped by the
 *    user's actual check-in count so it can't be inflated.
 *  - currentStreak * 5    — rewards consistency. The streak is the
 *    user's CURRENT consecutive-done run for the challenge, so it
 *    decays naturally if the user stops checking in.
 *  - earnedCoins          — derived proportionally from the challenge's
 *    `reward_coins` × completion ratio (see [coinsEarned] below) so the
 *    score scales linearly with engagement instead of being a 0/N step
 *    at completion time.
 *
 * No multiplicative bonuses, no time-of-day bonuses, no badge
 * multipliers — keeps the leaderboard easy to reason about and resistant
 * to gaming. If a user wants to climb, they just need to check in
 * consistently. That matches BetterMe's anti-toxic-productivity stance.
 */
object ScoreFormula {

    /** Final score the leaderboard ranks by. */
    fun compute(completedTasks: Int, currentStreak: Int, earnedCoins: Int): Int =
        completedTasks * TASK_WEIGHT +
            currentStreak * STREAK_WEIGHT +
            earnedCoins.coerceAtLeast(0)

    /**
     * Coins earned this season for a challenge, derived proportionally
     * from the challenge's reward and the user's check-in progress.
     *
     * Rationale: BetterMe's coin model only mints coins once, on full
     * completion. That makes `earnedCoins` a step function (0 until the
     * challenge is finished). For a leaderboard that should reward
     * day-to-day engagement, a linear proxy is more useful — 50%
     * progress earns 50% of the reward, 100% earns the full amount.
     *
     * Edge cases:
     *  - targetStreak ≤ 0 → return 0 (avoid divide-by-zero on bad data).
     *  - checkInsThisSeason > targetStreak → still cap at rewardCoins.
     */
    fun coinsEarned(rewardCoins: Int, checkInsThisSeason: Int, targetStreak: Int): Int {
        if (rewardCoins <= 0 || targetStreak <= 0) return 0
        val ratio = (checkInsThisSeason.toFloat() / targetStreak).coerceIn(0f, 1f)
        return (rewardCoins * ratio).toInt()
    }

    private const val TASK_WEIGHT = 10
    private const val STREAK_WEIGHT = 5
}
