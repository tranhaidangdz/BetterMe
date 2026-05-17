package com.example.betterme.data.leaderboard

import com.example.betterme.domain.leaderboard.LeaderboardEntry
import com.example.betterme.domain.leaderboard.MotivationalEvent
import java.util.concurrent.ConcurrentHashMap

/**
 * Process-scoped deterministic engine that derives
 * [MotivationalEvent]s from snapshot deltas.
 *
 * Pure function shape:
 *
 *   produce(current, previousRanks, currentUserId) → List<MotivationalEvent>
 *
 * Plus a tiny stateful 24h cooldown map keyed by `(eventKind,
 * challengeId, seasonKey, currentUserId)` so the same triggering
 * condition doesn't fire on every leaderboard refresh inside a day.
 *
 * Why state lives here (and not in the repository): the cooldown is
 * cross-screen UX concern, not a Firestore concern. Keeping it in this
 * thin object means the screen surface can stay dumb and the repository
 * doesn't need a side-cache for cooldown bookkeeping.
 *
 * The engine is intentionally conservative — at most ONE event of each
 * kind per (challenge, season) per 24h. The repository / VM is free to
 * pick the highest-priority event from the returned list and ignore
 * the rest.
 */
class MotivationalEventEngine {

    private val lastFiredAtMs = ConcurrentHashMap<String, Long>()

    /**
     * Produce all events whose triggering conditions fire on this
     * snapshot transition. Results are sorted by descending priority.
     *
     * @param entries current ranked entries (rank 1 first).
     * @param previousRanks the persisted map from
     *   [RankSnapshotStore]. May be empty on first-ever read.
     * @param currentUserId the logged-in user. When blank / not in
     *   [entries], no user-specific events fire and we return empty.
     * @param challengeId / seasonKey scope the cooldown buckets.
     * @param now wall-clock for cooldown bookkeeping.
     */
    fun produce(
        entries: List<LeaderboardEntry>,
        previousRanks: Map<String, Int>,
        currentUserId: String,
        challengeId: Int,
        seasonKey: String,
        now: Long = System.currentTimeMillis()
    ): List<MotivationalEvent> {
        if (currentUserId.isBlank()) return emptyList()
        val mine = entries.firstOrNull { it.userId == currentUserId } ?: return emptyList()
        val out = mutableListOf<MotivationalEvent>()

        // 1) EnteredTopRank — fires when user crosses Top 3 / Top 10 / Top 50
        //    threshold downward (better rank) since the previous snapshot.
        val prevRank = previousRanks[currentUserId]
        val curRank = mine.rank
        if (prevRank != null && curRank < prevRank) {
            val crossed = TIERS.firstOrNull { tier ->
                prevRank > tier && curRank <= tier
            }
            if (crossed != null && canFire(MotivationalEvent.Kind.ENTERED_TOP_RANK, challengeId, seasonKey, currentUserId, now)) {
                out += MotivationalEvent.EnteredTopRank(
                    tier = crossed,
                    message = when (crossed) {
                        3 -> "🥉 Bạn vừa lọt Top 3!"
                        10 -> "🚀 Bạn vừa vào Top 10!"
                        else -> "📈 Bạn vừa vào Top $crossed!"
                    }
                )
                markFired(MotivationalEvent.Kind.ENTERED_TOP_RANK, challengeId, seasonKey, currentUserId, now)
            }
        }

        // 2) PassedByRival — the entry that was JUST ABOVE the user
        //    last time is NOW above the user by a wider margin (they
        //    pulled ahead). We approximate "just above" as previousRank
        //    = curUserPreviousRank - 1.
        if (prevRank != null) {
            val rivalPrevRank = prevRank - 1
            val rival = entries.firstOrNull { entry ->
                previousRanks[entry.userId] == rivalPrevRank &&
                    entry.userId != currentUserId &&
                    entry.rank < curRank
            }
            if (rival != null) {
                val gap = rival.totalScore - mine.totalScore
                if (gap in 1..200 && canFire(MotivationalEvent.Kind.PASSED_BY_RIVAL, challengeId, seasonKey, currentUserId, now)) {
                    out += MotivationalEvent.PassedByRival(
                        rivalName = rival.displayName,
                        pointsAhead = gap,
                        message = "${rival.displayName} đang vượt bạn $gap điểm — đuổi theo nào!"
                    )
                    markFired(MotivationalEvent.Kind.PASSED_BY_RIVAL, challengeId, seasonKey, currentUserId, now)
                }
            }
        }

        // 3) CloseToNextRank — fires when the user is within 20 points
        //    of the entry immediately above them. Doesn't require a
        //    delta — it's an evergreen motivator.
        val above = entries.firstOrNull { it.rank == curRank - 1 }
        if (above != null) {
            val gap = above.totalScore - mine.totalScore
            if (gap in 1..20 && canFire(MotivationalEvent.Kind.CLOSE_TO_NEXT_RANK, challengeId, seasonKey, currentUserId, now)) {
                out += MotivationalEvent.CloseToNextRank(
                    pointsBehind = gap,
                    targetRank = above.rank,
                    message = "Chỉ còn $gap điểm là lên #${above.rank}!"
                )
                markFired(MotivationalEvent.Kind.CLOSE_TO_NEXT_RANK, challengeId, seasonKey, currentUserId, now)
            }
        }

        // 4) StreakMilestone — fires the first time per season that
        //    the streak hits a notable threshold. We use cooldown alone
        //    to enforce "first time" since the streak value changes
        //    only on check-in.
        val s = mine.currentStreak
        val tier = STREAK_TIERS.firstOrNull { it == s }
        if (tier != null && canFire(MotivationalEvent.Kind.STREAK_MILESTONE, challengeId, seasonKey, currentUserId, now, ttl = STREAK_COOLDOWN_MS)) {
            out += MotivationalEvent.StreakMilestone(
                streakDays = tier,
                message = "🔥 Chuỗi $tier ngày — bạn đang rất bền!"
            )
            markFired(MotivationalEvent.Kind.STREAK_MILESTONE, challengeId, seasonKey, currentUserId, now)
        }

        return out.sortedByDescending { it.priority }
    }

