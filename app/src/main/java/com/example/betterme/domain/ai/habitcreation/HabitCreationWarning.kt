package com.example.betterme.domain.ai.habitcreation

/**
 * One issue the AI spotted while comparing the new habit against the user's
 * existing routine. Up to ~3 warnings per analysis (parser-enforced).
 *
 * Semantics of each [type] (kept here so the prompt and the use case can't
 * drift on what each enum value actually means):
 *
 *  - [WarningType.TIME_CONFLICT]    — new habit's reminder overlaps or is too close to an existing one
 *  - [WarningType.DUPLICATE_INTENT] — semantically similar to a habit the user already has
 *  - [WarningType.OVERLOAD_RISK]    — too many HARD habits, too much daily duration
 *  - [WarningType.SLEEP_CONFLICT]   — new habit lands late at night / inside the sleep window
 *  - [WarningType.TOO_MANY_HABITS]  — total active habit count is already high
 *  - [WarningType.REDUNDANT_CATEGORY] — same category already well-covered
 *  - [WarningType.TOO_INTENSE]      — difficulty / duration not realistic for the user's level
 *  - [WarningType.TOO_FREQUENT]     — frequency unsustainable (e.g. 7×/week new HARD habit)
 */
data class HabitCreationWarning(
    val type: WarningType,
    /** ≤ 2-sentence Vietnamese message. */
    val message: String
)

enum class WarningType {
    TIME_CONFLICT,
    DUPLICATE_INTENT,
    OVERLOAD_RISK,
    SLEEP_CONFLICT,
    TOO_MANY_HABITS,
    REDUNDANT_CATEGORY,
    TOO_INTENSE,
    TOO_FREQUENT
}
