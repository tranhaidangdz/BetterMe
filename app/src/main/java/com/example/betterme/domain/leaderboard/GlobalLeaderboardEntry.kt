package com.example.betterme.domain.leaderboard

/**
 * One row on the cross-challenge GLOBAL monthly leaderboard. Distinct
 * from [LeaderboardEntry] (which is per-challenge) — this one aggregates
 * a user's activity across ALL habits + challenges in the season.
 *
 * Same shape philosophy as the per-challenge entry: stored fields stay
 * close to what Firestore actually persists; computed-at-read fields
 * ([rank], [rankDelta], [badges], [leagueProgress], [isSuspicious])
 * default to no-op values so the data layer can hydrate the entry
 * before the repository decorates it.
 *
 * Firestore path: `/leaderboards/{seasonKey}/global/{userId}`.
 */
data class GlobalLeaderboardEntry(
    val userId: String,
    val displayName: String,
    val avatarUrl: String?,
    /** Aggregate DONE-log count across every habit the user owns. */
    val totalCompletedHabits: Int,
    /** Longest streak across every habit (NOT just the active ones). */
    val longestStreak: Int,
    /** Lifetime coin balance the user has accumulated this season. */
    val earnedCoins: Int,
    /** Distinct challenges the user has FINISHED this season. */
    val completedChallenges: Int,
    /** 0-100 — derived rolling score from check-in regularity. */
    val monthlyConsistencyBonus: Int,
    val totalScore: Int,
    val updatedAt: Long,

    val rank: Int = 0,
    val isCurrentUser: Boolean = false,
    val isSeededRival: Boolean = false,
    val rankDelta: RankDelta = RankDelta.Hidden,
    val badges: List<RankBadge> = emptyList(),
    val leagueProgress: LeagueProgress = LeagueProgress.bronzeBaseline(),
    val isSuspicious: Boolean = false
)
