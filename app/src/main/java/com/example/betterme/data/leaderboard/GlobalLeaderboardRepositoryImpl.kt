package com.example.betterme.data.leaderboard

import com.example.betterme.data.local.datastore.DataStoreManager
import com.example.betterme.domain.leaderboard.FriendRelationship
import com.example.betterme.domain.leaderboard.GlobalLeaderboardEntry
import com.example.betterme.domain.leaderboard.GlobalScoreFormula
import com.example.betterme.domain.leaderboard.LeagueProgress
import com.example.betterme.domain.leaderboard.LeaderboardProfile
import com.example.betterme.domain.leaderboard.MonthlyWinner
import com.example.betterme.domain.leaderboard.RankBadge
import com.example.betterme.domain.leaderboard.RankDelta
import com.example.betterme.domain.leaderboard.RivalInsight
import com.example.betterme.domain.leaderboard.Season
import com.example.betterme.domain.repository.GlobalLeaderboardRepository
import com.example.betterme.domain.repository.GlobalLeaderboardRepository.FriendSnapshot
import com.example.betterme.domain.repository.GlobalLeaderboardRepository.GlobalSnapshot
import kotlinx.coroutines.flow.first
import java.util.Calendar
import java.util.concurrent.ConcurrentHashMap

/**
 * Phase 2B orchestrator for the cross-challenge GLOBAL leaderboard +
 * friends + monthly winners + profile reads.
 *
 * ### Read model
 *
 * **Global tab** — Firestore top-N merged with the deterministic
 * seeded pool. Real rows take precedence on userId collisions.
 * Sorted by `totalScore DESC, updatedAt ASC` (tie-break: earlier
 * updates ranked higher → rewards consistency). Each entry is
 * enriched with rankDelta (from [RankSnapshotStore], reusing the
 * Phase 2A persistence layer with a global namespace prefix),
 * RankBadge.award(), and LeagueProgress.fromScore().
 *
 * **Friends tab** — picks a deterministic subset of 10 rivals from the
 * merged global pool closest in score to the current user. Re-ranks
 * within the friend pool (so the user sees their rank relative to
 * friends, not their global rank). Computes RivalInsights against the
 * previous friend snapshot.
 *
 * **Monthly winners** — reads top 3 from each of the last [recentSeasonCount]
 * past seasons. Excludes the current season (still in progress).
 * Past-season rivals are deterministically reproduced by the seeder
 * with the same `seasonKey`, so even seasons before any real user
 * existed still show a credible top 3.
 *
 * **Profile** — for the current user, pulls from Room + Firestore
 * directly. For seeded rivals, the deterministic seed gives every
 * stat consistently, so the profile sheet feels real.
 *
 * ### Caching
 * Reuses [LeaderboardSessionMemory]'s discipline at a higher level:
 * a small in-memory map keyed by `(seasonKey, kind)` with 5-min TTL.
 * Cache busted on write.
 *
 * ### Write throttle
 * Reuses the existing per-user [LeaderboardSessionMemory] but under a
 * different challengeId sentinel (-1) so the global write throttle
 * doesn't collide with per-challenge writes.
 */
