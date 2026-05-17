package com.example.betterme.domain.leaderboard

/**
 * Lightweight public profile shown when a user taps any leaderboard
 * row. Phase 2B scope: data-only profile bottom sheet — no chat, no
 * follow, no DM. The user sees who they're competing with and what
 * that user's headline stats look like.
 *
 * Fields:
 *  - [favoriteChallengeTitle] — the user's most-active challenge this
 *    season (highest check-in count). Null when the user has no
 *    challenges joined.
 *  - [totalCompletedHabits] / [longestStreak] / [completedChallenges]
 *    mirror [GlobalLeaderboardEntry] so the same numbers the user sees
 *    on the leaderboard match the profile.
 *
 * For seeded rivals (the bulk of profile views in early BetterMe), the
 * repository fakes these stats deterministically so the profile feels
 * complete; for the real current user, they're sourced from Room.
 */
data class LeaderboardProfile(
    val userId: String,
    val displayName: String,
    val avatarUrl: String?,
    val favoriteChallengeTitle: String?,
    val totalCompletedHabits: Int,
    val longestStreak: Int,
    val completedChallenges: Int,
    val currentRank: Int,
    val totalScore: Int,
    val badges: List<RankBadge>,
    val leagueProgress: LeagueProgress
)
