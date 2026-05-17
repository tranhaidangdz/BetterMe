package com.example.betterme.domain.leaderboard

/**
 * How a row on the FRIENDS tab relates to the current user. Used by
 * the UI to differentiate styling and by the rival-insight engine to
 * pick relevant narrative beats.
 *
 * Phase 2B note: BetterMe doesn't have a real friend graph yet. The
 * repository synthesizes the friends list from a deterministic subset
 * of seeded global competitors based on score-proximity to the current
 * user — close-score rivals make for better leaderboard drama than a
 * random selection. When a real friend graph lands, this enum gains a
 * `SOCIAL_FRIEND` variant and the repository switches data sources;
 * the UI stays unchanged.
 */
enum class FriendRelationship {
    /** The current user's own entry — sticky-highlighted by the UI. */
    SELF,
    /**
     * A close-score competitor surfaced as the user's "friend" in the
     * absence of a real friend graph. Seeded deterministically.
     */
    RIVAL
}
