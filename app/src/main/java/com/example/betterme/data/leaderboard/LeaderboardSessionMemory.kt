package com.example.betterme.data.leaderboard

import com.example.betterme.domain.repository.ChallengeLeaderboardRepository.LeaderboardSnapshot
import java.util.concurrent.ConcurrentHashMap

/**
 * Process-scoped cache for leaderboard reads + write throttle for the
 * sync path. Sits inside the data layer because both fields it manages
 * (cached snapshot + last write time) are Firestore-I/O concerns.
 *
 * ### Why a session cache and not the AiCacheRepository?
 * The AI cache is Room-backed and content-keyed (12-24h TTL). Leaderboards
 * change minute-to-minute as users check in; a long TTL would make the
 * UI feel stale. A short in-memory session cache (5 min default) gives
 * the right tradeoff — fast tab toggles, no thrash, no leftover state on
 * sign-out.
 *
 * ### Write throttle
 * Per-(userId, challengeId), keeps `lastWriteAtMs` so we can skip
 * Firestore writes within [DEFAULT_WRITE_THROTTLE_MS]. Avoids burning
 * Firestore quota when a user rapidly logs multiple check-ins in a row
 * (e.g. catching up after an offline day).
 */
class LeaderboardSessionMemory {

    private val snapshots = ConcurrentHashMap<String, CachedSnapshot>()
    private val lastWriteAtMs = ConcurrentHashMap<String, Long>()

    /**
     * Cached snapshot if fresh (within [READ_CACHE_TTL_MS]). Returns null
     * when missing or expired so the caller re-reads from Firestore.
     */
    fun get(challengeId: Int, seasonKey: String, now: Long = System.currentTimeMillis()): LeaderboardSnapshot? {
        val cached = snapshots[key(challengeId, seasonKey)] ?: return null
        if (now - cached.fetchedAtMs > READ_CACHE_TTL_MS) return null
        return cached.snapshot
    }

    /** Stash a successful snapshot. */
    fun put(challengeId: Int, seasonKey: String, snapshot: LeaderboardSnapshot, now: Long = System.currentTimeMillis()) {
        snapshots[key(challengeId, seasonKey)] = CachedSnapshot(snapshot, now)
    }

    /**
     * Last-resort lookup for offline fallback — returns the cached
     * snapshot even when stale, flagged with `isStale = true` so the UI
     * can render an offline chip.
     */
    fun getAny(challengeId: Int, seasonKey: String): LeaderboardSnapshot? {
        val cached = snapshots[key(challengeId, seasonKey)] ?: return null
        return cached.snapshot.copy(isStale = true)
    }

    /**
     * Returns true when a Firestore write SHOULD be skipped because one
     * landed within the throttle window. Caller passes `force = true`
     * to bypass — used on challenge completion where the new streak
     * needs to be reflected immediately.
     */
    fun shouldThrottleWrite(
        userId: String,
        challengeId: Int,
        force: Boolean,
        now: Long = System.currentTimeMillis()
    ): Boolean {
        if (force) return false
        val last = lastWriteAtMs[writeKey(userId, challengeId)] ?: return false
        return (now - last) < DEFAULT_WRITE_THROTTLE_MS
    }

    fun markWritten(userId: String, challengeId: Int, now: Long = System.currentTimeMillis()) {
        lastWriteAtMs[writeKey(userId, challengeId)] = now
    }

    /** Clear all cached snapshots — used by sign-out / explicit reset. */
    fun invalidate() {
        snapshots.clear()
        // intentionally NOT clearing lastWriteAtMs: the throttle is a
        // quota-protection mechanism, not a UX one, and persists across
        // logical "screens".
    }

    private fun key(challengeId: Int, seasonKey: String): String = "$challengeId@$seasonKey"
    private fun writeKey(userId: String, challengeId: Int): String = "$userId@$challengeId"

    private data class CachedSnapshot(val snapshot: LeaderboardSnapshot, val fetchedAtMs: Long)

    private companion object {
        /** 5 minutes — tab toggles + nav backstack pops feel instant; new
         *  check-ins picked up on next deliberate refresh. */
        const val READ_CACHE_TTL_MS: Long = 5L * 60 * 1000

        /** 30 seconds between Firestore writes for the same user+challenge. */
        const val DEFAULT_WRITE_THROTTLE_MS: Long = 30L * 1000
    }
}
