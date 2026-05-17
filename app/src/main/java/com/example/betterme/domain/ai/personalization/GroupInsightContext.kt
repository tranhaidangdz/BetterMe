package com.example.betterme.domain.ai.personalization

/**
 * Everything the AI repo + canned template pool need to produce a
 * group-aware, behavior-aware review for one habit category.
 *
 * The use case computes this once from Room data; the repo embeds the
 * fields into the user prompt verbatim AND consults [signals] +
 * [categoryKind] when picking a canned template. Centralizing the bundle
 * here lets the canned pool and the AI prompt see the same shape — they
 * can't drift on "what counts as an overloaded user".
 *
 * The same context object is used for the GENERATIVE path (real AI call)
 * and the canned offline path so users never get a tonally inconsistent
 * experience just because their connection dropped.
 */
data class GroupInsightContext(
    val categoryName: String,
    val categoryKind: CategoryKind,
    val habitCount: Int,
    val overallRate: Int,
    val completedJourneys: Int,
    val missedDays: Int,
    val bestStreak: Int,
    /**
     * Pre-formatted one line per habit. Each line MUST include the habit
     * title in quotes — the system prompt relies on these tokens being
     * present to reference habits by name in the coaching message.
     */
    val perHabitLines: List<String>,
    /** Habit titles in this group, surfaced separately for canned templates. */
    val habitTitles: List<String>,
    val signals: Set<PersonalitySignal>,
    /** Distribution hint like ["evening-heavy", "no-morning-habits"]. */
    val timeOfDayHints: List<String>,
    /** Week-over-week trend label: "improving" | "holding" | "drifting" | "no-data". */
    val trendLabel: String
)

/**
 * Suggestion-flow analog of [GroupInsightContext]. Carries the user's
 * existing routine richly enough that the AI (and the canned pool) can
 * avoid suggesting habits whose intent the user already covers.
 *
 * [existingKindCoverage] is the deduplicated set of [CategoryKind]s
 * implied by the user's existing habits across ALL categories — not just
 * this one. A user who already runs daily shouldn't get cardio
 * suggestions in the Fitness group; classifying every existing habit
 * title gives the model the broader context.
 */
data class SuggestionContext(
    val categoryName: String,
    val categoryKind: CategoryKind,
    val existingInCategory: List<String>,
    val existingAcrossApp: List<String>,
    val existingKindCoverage: Set<CategoryKind>,
    val signals: Set<PersonalitySignal>,
    val activeHabitCount: Int,
    val overallRate: Int
)
