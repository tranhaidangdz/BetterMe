package com.example.betterme.data.leaderboard

import com.example.betterme.data.local.datastore.DataStoreManager
import com.example.betterme.domain.leaderboard.ChallengeMeta
import com.example.betterme.domain.leaderboard.LeaderboardEntry
import com.example.betterme.domain.leaderboard.LeaderboardIntegrityValidator
import com.example.betterme.domain.leaderboard.RankBadge
import com.example.betterme.domain.leaderboard.RankDelta
import com.example.betterme.domain.leaderboard.ScoreFormula
import com.example.betterme.domain.repository.ChallengeLeaderboardRepository
import com.example.betterme.domain.repository.ChallengeLeaderboardRepository.LeaderboardSnapshot
import com.example.betterme.domain.repository.ChallengeRepository
import com.example.betterme.domain.repository.UserChallengeRepository
import kotlinx.coroutines.flow.first

/**
 * Repository orchestrating Firestore I/O + hybrid seeding + session
 * cache + Phase 2 enhancements (rank deltas, badges, integrity
 * validation, motivational events) for the monthly challenge
 * leaderboard.
 *
 * ### Merge model (unchanged from Phase 1)
 *  1. [seeder] generates the deterministic seeded competitor pool.
 *  2. Firestore is queried for real entries.
 *  3. The two lists are merged + re-ranked by descending totalScore.
 *
 * ### Phase 2 enrichment layered on top of the merge
 *  - Each entry runs through [LeaderboardIntegrityValidator] before
 *    being shown. Suspicious rows are clamped + flagged but not hidden.
 *  - Rank deltas are computed against the persisted snapshot from
 *    [RankSnapshotStore].
 *  - Badges are awarded deterministically by [RankBadge.award] using
 *    the entry's final rank + streak + delta.
 *  - Motivational events are produced from the user's delta and
 *    surface as one event per (kind, day) via
 *    [MotivationalEventEngine] (cooldown-gated).
 *  - The fresh snapshot is persisted at the end so the *next* read can
 *    compute deltas against this one.
 */
