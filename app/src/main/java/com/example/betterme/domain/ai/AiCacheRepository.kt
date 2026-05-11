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
     * defined by [TTL_MS] from the row's `createdAt`.
     *
     * Returns the *raw* string — REVIEW rows are plain text; SUGGESTIONS rows are
     * JSON the caller must decode.
     */
    suspend fun getFresh(categoryId: Int, type: String): String?

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
        /** 12 hours — long enough that a single user session doesn't re-hit the model
         *  for the same screen, short enough that stale stats don't haunt the UI. */
        const val TTL_MS = 12L * 60L * 60L * 1000L
    }
}
