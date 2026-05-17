package com.example.betterme.domain.ai

/**
 * Cache layer for AI responses. Keyed by `(categoryId, type)`; entries older than
 * [AiCacheRepository.TTL_MS] are treated as misses so the next call refreshes.
 *
 * Why this lives in the domain layer: use cases need to consult the cache before
 * hitting OpenRouter — having a domain interface keeps the use case clean of
 * Room types and makes it trivial to swap in an in-memory test double.
 *
 * Type discriminator strings are kept here as constants so the repo and the use
 * cases can't drift.
 */
interface AiCacheRepository {

    /**
     * Returns the cached content if a fresh row exists, else null. Freshness is
     * defined by [ttlMs] from the row's `createdAt`. Callers that don't need
     * a custom TTL keep using the default [TTL_MS_DEFAULT] (12h).
     *
     * Returns the *raw* string — REVIEW rows are plain text; SUGGESTIONS and
     * SCHEDULE_ANALYSIS rows are JSON the caller must decode.
     */
    suspend fun getFresh(categoryId: Int, type: String, ttlMs: Long = TTL_MS_DEFAULT): String?

    /** Returns the row (fresh or stale) — useful for offline fallback UI. */
    suspend fun getAny(categoryId: Int, type: String): CachedEntry?

    suspend fun save(categoryId: Int, type: String, content: String)

    suspend fun clear(categoryId: Int, type: String)

    data class CachedEntry(
        val content: String,
        val ageMs: Long
    )

    companion object {
        const val TYPE_REVIEW = "REVIEW"
        const val TYPE_SUGGESTIONS = "SUGGESTIONS"
        /** Schedule conflict analyzer results. Cached longer because the user's
         *  schedule doesn't change every hour. */
        const val TYPE_SCHEDULE_ANALYSIS = "SCHEDULE_ANALYSIS"

        /** AI onboarding starter-habit suggestions. Cached for the duration of
         *  the onboarding session so revisits don't burn quota; cleared when
         *  the user finalizes their habit selection. */
        const val TYPE_ONBOARDING = "ONBOARDING"

        /** Default TTL — 12 hours. */
        const val TTL_MS_DEFAULT = 12L * 60L * 60L * 1000L

        /** Schedule analyzer TTL — 24 hours. The user's schedule is stable enough
         *  to amortize one analysis per day. */
        const val TTL_MS_SCHEDULE = 24L * 60L * 60L * 1000L

        /** @deprecated use [TTL_MS_DEFAULT]. Kept for binary compatibility. */
        const val TTL_MS = TTL_MS_DEFAULT
    }
}
