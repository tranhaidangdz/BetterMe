package com.example.betterme.domain.ai.progression

/**
 * One progression suggestion — the upbeat mirror of
 * [com.example.betterme.domain.ai.recovery.HabitRecoveryAction].
 *
 * All action types are advisory in this iteration: BetterMe's HabitEntity
 * doesn't yet store duration / frequency / difficulty, so progressing
 * those numerically requires the user to edit the habit themselves. The
 * UI shows a "Tham khảo" badge instead of an Apply CTA for that reason —
 * we'd rather under-promise than offer an Apply button that does nothing.
 *
 * @param targetHabit Title of the existing habit being progressed. Null
 *                    for ADD_COMPLEMENTARY_HABIT (it adds a *new* habit).
 * @param suggestedValue Free-form value the model writes — "20 phút",
 *                    "4 lần/tuần", "6:30 sáng", etc. UI renders it as a
 *                    💡 hint, never parses it.
 */
data class HabitProgressionAction(
    val type: ProgressionActionType,
    val targetHabit: String?,
    val title: String,
    val description: String,
    val suggestedValue: String
)

/**
 * The five progression types the spec allows. Anything more aggressive
 * (e.g. "double the workload", "switch beginner to HARD") is forbidden
 * by the system prompt — the AI is constrained to this enum and the
 * parser drops anything it doesn't recognize.
 */
enum class ProgressionActionType {
    /** Slight duration bump — e.g. 10 min → 15 min. Most common. */
    INCREASE_DURATION,
    /** Slight frequency bump — e.g. 3x/week → 4x/week. */
    INCREASE_FREQUENCY,
    /** Next-level variation, still beginner-safe — walk → light jog,
     *  stretch → beginner yoga. Never beginner → HARD. */
    LEVEL_UP_VARIATION,
    /** Add a complementary supportive habit — meditation after journaling,
     *  hydration on workout days. Must fit the user's existing schedule. */
    ADD_COMPLEMENTARY_HABIT,
    /** Pure positive reinforcement — no behavior change requested.
     *  "You're doing great, keep this rhythm" surface. */
    CONSISTENCY_REWARD
}
