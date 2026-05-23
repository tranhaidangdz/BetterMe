package com.example.betterme.domain.ai.progression

/**
 * Output of the Adaptive Smart Habit Progression Engine — the upbeat
 * mirror of [com.example.betterme.domain.ai.recovery.HabitRecoveryAnalysis].
 *
 * Mounts on Home only when [shouldProgress] is true. When false the
 * card is hidden entirely; the assistant never lectures a user who's
 * holding steady — silence is the default supportive stance.
 *
 * Note on tone: there is intentionally no AGGRESSIVE pace. The spec
 * forbids aggressive productivity pushes; the worst we ever go is
 * STEADY ("a clear step up, still gentle").
 */
data class HabitProgressionAnalysis(
    val shouldProgress: Boolean,
    val triggerReasons: List<ProgressionTrigger>,
    val overallPace: ProgressionPace,
    val coachingMessage: String,
    val vibrant: List<VibrantHabit>,
    val progressionActions: List<HabitProgressionAction>
)

/**
 * Why progression surfaced. Mirrors [com.example.betterme.domain.ai.recovery.RecoveryTrigger]
 * structure but inverts the direction — every reason is a *positive* signal.
 */
enum class ProgressionTrigger {
    /** ≥1 habit at completionRate14d >= 85. */
    HIGH_COMPLETION,
    /** Every habit has miss streak == 0 for the last cycle. */
    NO_MISS_STREAK,
    /** ≥1 habit with a 14+ day done-run. */
    STABLE_STREAK,
    /** activeHabitCount ≤ 6 — room to add a complementary habit safely. */
    HEADROOM_FOR_GROWTH,
    /** Recovery engine reports zero struggle triggers — explicit "clear to grow" signal. */
    NO_RECOVERY_NEEDED
}

/**
 * How forward the suggestions should lean. Capped at STEADY by spec —
 * progression must always feel light, never aggressive.
 */
enum class ProgressionPace {
    /** "+2-5 minutes", "1 extra day a week". Default for new high-performers. */
    GENTLE,
    /** "+50% duration cap", "next-level variation". For users sustained 21+ days. */
    STEADY
}
