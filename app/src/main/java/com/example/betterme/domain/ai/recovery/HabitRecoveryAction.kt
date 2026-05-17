package com.example.betterme.domain.ai.recovery

/**
 * One recovery action the engine recommends. Up to 4 per analysis.
 *
 * Semantics of each [type]:
 *
 *  - [RecoveryActionType.REDUCE_DIFFICULTY]   — easier variant of an existing habit
 *  - [RecoveryActionType.REDUCE_FREQUENCY]    — fewer days per week
 *  - [RecoveryActionType.REDUCE_DURATION]     — shorter session per occurrence
 *  - [RecoveryActionType.SWITCH_ALTERNATIVE]  — try a lighter habit altogether (e.g. walk vs run)
 *  - [RecoveryActionType.SPLIT_HABIT]         — break one habit into two smaller ones
 *  - [RecoveryActionType.ADD_RECOVERY_HABIT]  — insert a restorative habit (stretching, sleep, hydration)
 *  - [RecoveryActionType.PAUSE_TEMPORARILY]   — give a habit a planned break
 *  - [RecoveryActionType.CHANGE_TIME]         — move the reminder to a higher-energy window
 *
 * [targetHabit] is the title of the habit this action applies to. It's
 * nullable because actions like `ADD_RECOVERY_HABIT` aren't about an
 * existing habit at all — they propose a brand-new one. The UI shows the
 * target as a small chip when present.
 *
 * [suggestedValue] is an optional concrete value the AI proposes — e.g.
 * `"20 phút"` for REDUCE_DURATION or `"3 lần/tuần"` for REDUCE_FREQUENCY,
 * or `"07:00"` for CHANGE_TIME. The UI renders it as a separate pill.
 */
data class HabitRecoveryAction(
    val type: RecoveryActionType,
    val targetHabit: String?,
    /** ≤ 1-sentence Vietnamese title. */
    val title: String,
    /** ≤ 2-sentence Vietnamese explanation of why this helps. */
    val description: String,
    /** Optional concrete value (duration, frequency, time, etc.). May be empty. */
    val suggestedValue: String
)

enum class RecoveryActionType {
    REDUCE_DIFFICULTY,
    REDUCE_FREQUENCY,
    REDUCE_DURATION,
    SWITCH_ALTERNATIVE,
    SPLIT_HABIT,
    ADD_RECOVERY_HABIT,
    PAUSE_TEMPORARILY,
    CHANGE_TIME
}
