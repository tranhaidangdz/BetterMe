package com.example.betterme.domain.leaderboard

/**
 * One row on the monthly challenge leaderboard. Combined shape used by:
 *  - Firestore datasource (real-user rows hydrated from the entry doc)
 *  - HybridCompetitorSeeder (deterministic fake competitors for realism
 *    before BetterMe has a critical mass of real users)
 *  - Repository merger (real + fake → ranked list)
 *  - UI (podium card, full list)
 *
 * [isCurrentUser] flips for the entry that matches the logged-in user
 * so the UI can sticky-highlight it regardless of rank. [isSeededRival]
 * marks fake competitors — the UI shows them like real users (the whole
 * point of the hybrid strategy) but the field exists so analytics /
 * debug overlays can distinguish them.
 *
 * [rank] is computed by the repository at read time after merging real +
 * seeded entries by descending `totalScore`; it is not stored in
 * Firestore. Ties broken by `updatedAt` ascending — whoever reached the
 * tied score first is ranked higher (rewards consistency).
 */
data class LeaderboardEntry(
    val userId: String,
    val displayName: String,
    val avatarUrl: String?,
    val completedTasks: Int,
    val currentStreak: Int,
    val earnedCoins: Int,
    val totalScore: Int,
    val updatedAt: Long,
    val rank: Int = 0,
    val isCurrentUser: Boolean = false,
    val isSeededRival: Boolean = false,
    /**
     * Phase 2 — rank movement against the previously persisted snapshot.
     * [RankDelta.Hidden] is the safe default for unenhanced reads.
     */
    val rankDelta: RankDelta = RankDelta.Hidden,
    /**
     * Phase 2 — badges awarded deterministically by
     * [RankBadge.award]. Defaults to empty so callers that ignore Phase 2
     * fields continue to work.
     */
    val badges: List<RankBadge> = emptyList(),
    /**
     * Phase 2 — flagged by [com.example.betterme.domain.leaderboard.LeaderboardIntegrityValidator]
     * when the entry's stats are internally inconsistent (e.g. streak >
     * days-since-challenge-start). UI does NOT hide suspicious rows; it
     * just adds a small "?" marker so a human reviewing screenshots can
     * spot anomalies.
     */
    val isSuspicious: Boolean = false
)

/**
 * Per-(seasonKey, challengeId) aggregate the meta document holds. Read
 * separately from the entries list so a partial UI (just the
 * participant count + top score) can render without paging through the
 * full ranking.
 */
data class ChallengeMeta(
    val challengeId: Int,
    val seasonKey: String,
    val participantCount: Int,
    val topScore: Int,
    val updatedAt: Long
)
