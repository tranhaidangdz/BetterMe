package com.example.betterme.domain.leaderboard

/**
 * Per-user tier state: current tier + progress (0-100) toward the next
 * promotion threshold. Computed deterministically from a score; the
 * model itself is just a container.
 *
 * [progressPercent] reflects how much of the current-to-next-tier
 * range the user has covered. At [LeagueTier.MASTER] (the cap),
 * progress is 100 because there's nothing higher to climb to.
 */
data class LeagueProgress(
    val tier: LeagueTier,
    /** 0-100 — distance into the current tier's score band. */
    val progressPercent: Int,
    /** Score needed for the next promotion (0 when at MASTER). */
    val pointsToNextTier: Int
) {

    companion object {

        /**
         * Default state for entries whose score isn't computed yet
         * (e.g. cache miss, partial hydration). Lets the UI render
         * something instead of branching on null.
         */
        fun bronzeBaseline(): LeagueProgress = LeagueProgress(
            tier = LeagueTier.BRONZE,
            progressPercent = 0,
            pointsToNextTier = LeagueTier.SILVER.threshold
        )

        /**
         * Pure derivation from a global score. Returns the user's
         * current tier + how far they are into it.
         */
        fun fromScore(score: Int): LeagueProgress {
            val tier = LeagueTier.forScore(score)
            val next = tier.next
            return if (next == null) {
                // MASTER — capped.
                LeagueProgress(tier, progressPercent = 100, pointsToNextTier = 0)
            } else {
                val bandSize = (next.threshold - tier.threshold).coerceAtLeast(1)
                val intoBand = (score - tier.threshold).coerceIn(0, bandSize)
                LeagueProgress(
                    tier = tier,
                    progressPercent = ((intoBand.toFloat() / bandSize) * 100).toInt().coerceIn(0, 100),
                    pointsToNextTier = (next.threshold - score).coerceAtLeast(0)
                )
            }
        }
    }
}
