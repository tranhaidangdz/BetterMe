package com.example.betterme.domain.usecase.ai

import android.util.Log
import com.example.betterme.data.local.datastore.DataStoreManager
import com.example.betterme.domain.ai.AiCacheRepository
import com.example.betterme.domain.ai.AiCacheRepository.Companion.TYPE_SUGGESTIONS
import com.example.betterme.domain.ai.AiCoachPersonality
import com.example.betterme.domain.ai.AiHabitInsightRepository
import com.example.betterme.domain.ai.AiHabitInsightRepository.AiSuggestResult
import com.example.betterme.domain.ai.SuggestedHabit
import com.example.betterme.domain.ai.personalization.CategoryKind
import com.example.betterme.domain.ai.personalization.PersonalitySignal
import com.example.betterme.domain.ai.personalization.SuggestionContext
import com.example.betterme.domain.repository.CategoryRepository
import com.example.betterme.domain.repository.HabitLogRepository
import com.example.betterme.domain.repository.HabitRepository
import com.example.betterme.utils.DateUtils
import kotlinx.coroutines.flow.first
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Generates AI habit suggestions for a category.
 *
 * The use case now builds a [SuggestionContext] that includes:
 *  - habits already in THIS category (avoid duplicate titles),
 *  - habits across the WHOLE app (avoid duplicate intents in another
 *    category — a Fitness suggestion shouldn't overlap a Health one),
 *  - the inferred [CategoryKind] coverage the user already has,
 *  - derived [PersonalitySignal]s,
 *  - workload and overall completion rate.
 *
 * The repo reads these to:
 *  - feed the AI a context-rich prompt that forbids cardio when the
 *    user is already a runner, recovery picks when the user is
 *    overloaded, etc.
 *  - select a category-aware canned pool with rotation when every
 *    OpenRouter model fails.
 */
class SuggestHabitsForCategoryUseCase(
    private val dataStoreManager: DataStoreManager,
    private val habitRepository: HabitRepository,
    private val habitLogRepository: HabitLogRepository,
    private val categoryRepository: CategoryRepository,
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

        val allHabits = habitRepository.getHabits(userId).first()
        val todayMs = DateUtils.startOfDay()
        val activeHabits = allHabits.filter { it.end_date == null || it.end_date >= todayMs }

        val existingInCategory = activeHabits.filter { it.category_id == categoryId }.map { it.title }
        val existingAcrossApp = activeHabits.map { it.title }

        // Resolve category coverage by classifying every existing habit's
        // category name. Lets the AI / canned pool see "user already has
        // a Fitness habit in another category" — useful when categories
        // overlap intent.
        val allCategories = categoryRepository.getAll().first().associateBy { it.id }
        val existingKindCoverage = activeHabits
            .mapNotNull { allCategories[it.category_id ?: -1]?.name }
            .map { CategoryKind.classify(it) }
            .toSet()

        // Personality signals + overall completion across all habits.
        val logsByHabitId = allHabits.associate { habit ->
            habit.id to habitLogRepository.getLogs(habit.id).first()
        }
        val signals = PersonalitySignal.derive(allHabits, logsByHabitId, todayMs)

        var totalPlanned = 0
        var totalDone = 0
        for (habit in activeHabits) {
            val planned = if (habit.end_date != null) {
                (((habit.end_date - habit.start_date) / DAY_MS) + 1).toInt().coerceAtLeast(1)
            } else {
                (((todayMs - habit.start_date) / DAY_MS) + 1).toInt().coerceAtLeast(1)
            }
            totalPlanned += planned
            totalDone += habitLogRepository.countCompleted(habit.id)
        }
        val overallRate = if (totalPlanned > 0) ((totalDone.toFloat() / totalPlanned) * 100).toInt() else 0

        val context = SuggestionContext(
            categoryName = categoryName,
            categoryKind = CategoryKind.classify(categoryName),
            existingInCategory = existingInCategory,
            existingAcrossApp = existingAcrossApp,
            existingKindCoverage = existingKindCoverage,
            signals = signals,
            activeHabitCount = activeHabits.size,
            overallRate = overallRate
        )

        val result = aiRepository.suggestHabits(
            context = context,
            personality = personality
        )

        if (result is AiSuggestResult.Success) {
            cache.save(categoryId, TYPE_SUGGESTIONS, encode(result.suggestions))
            return result
        }

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

    private companion object {
        const val DAY_MS: Long = 24L * 60L * 60L * 1000L
    }
}
