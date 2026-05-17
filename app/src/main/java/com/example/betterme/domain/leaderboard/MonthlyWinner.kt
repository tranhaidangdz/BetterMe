package com.example.betterme.domain.leaderboard

/**
 * A frozen top-3 winner snapshot from a finished season. Derived at
 * read time from the global leaderboard collection for past seasons —
 * we don't write a separate archive document, the source-of-truth IS
 * `/leaderboards/{seasonKey}/global/`.
 *
 * Why no separate archive write path: deriving from the live
 * collection means we never need a server cron or admin script to
 * snapshot at season end. Past seasons are already "archived" simply
 * by being past; reading them is just `where seasonKey != current`.
 *
 * [seasonKey] is the "yyyy-MM" the user won. [rank] is 1, 2, or 3.
 */
data class MonthlyWinner(
    val seasonKey: String,
    val userId: String,
    val displayName: String,
    val avatarUrl: String?,
    val rank: Int,
    val totalScore: Int,
    val leagueTier: LeagueTier,
    val badges: List<RankBadge>,
    /** Pretty Vietnamese label for the season ("Tháng 5, 2026"). */
    val displayLabel: String
)
