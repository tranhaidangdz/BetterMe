package com.example.betterme.domain.ai

import com.example.betterme.domain.ai.habitcreation.HabitCreationAnalysis
import com.example.betterme.domain.ai.habitcreation.HabitCreationInput
import com.example.betterme.domain.ai.lifestyle.HabitCompletionRecord
import com.example.betterme.domain.ai.lifestyle.LifestyleInsight
import com.example.betterme.domain.ai.recovery.HabitRecoveryAnalysis
import com.example.betterme.domain.ai.recovery.HabitRecoveryInput
import com.example.betterme.domain.ai.onboarding.OnboardingProfile
import com.example.betterme.domain.ai.onboarding.OnboardingSuggestion
import com.example.betterme.domain.ai.schedule.ScheduleAnalysis
import com.example.betterme.domain.ai.schedule.UserLifestyleProfile

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
        /**
         * @param isCanned true when the response is a local "hard fallback" served
         *                 because every OpenRouter model in the chain failed. Use
         *                 cases must NOT persist canned content into the 12h cache —
         *                 next visit might have working connectivity.
         */
        data class Success(val text: String, val isCanned: Boolean = false) : AiResult()
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
        /** [isCanned] semantics match [AiResult.Success.isCanned]. */
        data class Success(
            val suggestions: List<SuggestedHabit>,
            val isCanned: Boolean = false
        ) : AiSuggestResult()
        data class Failure(val message: String) : AiSuggestResult()
    }

    /**
     * Analyzes a habit schedule for overlaps, overload, sleep balance, transition
     * realism, and burnout risk. Always returns a [ScheduleAnalysis] — when every
     * model in the fallback chain fails, a handwritten local analysis is served
     * with `isCanned = true` so the UI never has to handle a hard error path.
     *
     * @param profile sleep / wake / work-hours context. Pass null to use [UserLifestyleProfile.Default].
     * @param habits  domain-side input shape (see [ScheduleHabitInput]); the use
     *                case translates from [com.example.betterme.data.local.room.entities.HabitEntity].
     */
    suspend fun analyzeSchedule(
        profile: UserLifestyleProfile?,
        habits: List<ScheduleHabitInput>
    ): ScheduleAnalysis

    /**
     * Generates an opening 4–6 habit starter set for a new BetterMe user.
     * The prompt enforces sustainable defaults (no 4 AM wake-ups, mostly
     * EASY/MEDIUM, ≤2 HARD, etc.) and respects [OnboardingProfile.selectedCategories]
     * when non-empty.
     *
     * Always returns an [OnboardingSuggestion]. When every OpenRouter model in
     * the fallback chain fails, the repo serves a handwritten 4-habit beginner
     * starter set with `isCanned = true` so the onboarding flow never deadlocks
     * on a network error.
     *
     * @param profile   user inputs (goals, categories, experience, activity, etc.)
     * @param lifestyle sleep / work / meal anchors. Pass null to use
     *                  [UserLifestyleProfile.Default] (healthy baseline).
     */
    suspend fun suggestOnboardingHabits(
        profile: OnboardingProfile,
        lifestyle: UserLifestyleProfile?
    ): OnboardingSuggestion

    /**
     * Long-term adaptive coaching. Reads a 14-day rollup of habit completion +
     * detected behavioral patterns and returns gentle, sustainable adjustments.
     * Always returns a [LifestyleInsight] — when every OpenRouter model fails,
     * the repo serves a deterministic local "stable baseline" insight flagged
     * `isCanned = true`.
     *
     * @param lifestyle             sleep / work / meal anchors; null → use Default.
     * @param history               per-habit completion rollup, last 14 days.
     * @param missedPatterns        detected behavioral patterns (e.g. "late-night habits",
     *                              "weekend inconsistency"). Empty when no patterns.
     * @param activeHabitTitles     just the active habit titles, for the prompt's
     *                              "Current active habits" section.
     * @param wellnessSignals       optional signals like "sleep-debt". Empty until
     *                              BetterMe tracks mood/sleep explicitly.
     */
    suspend fun analyzeLifestyle(
        lifestyle: UserLifestyleProfile?,
        history: List<HabitCompletionRecord>,
        missedPatterns: List<String>,
        activeHabitTitles: List<String>,
        wellnessSignals: List<String>
    ): LifestyleInsight

    /**
     * Pre-save advisory check fired when the user taps Save on the Add Habit
     * form. Compares the new habit against the user's existing routine and
     * returns warnings + sustainable alternatives. The result is **purely
     * advisory** — the UI always proceeds to save on "Vẫn tạo" regardless of
     * the analysis content; the assistant never blocks creation.
     *
     * Always returns a [HabitCreationAnalysis]. When every OpenRouter model
     * in the fallback chain fails, the repo runs a deterministic rule-based
     * local analysis (overlap, late-night, overload, duplicate title) and
     * flags `isCanned = true`.
     */
    suspend fun analyzeHabitCreation(input: HabitCreationInput): HabitCreationAnalysis

    /**
     * Proactive recovery coaching when the user is struggling. Should be
     * called by the use case ONLY after deterministic trigger detection
     * (low completion, miss streaks, late-night failures, overload) — the
     * AI never decides on its own whether to surface a recovery card.
     *
     * Always returns a [HabitRecoveryAnalysis]. When every model in the
     * fallback chain fails, the repo derives a deterministic local plan from
     * the same struggle stats the prompt would have consumed and flags
     * `isCanned = true`.
     */
    suspend fun analyzeHabitRecovery(input: HabitRecoveryInput): HabitRecoveryAnalysis
}

/**
 * Per-habit input row the schedule analyzer expects in its runtime user prompt.
 *
 * BetterMe's [com.example.betterme.data.local.room.entities.HabitEntity] doesn't
 * yet store [difficulty], [priority] or [estimatedMinutes]; the use case fills
 * defaults until those fields are surfaced through the Add Habit form. Keeping
 * the input shape rich now means the prompt is future-proof without churn here.
 */
data class ScheduleHabitInput(
    val id: Int,
    val title: String,
    /** "HH:mm" 24-hour. Habits without a reminder are filtered out by the use case. */
    val reminderTime: String,
    /** EASY | MEDIUM | HARD. */
    val difficulty: String,
    /** LOW | MEDIUM | HIGH. */
    val priority: String,
    val estimatedMinutes: Int
)

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
