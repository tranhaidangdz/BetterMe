package com.example.betterme.domain.ai

/**
 * Single point of contact between the rest of the app and the AI provider.
 *
 * Every AI-powered feature (group review, habit suggestions, smart reminders)
 * routes through here. The repository owns:
 * - Prompt construction (deterministic, includes the relevant analytics)
 * - Personality injection (system-prompt prefix)
 * - Model selection (free-tier first, fallback handled internally)
 * - Error normalization (network errors / rate-limits / empty responses all
 *   surface as `AiResult.Failure(message)` so call sites stay simple)
 *
 * Methods are suspend — caller awaits the full response. The OpenRouter free-tier
 * endpoint typically responds in 5-15s; the UI should show a loading state.
 */
interface AiHabitInsightRepository {

    sealed class AiResult {
        data class Success(val text: String) : AiResult()
        data class Failure(val message: String) : AiResult()
    }

    /**
     * Coach-style review of one habit category for the user.
     *
     * @param categoryName    "Vận động & thể chất", "Học tập", …
     * @param stats           narrative summary of the analytics. The repo embeds this verbatim
     *                        into the user prompt — pre-formatting keeps the AI from inventing
     *                        numbers.
     * @param personality     coaching tone selected by the user.
     */
    suspend fun reviewHabitGroup(
        categoryName: String,
        stats: String,
        personality: AiCoachPersonality
    ): AiResult

    /**
     * Habit suggestions for a category. The AI is prompted to return strict JSON;
     * the repo parses that JSON into [SuggestedHabit] objects so the UI doesn't need
     * to do any string parsing. On parse failure the call falls back to
     * [AiSuggestResult.Failure] with a clear message — no half-rendered suggestions
     * surface.
     */
    suspend fun suggestHabits(
        categoryName: String,
        existingHabitTitles: List<String>,
        personality: AiCoachPersonality
    ): AiSuggestResult

    sealed class AiSuggestResult {
        data class Success(val suggestions: List<SuggestedHabit>) : AiSuggestResult()
        data class Failure(val message: String) : AiSuggestResult()
    }
}

/**
 * One AI-generated habit suggestion. All fields are user-visible — the AI is
 * prompted to fill every one in Vietnamese.
 */
data class SuggestedHabit(
    val title: String,
    val emoji: String,
    val description: String,
    /** "EASY" | "MEDIUM" | "HARD" — used to color the difficulty badge. */
    val difficulty: String,
    /** Free-text impact line, e.g. "Cải thiện năng lượng buổi sáng". */
    val estimatedImpact: String,
    /** Why doing this *consistently* matters. e.g. "Chuỗi 21 ngày sẽ tạo phản xạ tự động". */
    val streakBenefit: String = ""
)
