package com.example.betterme.domain.repository

import com.example.betterme.domain.leaderboard.FriendRelationship
import com.example.betterme.domain.leaderboard.GlobalLeaderboardEntry
import com.example.betterme.domain.leaderboard.LeaderboardProfile
import com.example.betterme.domain.leaderboard.MonthlyWinner
import com.example.betterme.domain.leaderboard.RivalInsight

/**
 * Domain-side contract for the cross-challenge GLOBAL leaderboard
 * surface introduced in Phase 2B. Lives next to the per-challenge
 * [ChallengeLeaderboardRepository] — the two stay separate because the
 * data shapes, Firestore paths, and seeding strategies are different,
 * and merging them would force unhelpful generality on the impl.
 *
 * Four read paths, one write path:
 *
 *  - [getGlobal]         — top-N entries on the current global ladder.
 *  - [getFriends]        — close-score rivals (Phase 2B uses
 *                          deterministic seeded subset; future versions
 *                          will swap in a real friend graph).
 *  - [getMonthlyWinners] — top 3 from finished seasons. Derived live
 *                          from the same Firestore collection — no
 *                          separate archive doc.
 *  - [getProfile]        — single-user profile view for the bottom
 *                          sheet shown on row tap.
 *  - [upsertMyEntry]     — write the current user's global row,
 *                          throttled by the same session-memory layer
 *                          the per-challenge repo uses (5min read TTL,
 *                          30s per-user write throttle).
 */
interface GlobalLeaderboardRepository {

    suspend fun getGlobal(
        seasonKey: String,
        limit: Int = 100,
        forceRefresh: Boolean = false
    ): GlobalSnapshot

    suspend fun getFriends(
        seasonKey: String,
        forceRefresh: Boolean = false
    ): FriendSnapshot

    suspend fun getMonthlyWinners(
        recentSeasonCount: Int = 6
    ): List<MonthlyWinner>

    suspend fun getProfile(
        userId: String,
        seasonKey: String
    ): LeaderboardProfile?

    suspend fun upsertMyEntry(
        seasonKey: String,
        displayName: String,
        avatarUrl: String?,
        totalCompletedHabits: Int,
        longestStreak: Int,
        earnedCoins: Int,
        completedChallenges: Int,
        monthlyConsistencyBonus: Int,
        force: Boolean = false
    )

    fun invalidateCache()

    /**
     * Snapshot for the GLOBAL tab. Entries are ranked + enriched
     * (rankDelta / badges / leagueProgress) by the impl before return.
     */
    data class GlobalSnapshot(
        val entries: List<GlobalLeaderboardEntry>,
        val myEntry: GlobalLeaderboardEntry?,
        val totalParticipants: Int,
        val isStale: Boolean = false,
        val seasonEndsInDays: Int
    )

    /**
     * Snapshot for the FRIENDS tab. The same entries are ranked
     * relative to each other (so the current user sees their rank
     * within the friend pool, not their global rank). [insights] are
     * the narrative cards rendered above the list.
     */
    data class FriendSnapshot(
        val entries: List<GlobalLeaderboardEntry>,
        val relationships: Map<String, FriendRelationship>,
        val insights: List<RivalInsight>,
        val isStale: Boolean = false
    )
}
