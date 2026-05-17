package com.example.betterme.domain.ai.habitcreation

/**
 * One actionable adjustment the AI proposes instead of (or in addition to)
 * creating the new habit as-is.
 *
 * `SuggestionType` is **intentionally renamed to `CreationSuggestionType`** to
 * avoid colliding with `com.example.betterme.domain.ai.lifestyle.SuggestionType`
 * — the two enums describe different actions on different surfaces, and the
 * repository implementation imports both.
 *
 * Semantics of each [type]:
 *
 *  - [CreationSuggestionType.MERGE_EXISTING]   — combine new habit into one already on the list
 *  - [CreationSuggestionType.REPLACE_EXISTING] — upgrade an existing habit instead of adding a new one
 *  - [CreationSuggestionType.REDUCE_INTENSITY] — lower the difficulty of the new habit
 *  - [CreationSuggestionType.REDUCE_FREQUENCY] — schedule less often than originally planned
 *  - [CreationSuggestionType.CHANGE_TIME]      — pick a different reminder time
 *  - [CreationSuggestionType.START_SMALLER]    — begin with a shorter duration / easier version
 *  - [CreationSuggestionType.TRY_ALTERNATIVE]  — try a related but more sustainable habit
 */
data class HabitCreationSuggestion(
    val type: CreationSuggestionType,
    /** ≤ 2-sentence Vietnamese message. */
    val message: String
)

enum class CreationSuggestionType {
    MERGE_EXISTING,
    REPLACE_EXISTING,
    REDUCE_INTENSITY,
    REDUCE_FREQUENCY,
    CHANGE_TIME,
    START_SMALLER,
    TRY_ALTERNATIVE
}
