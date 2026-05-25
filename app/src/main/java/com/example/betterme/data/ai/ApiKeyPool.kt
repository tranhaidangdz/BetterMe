package com.example.betterme.data.ai

import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

/**
 * In-memory pool of API keys for a single provider, with per-key cooldown.
 *
 * Acquisition is round-robin over healthy keys: each call to [acquireHealthy]
 * advances an internal cursor so requests spread across keys instead of
 * hammering whichever one happens to be first. A key whose [cooldown] was
 * called returns false to "is the key cooled until `now`?" until the cooldown
 * timestamp elapses; the cursor skips it.
 *
 * Cooldown windows are passed in by the caller (the router) so policy lives in
 * one place: 60 s for `RATE_LIMITED`, 10 min for `INVALID_KEY` / `QUOTA_EXCEEDED`.
 * Transient categories (TIMEOUT, SERVER_ERROR, MODEL_UNAVAILABLE) shouldn't cool
 * the key at all — the problem is upstream of the credential.
 *
 * Concurrency: a single [ReentrantLock] guards cursor + key states. The lock
 * isn't held across network I/O — callers receive a snapshot of the key value
 * and release the lock before issuing the request.
 */
class ApiKeyPool(
    keys: List<String>,
    private val nowProvider: () -> Long = { System.currentTimeMillis() }
) {
    private data class KeyState(val key: String, var cooldownUntil: Long = 0L)

    private val states: MutableList<KeyState> =
        keys.filter { it.isNotBlank() }.map { KeyState(it) }.toMutableList()
    private val mu = ReentrantLock()
    private var cursor: Int = 0

    /** Number of keys configured, regardless of health. */
    val size: Int get() = mu.withLock { states.size }

    /** True iff no keys were configured at all. */
    val isEmpty: Boolean get() = mu.withLock { states.isEmpty() }

    /**
     * Returns the next healthy key (cooldownUntil <= now), advancing the
     * round-robin cursor. Returns null when every key is currently cooled —
     * the router treats that as "this provider is exhausted, fall through to
     * the next provider".
     */
    fun acquireHealthy(): String? = mu.withLock {
        val n = states.size
        if (n == 0) return null
        val now = nowProvider()
        repeat(n) {
            val idx = cursor
            cursor = (cursor + 1) % n
            val s = states[idx]
            if (s.cooldownUntil <= now) return s.key
        }
        null
    }

    /**
     * Mark a key cooled until `now + durationMs`. Safe to call with an unknown
     * key (no-op) — happens if a parallel rotation already evicted it.
     */
    fun cooldown(key: String, durationMs: Long) = mu.withLock {
        if (durationMs <= 0L) return@withLock
        val now = nowProvider()
        states.find { it.key == key }?.let { it.cooldownUntil = now + durationMs }
    }

    /**
     * Returns true iff every configured key is currently cooled. Used by tests
     * and by router diagnostics; not part of the hot path.
     */
    fun allCooled(): Boolean = mu.withLock {
        val now = nowProvider()
        states.isNotEmpty() && states.all { it.cooldownUntil > now }
    }

    /**
     * For logging only — masks each key so only its first 6 and last 2 chars
     * are exposed. Never logs the raw key, never returns it.
     */
    fun debugStatusLine(): String = mu.withLock {
        val now = nowProvider()
        states.joinToString(", ", prefix = "[", postfix = "]") { s ->
            val masked = mask(s.key)
            val cool = if (s.cooldownUntil > now) " (cool ${s.cooldownUntil - now}ms)" else ""
            "$masked$cool"
        }
    }

    companion object {
        /**
         * Mask helper exposed for the network module to log "key in use" lines
         * without leaking the raw secret. Keeps a stable 8-char fragment so
         * support can correlate "key6 is failing" reports without ever seeing
         * the full value.
         */
        fun mask(key: String): String = when {
            key.isBlank() -> "(blank)"
            key.length <= 8 -> "***"
            else -> "${key.take(6)}…${key.takeLast(2)}"
        }
    }
}
