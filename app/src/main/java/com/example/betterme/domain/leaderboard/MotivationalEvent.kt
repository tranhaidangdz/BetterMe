package com.example.betterme.domain.leaderboard

/**
 * Local-only motivational events emitted when the leaderboard refreshes
 * with a new snapshot. Surfaced as a toast / chip by the screen — never
 * a push notification.
 *
 * Deterministic: events are pure functions of (current snapshot,
 * previous snapshot, last-fired-at map). No randomness, no server
 * decisions. The data layer's event engine produces an ordered list and
 * the VM picks the highest-priority non-cooldown event to show.
 *
 * Priority order (highest first):
 *  1. [EnteredTopRank] — user crossed a milestone rank threshold.
 *  2. [PassedByRival]  — someone moved ahead of the user.
 *  3. [CloseToNextRank] — only N points behind the next-higher rank.
 *  4. [StreakMilestone] — user's streak hit a notable threshold.
 *
 * Cooldown: each event-kind has its own 24h cooldown so we don't spam
 * the user with the same message every Home entry.
 */
sealed class MotivationalEvent {
    abstract val message: String
    abstract val priority: Int
    abstract val kind: Kind

    enum class Kind {
        ENTERED_TOP_RANK,
        PASSED_BY_RIVAL,
        CLOSE_TO_NEXT_RANK,
        STREAK_MILESTONE
    }

    /**
     * Fired when the user enters a "Top N" tier (Top 3, Top 10, Top 50).
     * Higher tiers (Top 3) fire only the rarer top-3 message.
     */
    data class EnteredTopRank(
        val tier: Int,
        override val message: String
    ) : MotivationalEvent() {
        override val priority: Int = if (tier <= 3) 100 else 80
        override val kind: Kind = Kind.ENTERED_TOP_RANK
    }

    /**
     * Fired when a specific rival (the row immediately above the user)
     * moved past them since the last snapshot. Carries the rival's
     * displayName so the UI can attribute it.
     */
    data class PassedByRival(
        val rivalName: String,
        val pointsAhead: Int,
        override val message: String
    ) : MotivationalEvent() {
        override val priority: Int = 70
        override val kind: Kind = Kind.PASSED_BY_RIVAL
    }

    /**
     * Fired when the user is within 20 points of the rank immediately
     * above. The pointsBehind value drives the message text.
     */
    data class CloseToNextRank(
        val pointsBehind: Int,
        val targetRank: Int,
        override val message: String
    ) : MotivationalEvent() {
        override val priority: Int = 50
        override val kind: Kind = Kind.CLOSE_TO_NEXT_RANK
    }

    /**
     * Fired when the user's current streak hits 3, 7, 14, 21, 30, 60
     * days for the first time in this season. Tier captures which
     * milestone fired so the UI can vary the celebration.
     */
    data class StreakMilestone(
        val streakDays: Int,
        override val message: String
    ) : MotivationalEvent() {
        override val priority: Int = 30
        override val kind: Kind = Kind.STREAK_MILESTONE
    }
}
