package com.example.betterme.data.leaderboard

import com.example.betterme.data.local.datastore.DataStoreManager
import com.example.betterme.domain.leaderboard.ChallengeMeta
import com.example.betterme.domain.leaderboard.LeaderboardEntry
import com.example.betterme.domain.leaderboard.ScoreFormula
import com.example.betterme.domain.repository.ChallengeLeaderboardRepository
import com.example.betterme.domain.repository.ChallengeLeaderboardRepository.LeaderboardSnapshot
import com.example.betterme.domain.repository.ChallengeRepository
import kotlinx.coroutines.flow.first

/**
 * Repository orchestrating Firestore I/O + hybrid seeding + session
 * cache for the monthly challenge leaderboard.
 *
 * ### Merge model
 *  1. [seeder] generates the deterministic seeded competitor pool for
 *     (challengeId, seasonKey). These rows have `isSeededRival = true`.
 *  2. Firestore is queried for the top entries.
 *  3. The two lists are concatenated and re-ranked by `totalScore`
 *     descending. The current user's entry is then sticky-marked with
 *     `isCurrentUser = true` regardless of which source it came from.
 *  4. Real Firestore rows take precedence over seeded rivals on userId
 *     collisions (paranoia — userIds are namespaced apart but defense
 *     in depth costs nothing).
 *
 * ### Why we always include the seeder
 * Even in production with thousands of users, the seeded pool fills the
 * tail when a specific challenge has only a handful of real
 * participants. The competitive feel is most fragile early — for niche
 * challenges. Keeping the seeder in the merge keeps the leaderboard
 * feeling alive everywhere.
 */
class ChallengeLeaderboardRepositoryImpl(
    private val firestoreDs: FirebaseChallengeLeaderboardDataSource,
    private val seeder: HybridCompetitorSeeder,
    private val sessionMemory: LeaderboardSessionMemory,
    private val dataStoreManager: DataStoreManager,
    private val challengeRepository: ChallengeRepository
) : ChallengeLeaderboardRepository {

    override suspend fun getLeaderboard(
        challengeId: Int,
        seasonKey: String,
        limit: Int,
        forceRefresh: Boolean
    ): LeaderboardSnapshot {
        if (!forceRefresh) {
            sessionMemory.get(challengeId, seasonKey)?.let { return it }
        }
        return fetchAndMerge(challengeId, seasonKey, limit)
    }

    override suspend fun getSummary(
        challengeId: Int,
        seasonKey: String,
        forceRefresh: Boolean
    ): LeaderboardSnapshot {
        // Reuse the full leaderboard path so the summary and the screen
        // share one cache slot. The summary card on Overview and the
        // full screen will share the same cached snapshot.
        return getLeaderboard(challengeId, seasonKey, limit = 50, forceRefresh)
    }

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
            participantHint = meta.participantCount
        )

        // Merge — real rows take precedence when userIds collide.
        val realByUid = realTop.associateBy { it.userId }
        val merged = buildList {
            addAll(realTop)
            // Plus the current user if they're not already in the top.
            realMine?.takeIf { it.userId !in realByUid }?.let { add(it) }
            // Plus seeded rivals not colliding with real rows.
            seededPool.filter { it.userId !in realByUid }.forEach { add(it) }
        }
            .sortedWith(
                compareByDescending<LeaderboardEntry> { it.totalScore }
                    .thenBy { it.updatedAt } // earlier == better tie-break
            )
            .mapIndexed { index, e ->
                e.copy(
                    rank = index + 1,
                    isCurrentUser = currentUserId.isNotBlank() && e.userId == currentUserId
                )
            }

        val myEntry = merged.firstOrNull { it.isCurrentUser }

        val mergedMeta = meta.copy(
            // Inflate participantCount with the seeded pool so the UI's
            // "2,431 participants" headline isn't dominated by real-user
            // count alone in the early days.
            participantCount = (meta.participantCount + seededPool.size),
            topScore = merged.firstOrNull()?.totalScore ?: meta.topScore
        )

        val snapshot = LeaderboardSnapshot(
            entries = merged.take(limit),
            meta = mergedMeta,
            myEntry = myEntry,
            isStale = false
        )
        sessionMemory.put(challengeId, seasonKey, snapshot)
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
            // Bust the read cache — next view will hydrate including
            // the user's new score.
            sessionMemory.invalidate()
        }
    }

    override suspend fun getMeta(challengeId: Int, seasonKey: String): ChallengeMeta? =
        firestoreDs.readMeta(challengeId, seasonKey)

    override fun invalidateCache() {
        sessionMemory.invalidate()
    }
}
