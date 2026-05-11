package com.example.betterme.domain.usecase.ai

import android.util.Log
import com.example.betterme.data.local.datastore.DataStoreManager
import com.example.betterme.domain.ai.AiCacheRepository
import com.example.betterme.domain.ai.AiCacheRepository.Companion.TYPE_SUGGESTIONS
import com.example.betterme.domain.ai.AiCoachPersonality
import com.example.betterme.domain.ai.AiHabitInsightRepository
import com.example.betterme.domain.ai.AiHabitInsightRepository.AiSuggestResult
import com.example.betterme.domain.ai.SuggestedHabit
import com.example.betterme.domain.repository.HabitRepository
import kotlinx.coroutines.flow.first
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Generates AI habit suggestions for a category.
 *
 * - Pulls the user's existing habit titles in that category so the AI never suggests
 *   what they already have.
 * - Cache-first: a fresh cached list returns instantly. `forceRefresh = true` (user
 *   taps "Tạo lại") bypasses cache.
 * - On network failure, falls back to a stale cache entry if one exists so the user
 *   sees suggestions instead of a blank error.
 *
 * Cache content is a small JSON envelope (not the raw OpenRouter blob) so future
 * additions to [SuggestedHabit] auto-tolerate missing fields without breaking older
 * stored rows.
 */
class SuggestHabitsForCategoryUseCase(
    private val dataStoreManager: DataStoreManager,
    private val habitRepository: HabitRepository,
    private val aiRepository: AiHabitInsightRepository,
    private val cache: AiCacheRepository
) {
    suspend operator fun invoke(
        categoryId: Int,
        categoryName: String,
        personality: AiCoachPersonality = AiCoachPersonality.Default,
        forceRefresh: Boolean = false
    ): AiSuggestResult {
        if (!forceRefresh) {
            cache.getFresh(categoryId, TYPE_SUGGESTIONS)?.let { cachedJson ->
                decode(cachedJson)?.let { return AiSuggestResult.Success(it) }
            }
        }

        val userId = dataStoreManager.getCurrentUserId().first().orEmpty()
        if (userId.isBlank()) {
            return AiSuggestResult.Failure("Vui lòng đăng nhập để dùng AI")
        }
        val existing = habitRepository.getHabits(userId).first()
            .filter { it.category_id == categoryId }
            .map { it.title }
        val result = aiRepository.suggestHabits(
            categoryName = categoryName,
            existingHabitTitles = existing,
            personality = personality
        )

        // Real model success → persist for the next 12h.
        // Canned success (all models failed) → DO NOT cache. Canned suggestions
        // are generic; we don't want them to sit in cache for 12h once the
        // network is back.
        if (result is AiSuggestResult.Success) {
            if (!result.isCanned) {
                cache.save(categoryId, TYPE_SUGGESTIONS, encode(result.suggestions))
            }
            return result
        }

        // Network failed entirely → fall back to whatever we have, fresh or stale.
        cache.getAny(categoryId, TYPE_SUGGESTIONS)?.let { stale ->
            decode(stale.content)?.let { return AiSuggestResult.Success(it) }
        }
        return result
    }

    private fun encode(list: List<SuggestedHabit>): String =
        json.encodeToString(list.map(::toDto))

    private fun decode(raw: String): List<SuggestedHabit>? = try {
        json.decodeFromString<List<SuggestionDto>>(raw).map(::fromDto)
    } catch (e: Exception) {
        Log.w("SuggestUseCase", "Failed to decode cached suggestions", e)
        null
    }

    @Serializable
    private data class SuggestionDto(
        val title: String,
        val emoji: String,
        val description: String,
        val difficulty: String,
        val estimatedImpact: String,
        val streakBenefit: String = ""
    )

    private fun toDto(s: SuggestedHabit) = SuggestionDto(
        title = s.title,
        emoji = s.emoji,
        description = s.description,
        difficulty = s.difficulty,
        estimatedImpact = s.estimatedImpact,
        streakBenefit = s.streakBenefit
    )

    private fun fromDto(d: SuggestionDto) = SuggestedHabit(
        title = d.title,
        emoji = d.emoji,
        description = d.description,
        difficulty = d.difficulty,
        estimatedImpact = d.estimatedImpact,
        streakBenefit = d.streakBenefit
    )

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }
}
