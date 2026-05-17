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
 * ### Structured apply fields
 * The optional `suggested…` fields carry the *concrete* mutation the AI is
 * recommending — e.g. a CHANGE_TIME suggestion fills in `suggestedReminderTime
 * = "18:30"`. The Add Habit screen reads these when the user taps "Áp dụng"
 * and patches the corresponding form fields directly:
 *
 *  - [suggestedTitle], [suggestedReminderTime], [suggestedCategory] map to
 *    real `HabitEntity` columns and DO mutate the form.
 *  - [suggestedDifficulty], [suggestedDurationMinutes], [suggestedFrequency]
 *    don't yet have backing columns on `HabitEntity`. The card still shows
 *    them as hints, but applying them is a no-op — the AddHabit VM ignores
 *    them silently. When those columns land later, only the VM changes.
 *  - [suggestedReplacementHabit] is the title of an existing habit the AI is
 *    proposing to swap into instead of creating a new one (used with
 *    REPLACE_EXISTING / MERGE_EXISTING). Advisory-only this iteration — the
 *    UI shows it as a hint; we never auto-mutate an existing habit from the
 *    creation flow.
 *
 * Every field is optional and defaults to null, so historical responses
 * (parser fallback path) and the canned offline analysis stay compatible.
 */
data class HabitCreationSuggestion(
    val type: CreationSuggestionType,
    /** ≤ 2-sentence Vietnamese message. */
    val message: String,
    val suggestedTitle: String? = null,
    /** "HH:mm" 24-hour. */
    val suggestedReminderTime: String? = null,
    /** EASY | MEDIUM | HARD. */
    val suggestedDifficulty: String? = null,
    /** "daily" | "3x/week" | etc. */
    val suggestedFrequency: String? = null,
    /** Vietnamese category name — the VM looks this up in the loaded category list. */
    val suggestedCategory: String? = null,
    val suggestedDurationMinutes: Int? = null,
    /** Title of an existing habit being proposed as the replacement target. */
    val suggestedReplacementHabit: String? = null
) {
    /**
     * True when any field would actually mutate AddHabit form state today
     * (title / reminderTime / categoryName). Drives whether the per-suggestion
     * "Áp dụng" CTA renders.
     */
    val hasApplicableMutation: Boolean
        get() = !suggestedTitle.isNullOrBlank() ||
            !suggestedReminderTime.isNullOrBlank() ||
            !suggestedCategory.isNullOrBlank()
}

enum class CreationSuggestionType {
    MERGE_EXISTING,
    REPLACE_EXISTING,
    REDUCE_INTENSITY,
    REDUCE_FREQUENCY,
    CHANGE_TIME,
    START_SMALLER,
    TRY_ALTERNATIVE
}
