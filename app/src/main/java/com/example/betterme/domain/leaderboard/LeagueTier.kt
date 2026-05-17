package com.example.betterme.domain.leaderboard

/**
 * Monthly tier the user sits in based on their global score. Tiers
 * promote and demote each season — a user at the Gold threshold this
 * month doesn't carry that into next month unless they re-earn it.
 *
 * Thresholds are CONFIGURABLE constants kept on the enum (not pulled
 * from a remote config) so the same tier mapping holds for unit tests,
 * UI previews, and the AI assistant.
 *
 * Tier ordering (ascending): BRONZE < SILVER < GOLD < PLATINUM <
 * DIAMOND < MASTER.
 */
enum class LeagueTier(
    val displayName: String,
    val emoji: String,
    /** Minimum global score to enter this tier. */
    val threshold: Int
) {
    BRONZE("Đồng", "🥉", threshold = 0),
    SILVER("Bạc", "🥈", threshold = 500),
    GOLD("Vàng", "🥇", threshold = 1500),
    PLATINUM("Bạch kim", "💠", threshold = 3500),
    DIAMOND("Kim cương", "💎", threshold = 6500),
    MASTER("Bậc Thầy", "👑", threshold = 10500);

    /** Next tier up, or null when already at the top. */
    val next: LeagueTier? get() = entries.getOrNull(ordinal + 1)

    companion object {
        /**
         * Pick the tier whose threshold the [score] passes. Falls back
         * to [BRONZE] for negative input — the score itself is clamped
         * to ≥ 0 by [GlobalScoreFormula], so we only see negatives in
         * tests.
         */
        fun forScore(score: Int): LeagueTier {
            if (score < 0) return BRONZE
            // Walk top-down — picks the highest tier the score qualifies for.
            return entries.lastOrNull { score >= it.threshold } ?: BRONZE
        }
    }
}
