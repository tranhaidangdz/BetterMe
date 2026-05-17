package com.example.betterme.domain.repository

import com.example.betterme.domain.leaderboard.ChallengeMeta
import com.example.betterme.domain.leaderboard.LeaderboardEntry

/**
 * Single entry point for the monthly challenge leaderboard. Lives in
 * the domain layer so use cases never see Firestore types; the impl
 * (in `data/leaderboard/`) handles the Firestore I/O and the hybrid
 * merge with seeded competitors.
 *
 * The "hybrid" part: the impl combines real Firestore rows (one per
 * actual user who has checked in this season) with deterministic
 * client-side seeded competitors. The merge happens at read time only —
 * seeded competitors are never written to Firestore.
 */
interface ChallengeLeaderboardRepository {

    /**
     * Top-[limit] entries for one challenge in one season, descending by
     * totalScore. The current user's entry is always included even if
     * outside the top [limit] — the UI uses [LeaderboardEntry.isCurrentUser]
     * to sticky-highlight it.
     *
     * @param forceRefresh bypass the in-memory session cache.
     */
    suspend fun getLeaderboard(
        challengeId: Int,
        seasonKey: String,
        limit: Int = 50,
        forceRefresh: Boolean = false
    ): LeaderboardSnapshot

    /**
     * Lightweight version of [getLeaderboard] for the Challenge Overview
     * / Detail cards — only the top 3 + the current user's rank, no
     * paging. Same cache as the full version.
     */
    suspend fun getSummary(
        challengeId: Int,
        seasonKey: String,
        forceRefresh: Boolean = false
    ): LeaderboardSnapshot

    /**
     * Upsert the current user's entry. Composes the score from the
     * fields passed in and writes:
     *  - the entry doc at /leaderboards/{seasonKey}/challenges/{challengeId}/entries/{userId}
     *  - the meta doc (updates participantCount + topScore if changed).
     *
     * The impl maintains a per-(userId, challengeId) lastWriteAt to
     * skip writes within [throttleMs] of the previous one. Pass
     * `force = true` to bypass the throttle (e.g. on challenge completion
     * where the streak just landed and the user expects to see it).
     */
    suspend fun upsertMyEntry(
        challengeId: Int,
        seasonKey: String,
        displayName: String,
        avatarUrl: String?,
        completedTasks: Int,
        currentStreak: Int,
        earnedCoins: Int,
        force: Boolean = false
    )

    /** Pure read of the meta doc without touching the entries list. */
    suspend fun getMeta(challengeId: Int, seasonKey: String): ChallengeMeta?

    /** Reset session-memory cache. Used by sign-out and tests. */
    fun invalidateCache()

    /**
     * Snapshot returned by getLeaderboard / getSummary. Carries the
     * entries already merged + ranked, plus the meta block, plus a
     * convenience pointer to the current user's row.
     *
     * [isStale] flips to true when the impl is returning data from the
     * cache after a failed live fetch — the UI shows an "offline" chip
     * in that case so users know what they're looking at.
     */
    data class LeaderboardSnapshot(
        val entries: List<LeaderboardEntry>,
        val meta: ChallengeMeta,
        val myEntry: LeaderboardEntry?,
        val isStale: Boolean = false
    )
}