    fun reset() {
        lastFiredAtMs.clear()
    }

    // ── Cooldown helpers ───────────────────────────────────────

    private fun cooldownKey(
        kind: MotivationalEvent.Kind,
        challengeId: Int,
        seasonKey: String,
        userId: String
    ): String = "${kind.name}|$challengeId|$seasonKey|$userId"

    private fun canFire(
        kind: MotivationalEvent.Kind,
        challengeId: Int,
        seasonKey: String,
        userId: String,
        now: Long,
        ttl: Long = DEFAULT_COOLDOWN_MS
    ): Boolean {
        val last = lastFiredAtMs[cooldownKey(kind, challengeId, seasonKey, userId)] ?: return true
        return (now - last) >= ttl
    }

    private fun markFired(
        kind: MotivationalEvent.Kind,
        challengeId: Int,
        seasonKey: String,
        userId: String,
        now: Long
    ) {
        lastFiredAtMs[cooldownKey(kind, challengeId, seasonKey, userId)] = now
    }

    private companion object {
        /** Threshold ranks the user can "enter" for the top-rank event. */
        val TIERS = listOf(3, 10, 50)

        /** Streak day-counts that trigger a milestone event. */
        val STREAK_TIERS = listOf(3, 7, 14, 21, 30, 60)

        /** 24h cooldown for non-streak events. */
        const val DEFAULT_COOLDOWN_MS: Long = 24L * 60L * 60L * 1000L

        /** 6h cooldown for streak milestones — streak only moves once
         *  per day max, so a shorter window is fine and lets users see
         *  the celebration the same day their streak ticks up. */
        const val STREAK_COOLDOWN_MS: Long = 6L * 60L * 60L * 1000L
    }
}
