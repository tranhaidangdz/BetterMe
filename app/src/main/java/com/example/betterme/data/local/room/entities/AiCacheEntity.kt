package com.example.betterme.data.local.room.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Persisted AI response cache. Backs the cache-first strategy for AI features:
 *
 * - Group review: one entry per `(categoryId, REVIEW)` — cheap repeat reads without
 *   burning Gemini free-tier tokens, plus survives screen rotation / app restart.
 * - Suggestions: one entry per `(categoryId, SUGGESTIONS)`. Stored as JSON so the
 *   repository can decode back into [com.example.betterme.domain.ai.SuggestedHabit].
 *
 * Why a single table for both types: the schema is identical (key + type + blob +
 * timestamp) and the row count is tiny (≤ 2 per category). Splitting tables would
 * add migrations and DAO surface area for no gain.
 *
 * The unique index on (categoryId, type) means inserts use `REPLACE` to refresh
 * the latest response — no orphan rows, no de-dup logic in the repo.
 */
@Entity(
    tableName = "ai_cache",
    indices = [Index(value = ["categoryId", "type"], unique = true)]
)
data class AiCacheEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    /** Habit category this cache row belongs to. */
    val categoryId: Int,
    /** Discriminator: "REVIEW" or "SUGGESTIONS". Plain string for DAO simplicity. */
    val type: String,
    /** Free-text for REVIEW; JSON array for SUGGESTIONS. Decoded by the repo. */
    val content: String,
    /** Epoch millis when the response was generated. Used to expire the row at 12h. */
    val createdAt: Long
)