class GlobalLeaderboardRepositoryImpl(
    private val firestoreDs: FirebaseGlobalLeaderboardDataSource,
    private val seeder: GlobalCompetitorSeeder,
    private val sessionMemory: LeaderboardSessionMemory,
    private val dataStoreManager: DataStoreManager,
    private val rankSnapshotStore: RankSnapshotStore
) : GlobalLeaderboardRepository {

    /** 5-min in-memory cache for the per-tab snapshots. */
    private val cache = ConcurrentHashMap<String, CachedSnapshot<Any>>()

    override suspend fun getGlobal(
        seasonKey: String,
        limit: Int,
        forceRefresh: Boolean
    ): GlobalSnapshot {
        val key = "$seasonKey|GLOBAL"
        if (!forceRefresh) {
            (cache[key]?.payload as? GlobalSnapshot)?.let { hit ->
                if (System.currentTimeMillis() - cache[key]!!.fetchedAtMs <= READ_TTL_MS) return hit
            }
        }
        val snapshot = fetchGlobal(seasonKey, limit)
        cache[key] = CachedSnapshot(snapshot, System.currentTimeMillis())
        return snapshot
    }

    private suspend fun fetchGlobal(seasonKey: String, limit: Int): GlobalSnapshot {
        val currentUserId = dataStoreManager.getCurrentUserId().first().orEmpty()

        val realTop = firestoreDs.readTopEntries(seasonKey, limit.toLong())
        val realMine = if (currentUserId.isBlank()) null
        else firestoreDs.readEntry(seasonKey, currentUserId)
        val seededPool = seeder.seedFor(seasonKey)

        val realByUid = realTop.associateBy { it.userId }
        val merged = buildList {
            addAll(realTop)
            realMine?.takeIf { it.userId !in realByUid }?.let { add(it) }
            seededPool.filter { it.userId !in realByUid }.forEach { add(it) }
        }
            .sortedWith(
                compareByDescending<GlobalLeaderboardEntry> { it.totalScore }
                    .thenBy { it.updatedAt }
            )
            .mapIndexed { index, e ->
                e.copy(
                    rank = index + 1,
                    isCurrentUser = currentUserId.isNotBlank() && e.userId == currentUserId
                )
            }

        // Reuse the Phase 2A rank snapshot store under a global
        // namespace (challengeId = -1) so deltas work the same way.
        val previousRanks = rankSnapshotStore.load(GLOBAL_NAMESPACE_ID, seasonKey)
        val totalParticipants = merged.size
        val enriched = merged.map { entry ->
            val delta = if (entry.isSeededRival && previousRanks.isEmpty()) RankDelta.Hidden
            else RankDelta.compute(entry.rank, previousRanks[entry.userId])
            val badges = RankBadge.award(
                rank = entry.rank,
                currentStreak = entry.longestStreak,
                participantCount = totalParticipants,
                delta = delta
            )
            entry.copy(
                rankDelta = delta,
                badges = badges,
                leagueProgress = LeagueProgress.fromScore(entry.totalScore)
            )
        }

        // Persist the fresh snapshot so the NEXT global read can
        // compute deltas. Best-effort.
        rankSnapshotStore.save(
            challengeId = GLOBAL_NAMESPACE_ID,
            seasonKey = seasonKey,
            ranks = enriched.associate { it.userId to it.rank }
        )

        val myEntry = enriched.firstOrNull { it.isCurrentUser }
        val seasonEndsInDays = daysUntilEndOfSeason(seasonKey)
        return GlobalSnapshot(
            entries = enriched.take(limit),
            myEntry = myEntry,
            totalParticipants = totalParticipants,
            isStale = false,
            seasonEndsInDays = seasonEndsInDays
        )
    }

    override suspend fun getFriends(
        seasonKey: String,
        forceRefresh: Boolean
    ): FriendSnapshot {
        val key = "$seasonKey|FRIENDS"
        if (!forceRefresh) {
            (cache[key]?.payload as? FriendSnapshot)?.let { hit ->
                if (System.currentTimeMillis() - cache[key]!!.fetchedAtMs <= READ_TTL_MS) return hit
            }
        }
        // Source from the same merged global pool. Pulling via getGlobal
        // means cache hits cascade — viewing Global then Friends is two
        // cache lookups, not two Firestore queries.
        val global = getGlobal(seasonKey, limit = 100, forceRefresh = forceRefresh)
        val mine = global.myEntry
        val friends = if (mine == null) {
            // Anonymous / brand-new user — give them a slice from the
            // top + middle of the pool so the Friends tab still has
            // content. They join the leaderboard at the bottom for now.
            (global.entries.take(3) + global.entries.drop(15).take(7)).distinctBy { it.userId }
        } else {
            pickFriends(global.entries, mine)
        }

        // Re-rank within the friend pool — friends see their position
        // RELATIVE to each other, which is the whole point of the tab.
        val rankedFriends = friends
            .sortedWith(
                compareByDescending<GlobalLeaderboardEntry> { it.totalScore }
                    .thenBy { it.updatedAt }
            )
            .mapIndexed { idx, e -> e.copy(rank = idx + 1) }

        val relationships = rankedFriends.associate { entry ->
            entry.userId to if (entry.isCurrentUser) FriendRelationship.SELF
            else FriendRelationship.RIVAL
        }

        // Compute rival insights against the previous friend snapshot.
        val previousFriendRanks = rankSnapshotStore.load(FRIEND_NAMESPACE_ID, seasonKey)
        val insights = if (mine != null) buildRivalInsights(rankedFriends, mine, previousFriendRanks)
        else emptyList()
        rankSnapshotStore.save(
            challengeId = FRIEND_NAMESPACE_ID,
            seasonKey = seasonKey,
            ranks = rankedFriends.associate { it.userId to it.rank }
        )

        val snapshot = FriendSnapshot(
            entries = rankedFriends,
            relationships = relationships,
            insights = insights,
            isStale = false
        )
        cache[key] = CachedSnapshot(snapshot, System.currentTimeMillis())
        return snapshot
    }

    /**
     * Pick 8-10 deterministic "friends" centered on the current user's
     * score band — closest rivals make for better drama. Plus the
     * user themselves so they always anchor the list. Plus a couple
     * of top-of-leaderboard entries so the user has aspirational
     * comparisons even when they're far from the top.
     */
    private fun pickFriends(
        all: List<GlobalLeaderboardEntry>,
        me: GlobalLeaderboardEntry
    ): List<GlobalLeaderboardEntry> {
        val byScore = all.sortedBy { kotlin.math.abs(it.totalScore - me.totalScore) }
        val close = byScore.filter { it.userId != me.userId }.take(6)
        val aspirational = all.filter { it.rank <= 3 && it.userId != me.userId }.take(2)
        return (listOf(me) + aspirational + close).distinctBy { it.userId }
    }

    private fun buildRivalInsights(
        rankedFriends: List<GlobalLeaderboardEntry>,
        me: GlobalLeaderboardEntry,
        previousRanks: Map<String, Int>
    ): List<RivalInsight> {
        val out = mutableListOf<RivalInsight>()
        val myCurrentRank = rankedFriends.firstOrNull { it.userId == me.userId }?.rank ?: return emptyList()
        val myPreviousRank = previousRanks[me.userId]

        // 1) Within reach — first friend ranked one above the user.
        rankedFriends.firstOrNull { it.rank == myCurrentRank - 1 }?.let { above ->
            val gap = above.totalScore - me.totalScore
            if (gap in 1..40) {
                out += RivalInsight(
                    kind = RivalInsight.Kind.WITHIN_REACH,
                    rivalDisplayName = above.displayName,
                    rivalAvatarUrl = above.avatarUrl,
                    pointsGap = gap,
                    message = "Chỉ còn $gap điểm là vượt ${above.displayName}!"
                )
            }
        }

        // 2) Passed-you / defeated — friends whose rank crossed yours.
        rankedFriends.forEach { friend ->
            if (friend.userId == me.userId) return@forEach
            val prev = previousRanks[friend.userId] ?: return@forEach
            val myPrev = myPreviousRank ?: return@forEach
            val gap = kotlin.math.abs(friend.totalScore - me.totalScore)
            when {
                // Rival was below or tied last time, now above.
                prev >= myPrev && friend.rank < myCurrentRank -> {
                    if (out.none { it.kind == RivalInsight.Kind.PASSED_YOU }) {
                        out += RivalInsight(
                            kind = RivalInsight.Kind.PASSED_YOU,
                            rivalDisplayName = friend.displayName,
                            rivalAvatarUrl = friend.avatarUrl,
                            pointsGap = gap,
                            message = "${friend.displayName} đã vượt bạn $gap điểm. Lội ngược dòng nào!"
                        )
                    }
                }
                // Rival was above last time, now below.
                prev <= myPrev && friend.rank > myCurrentRank -> {
                    if (out.none { it.kind == RivalInsight.Kind.DEFEATED }) {
                        out += RivalInsight(
                            kind = RivalInsight.Kind.DEFEATED,
                            rivalDisplayName = friend.displayName,
                            rivalAvatarUrl = friend.avatarUrl,
                            pointsGap = gap,
                            message = "Bạn vừa vượt ${friend.displayName} tuần này 💪"
                        )
                    }
                }
            }
        }
        return out
    }

    override suspend fun getMonthlyWinners(recentSeasonCount: Int): List<MonthlyWinner> {
        // Skip the current season (still in progress) and walk back.
        val seasons = Season.recent(recentSeasonCount + 1).drop(1)
        val out = mutableListOf<MonthlyWinner>()
        for (seasonKey in seasons) {
            // Try Firestore first — past seasons may have real winners
            // we want to honor. Fall back to the deterministic seeded
            // pool so even pre-launch months show a credible top 3.
            val real = firestoreDs.readTopForSeason(seasonKey, limit = 3)
            val seeded = seeder.seedFor(seasonKey)
            val merged = (real + seeded.filter { it.userId !in real.map { r -> r.userId } })
                .sortedByDescending { it.totalScore }
                .take(3)
            merged.forEachIndexed { idx, entry ->
                out += MonthlyWinner(
                    seasonKey = seasonKey,
                    userId = entry.userId,
                    displayName = entry.displayName,
                    avatarUrl = entry.avatarUrl,
                    rank = idx + 1,
                    totalScore = entry.totalScore,
                    leagueTier = LeagueProgress.fromScore(entry.totalScore).tier,
                    badges = RankBadge.award(
                        rank = idx + 1,
                        currentStreak = entry.longestStreak,
                        participantCount = merged.size + seeded.size,
                        delta = RankDelta.Hidden
                    ),
                    displayLabel = Season.displayLabel(seasonKey)
                )
            }
        }
        return out
    }

    override suspend fun getProfile(userId: String, seasonKey: String): LeaderboardProfile? {
        // Try the live global snapshot first — guarantees the rank +
        // score the user just saw on the leaderboard matches the sheet.
        val snapshot = getGlobal(seasonKey)
        val entry = snapshot.entries.firstOrNull { it.userId == userId }
            ?: snapshot.myEntry?.takeIf { it.userId == userId }
            ?: return null

        // For the current user, the Room layer has a real "favorite
        // challenge" (their highest check-in count). For seeded rivals,
        // we synthesize one deterministically using the user id.
        val favorite = if (entry.isCurrentUser) null // hooked up in a future commit when Room <-> profile bridge lands
        else FAKE_FAVORITES[(userId.hashCode().let { if (it < 0) -it else it }) % FAKE_FAVORITES.size]

        return LeaderboardProfile(
            userId = entry.userId,
            displayName = entry.displayName,
            avatarUrl = entry.avatarUrl,
            favoriteChallengeTitle = favorite,
            totalCompletedHabits = entry.totalCompletedHabits,
            longestStreak = entry.longestStreak,
            completedChallenges = entry.completedChallenges,
            currentRank = entry.rank,
            totalScore = entry.totalScore,
            badges = entry.badges,
            leagueProgress = entry.leagueProgress
        )
    }

    override suspend fun upsertMyEntry(
        seasonKey: String,
        displayName: String,
        avatarUrl: String?,
        totalCompletedHabits: Int,
        longestStreak: Int,
        earnedCoins: Int,
        completedChallenges: Int,
        monthlyConsistencyBonus: Int,
        force: Boolean
    ) {
        val userId = dataStoreManager.getCurrentUserId().first().orEmpty()
        if (userId.isBlank()) return
        // Reuse the per-user throttle from Phase 1 — different
        // challengeId sentinel so global doesn't compete with
        // per-challenge writes.
        if (sessionMemory.shouldThrottleWrite(userId, GLOBAL_NAMESPACE_ID, force)) return

        val score = GlobalScoreFormula.compute(
            totalCompletedHabits = totalCompletedHabits,
            longestStreak = longestStreak,
            earnedCoins = earnedCoins,
            completedChallenges = completedChallenges,
            monthlyConsistencyBonus = monthlyConsistencyBonus
        )
        val entry = GlobalLeaderboardEntry(
            userId = userId,
            displayName = displayName,
            avatarUrl = avatarUrl,
            totalCompletedHabits = totalCompletedHabits,
            longestStreak = longestStreak,
            earnedCoins = earnedCoins,
            completedChallenges = completedChallenges,
            monthlyConsistencyBonus = monthlyConsistencyBonus,
            totalScore = score,
            updatedAt = System.currentTimeMillis()
        )
        val ok = firestoreDs.upsertEntry(seasonKey, userId, entry)
        if (ok) {
            sessionMemory.markWritten(userId, GLOBAL_NAMESPACE_ID)
            invalidateCache()
        }
    }

    override fun invalidateCache() {
        cache.clear()
    }

    /** Days until midnight of the 1st of next month, in local time. */
    private fun daysUntilEndOfSeason(seasonKey: String): Int {
        val parts = seasonKey.split("-")
        if (parts.size != 2) return 0
        val year = parts[0].toIntOrNull() ?: return 0
        val month = parts[1].toIntOrNull() ?: return 0
        val cal = Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month - 1)
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            add(Calendar.MONTH, 1) // first of next month
        }
        val nextMonthStart = cal.timeInMillis
        val now = System.currentTimeMillis()
        if (now >= nextMonthStart) return 0
        return ((nextMonthStart - now) / (24L * 60 * 60 * 1000)).toInt().coerceAtLeast(0)
    }

    private data class CachedSnapshot<T>(val payload: T, val fetchedAtMs: Long)

    private companion object {
        const val READ_TTL_MS: Long = 5L * 60 * 1000

        /** Sentinel "challengeId" for the snapshot store + write throttle. */
        const val GLOBAL_NAMESPACE_ID = -1

        /** Sentinel for friend-tab deltas. */
        const val FRIEND_NAMESPACE_ID = -2

        /** Pool of fake favorite challenge titles for seeded-rival profiles. */
        val FAKE_FAVORITES: List<String> = listOf(
            "Đi bộ 10.000 bước",
            "Uống 2 lít nước mỗi ngày",
            "Ngủ trước 23:00",
            "Đọc 10 trang sách",
            "Tập thở 5 phút",
            "Học 1 từ vựng mới",
            "Thiền 10 phút",
            "Viết nhật ký biết ơn",
            "Tập cardio 20 phút"
        )
    }
}