class ChallengeLeaderboardRepositoryImpl(
    private val firestoreDs: FirebaseChallengeLeaderboardDataSource,
    private val seeder: HybridCompetitorSeeder,
    private val sessionMemory: LeaderboardSessionMemory,
    private val dataStoreManager: DataStoreManager,
    private val challengeRepository: ChallengeRepository,
    private val userChallengeRepository: UserChallengeRepository,
    private val rankSnapshotStore: RankSnapshotStore,
    private val motivationalEventEngine: MotivationalEventEngine
) : ChallengeLeaderboardRepository {

    override suspend fun getLeaderboard(
        challengeId: Int,
        seasonKey: String,
        limit: Int,
        forceRefresh: Boolean
    ): LeaderboardSnapshot {
        if (!forceRefresh) {
            // Phase 2 — motivational events are one-shot. The cached
            // copy gets them stripped on retrieval so a navigation
            // bounce doesn't re-surface the same toast.
            sessionMemory.get(challengeId, seasonKey)?.let { cached ->
                return cached.copy(motivationalEvents = emptyList())
            }
        }
        return fetchAndMerge(challengeId, seasonKey, limit)
    }

    override suspend fun getSummary(
        challengeId: Int,
        seasonKey: String,
        forceRefresh: Boolean
    ): LeaderboardSnapshot = getLeaderboard(challengeId, seasonKey, limit = 50, forceRefresh)

    private suspend fun fetchAndMerge(
        challengeId: Int,
        seasonKey: String,
        limit: Int
    ): LeaderboardSnapshot {
        val currentUserId = dataStoreManager.getCurrentUserId().first().orEmpty()
        val challenge = challengeRepository.getById(challengeId)
        val targetStreak = challenge?.target_streak ?: 14

        val realTop = firestoreDs.readTopEntries(challengeId, seasonKey, limit.toLong())
        val realMine = if (currentUserId.isBlank()) null
        else firestoreDs.readEntry(challengeId, seasonKey, currentUserId)
        val meta = firestoreDs.readMeta(challengeId, seasonKey)
            ?: ChallengeMeta(challengeId, seasonKey, 0, 0, 0L)

        val seededPool = seeder.seedFor(
            challengeId = challengeId,
            seasonKey = seasonKey,
            targetStreak = targetStreak,
            participantHint = meta.participantCount,
            // Phase 3 — pass difficulty so the seeder lifts the top
            // score band on HARD / LEGENDARY ladders, making them feel
            // proportionally tougher to climb.
            difficulty = challenge?.difficulty
        )

        // Merge — real rows take precedence when userIds collide.
        val realByUid = realTop.associateBy { it.userId }
        val rankedBase = buildList {
            addAll(realTop)
            realMine?.takeIf { it.userId !in realByUid }?.let { add(it) }
            seededPool.filter { it.userId !in realByUid }.forEach { add(it) }
        }
            .sortedWith(
                compareByDescending<LeaderboardEntry> { it.totalScore }
                    .thenBy { it.updatedAt }
            )
            .mapIndexed { index, e ->
                e.copy(
                    rank = index + 1,
                    isCurrentUser = currentUserId.isNotBlank() && e.userId == currentUserId
                )
            }

        // Load the previously persisted ranks BEFORE we enrich — the
        // delta has to reflect the snapshot the user last saw, not a
        // value we just wrote.
        val previousRanks = rankSnapshotStore.load(challengeId, seasonKey)
        val challengeStartMs = if (currentUserId.isNotBlank()) {
            userChallengeRepository
                .getByUserAndChallenge(currentUserId, challengeId)
                ?.start_date
        } else null

        // Phase 2 — enrich each row with integrity check, delta, badges.
        val participantsForBadge = (meta.participantCount + seededPool.size).coerceAtLeast(rankedBase.size)
        val enriched = rankedBase.map { entry ->
            val validated = LeaderboardIntegrityValidator.validate(
                entry = entry,
                seasonKey = seasonKey,
                currentUserChallengeStartMs = if (entry.isCurrentUser) challengeStartMs else null
            )
            val delta = if (validated.isSeededRival && previousRanks.isEmpty()) {
                // First-ever read — seeded rivals show no delta; we'd be
                // claiming they all just appeared, which is noise.
                RankDelta.Hidden
            } else {
                RankDelta.compute(validated.rank, previousRanks[validated.userId])
            }
            val badges = RankBadge.award(
                rank = validated.rank,
                currentStreak = validated.currentStreak,
                participantCount = participantsForBadge,
                delta = delta
            )
            validated.copy(rankDelta = delta, badges = badges)
        }

        val myEntry = enriched.firstOrNull { it.isCurrentUser }

        val mergedMeta = meta.copy(
            participantCount = participantsForBadge,
            topScore = enriched.firstOrNull()?.totalScore ?: meta.topScore
        )

        // Produce motivational events BEFORE we write the new snapshot —
        // engine needs the previous ranks to detect crossings.
        val events = if (currentUserId.isNotBlank()) {
            motivationalEventEngine.produce(
                entries = enriched,
                previousRanks = previousRanks,
                currentUserId = currentUserId,
                challengeId = challengeId,
                seasonKey = seasonKey
            )
        } else emptyList()

        val snapshot = LeaderboardSnapshot(
            entries = enriched.take(limit),
            meta = mergedMeta,
            myEntry = myEntry,
            isStale = false,
            motivationalEvents = events
        )
        sessionMemory.put(challengeId, seasonKey, snapshot)

        // Persist the fresh snapshot AFTER everything else so a crash
        // mid-fetch doesn't poison future delta computations with a
        // partial map. Best-effort — failures swallowed inside the store.
        rankSnapshotStore.save(
            challengeId = challengeId,
            seasonKey = seasonKey,
            ranks = enriched.associate { it.userId to it.rank }
        )
        return snapshot
    }

    override suspend fun upsertMyEntry(
        challengeId: Int,
        seasonKey: String,
        displayName: String,
        avatarUrl: String?,
        completedTasks: Int,
        currentStreak: Int,
        earnedCoins: Int,
        force: Boolean
    ) {
        val userId = dataStoreManager.getCurrentUserId().first().orEmpty()
        if (userId.isBlank()) return
        if (sessionMemory.shouldThrottleWrite(userId, challengeId, force)) return

        val score = ScoreFormula.compute(completedTasks, currentStreak, earnedCoins)
        val entry = LeaderboardEntry(
            userId = userId,
            displayName = displayName,
            avatarUrl = avatarUrl,
            completedTasks = completedTasks,
            currentStreak = currentStreak,
            earnedCoins = earnedCoins,
            totalScore = score,
            updatedAt = System.currentTimeMillis()
        )
        val ok = firestoreDs.upsertEntry(challengeId, seasonKey, userId, entry)
        if (ok) {
            sessionMemory.markWritten(userId, challengeId)
            sessionMemory.invalidate()
        }
    }

    override suspend fun getMeta(challengeId: Int, seasonKey: String): ChallengeMeta? =
        firestoreDs.readMeta(challengeId, seasonKey)

    override fun invalidateCache() {
        sessionMemory.invalidate()
    }
}
