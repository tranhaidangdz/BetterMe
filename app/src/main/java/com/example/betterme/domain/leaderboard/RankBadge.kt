package com.example.betterme.domain.leaderboard

/**
 * Badge tiers awarded deterministically based on an entry's position +
 * stats inside one leaderboard snapshot. Multiple badges can fire for
 * the same entry (e.g. rank #1 + 7-day streak = Champion + Streak Master).
 *
 * Why "deterministic": both the AI assistant and unit tests need to
 * reproduce the badge set from the same input. No randomness, no
 * server-side flags, no time-of-day rules. Award purely on the data the
 * repository already has in the [LeaderboardEntry] + the row's rank
 * inside the merged list.
 *
 * UI ordering: when an entry has multiple badges, render highest-tier
 * first (`Champion → Top10 → ConsistencyKing → StreakMaster → FastClimber`).
 * The repository sorts the list before handing it to the UI so each
 * surface stays consistent.
 */
enum class RankBadge(
    val emoji: String,
    val label: String,
    /** Higher = rendered first when multiple badges fire. */
    val priority: Int
) {
    /** Rank #1 in the active season. The headline badge. */
    CHAMPION(emoji = "🥇", label = "Quán quân tháng", priority = 100),

    /** Rank inside top 10% of the leaderboard (rounded up). */
    TOP_10_PERCENT(emoji = "💎", label = "Top 10%", priority = 80),

    /** Current streak ≥ 21 days. The "I show up every day" badge. */
    CONSISTENCY_KING(emoji = "🚀", label = "Consistency King", priority = 60),

    /** Current streak ≥ 7 days. */
    STREAK_MASTER(emoji = "🔥", label = "7-Day Streak Master", priority = 40),

    /** Climbed ≥ 5 ranks since last snapshot. Computed by the repo. */
    FAST_CLIMBER(emoji = "⚡", label = "Fast Climber", priority = 30);

    companion object {

        /**
         * Pure award function. Run by the repository for each entry
         * after it has the merged ranked list, the entry's [RankDelta],
         * and the total participant count.
         *
         * Result is sorted high-to-low priority so the UI can render
         * the first N badges and trust the visual hierarchy.
         */
        fun award(
            rank: Int,
            currentStreak: Int,
            participantCount: Int,
            delta: RankDelta
        ): List<RankBadge> {
            val badges = mutableListOf<RankBadge>()

            // Champion — strict rank == 1.
            if (rank == 1) badges += CHAMPION

            // Top 10% — only when there's a meaningful sample size. We
            // skip it for tiny leaderboards (< 10) because "top 10%" of
            // 5 entries is "top 1 entry" which is just CHAMPION already.
            if (participantCount >= 10) {
                val cutoff = ((participantCount.toFloat() * 0.10f).coerceAtLeast(1f)).toInt()
                if (rank in 2..cutoff) badges += TOP_10_PERCENT
            }

            // Streak badges. Long streak supersedes short streak — never
            // award both to keep the chip row clean.
            when {
                currentStreak >= 21 -> badges += CONSISTENCY_KING
                currentStreak >= 7 -> badges += STREAK_MASTER
            }

            // Fast climber — only fires on a real Up delta.
            if (delta is RankDelta.Up && delta.by >= 5) {
                badges += FAST_CLIMBER
            }

            return badges.sortedByDescending { it.priority }
        }
    }
}
