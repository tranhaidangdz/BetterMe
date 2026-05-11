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
     * @param stats           narrative summary of the analytics (see
     *                        [com.example.betterme.domain.usecase.ai.HabitGroupStatsSnapshot]
     *                        for the canonical shape). The repo embeds this verbatim into the
     *                        user prompt — pre-formatting keeps the AI from inventing numbers.
     * @param personality     coaching tone selected by the user.
     */
    suspend fun reviewHabitGroup(
        categoryName: String,
        stats: String,
        personality: AiCoachPersonality
    ): AiResult
}
