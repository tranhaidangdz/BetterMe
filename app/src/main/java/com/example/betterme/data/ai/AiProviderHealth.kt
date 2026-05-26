package com.example.betterme.data.ai

import com.example.betterme.domain.ai.AiErrorCategory
import java.util.EnumMap
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

/**
 * Process-scoped health snapshot per [AiProvider]. The router records every
 * terminal attempt (success or failure) here so a developer can ask "which
 * provider answered last, and how?" without grepping Logcat.
 *
 * Deliberately tiny on purpose: each entry is just (lastSuccessAt, lastFailureAt,
 * lastFailureCategory, lastSuccessModel). No EWMA, no per-model histograms — the
 * pool cooldowns already give the chain walker actionable failover; this layer
 * is *diagnostic*, not load-bearing.
 *
 * Concurrency: a single [ReentrantLock] guards the EnumMap. Updates and reads
 * are O(1) so the lock is uncontended in practice.
 *
 * Lifetime: process-scoped via Koin `single`. State resets on process death,
 * which is fine — diagnostics about "what happened in this session" is the
 * useful question; persisting across cold starts would just hide stale info.
 */
class AiProviderHealth(
    private val nowProvider: () -> Long = { System.currentTimeMillis() }
) {

    data class Entry(
        val lastSuccessAt: Long = 0L,
        val lastSuccessModel: String? = null,
        val lastFailureAt: Long = 0L,
        val lastFailureCategory: AiErrorCategory? = null
    )

    private val states: EnumMap<AiProvider, Entry> = EnumMap(AiProvider::class.java)
    private val mu = ReentrantLock()

    fun recordSuccess(provider: AiProvider, model: String) = mu.withLock {
        val now = nowProvider()
        val prev = states[provider] ?: Entry()
        states[provider] = prev.copy(lastSuccessAt = now, lastSuccessModel = model)
    }

    fun recordFailure(provider: AiProvider, category: AiErrorCategory) = mu.withLock {
        val now = nowProvider()
        val prev = states[provider] ?: Entry()
        states[provider] = prev.copy(lastFailureAt = now, lastFailureCategory = category)
    }

    /** Snapshot of every tracked provider. Returns a copy so callers can't mutate. */
    fun snapshot(): Map<AiProvider, Entry> = mu.withLock { states.toMap() }

    /**
     * One-line summary for Logcat triage. Format:
     * `GEMINI(ok=1200ms ago model=gemini-2.5-flash) OPENROUTER(fail=3000ms ago cat=RATE_LIMITED)`
     */
    fun debugStatusLine(): String = mu.withLock {
        val now = nowProvider()
        if (states.isEmpty()) return "AiProviderHealth(empty)"
        AiProvider.ORDERED.joinToString(" ") { p ->
            val e = states[p] ?: return@joinToString "$p(untouched)"
            val parts = buildList {
                if (e.lastSuccessAt > 0) {
                    add("ok=${now - e.lastSuccessAt}ms model=${e.lastSuccessModel ?: "?"}")
                }
                if (e.lastFailureAt > 0) {
                    add("fail=${now - e.lastFailureAt}ms cat=${e.lastFailureCategory}")
                }
            }
            "$p(${parts.joinToString(" ")})"
        }
    }
}
