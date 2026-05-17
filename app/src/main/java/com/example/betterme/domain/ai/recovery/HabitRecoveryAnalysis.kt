package com.example.betterme.domain.ai.recovery

/**
 * Top-level result returned by the Adaptive Habit Recovery Engine.
 *
 * The engine is proactive coaching for users who are struggling — short
 * windows of low completion, growing miss streaks, late-night failures. When
 * no struggle signals fire, the use case short-circuits to an `shouldRecover
 * = false` baseline analysis WITHOUT calling OpenRouter (saves quota on
 * healthy users; the Home card simply doesn't mount).
 *
 * [isCanned] = true when every model in the fallback chain failed and the
 * repo served a deterministic local analysis derived from the same struggle
 * stats the prompt would have consumed. Use cases skip caching canned
 * content so the next session can produce a real coaching message.
 */
data class HabitRecoveryAnalysis(
    /** True when at least one trigger fired and the UI should surface the card. */
    val shouldRecover: Boolean,
    /** Trigger reasons that fired. Empty when [shouldRecover] = false. */
    val triggerReasons: List<RecoveryTrigger>,
    /** How aggressive the suggested reductions should be. */
    val overallTone: RecoveryIntensity,
    /** ≤ 2-sentence Vietnamese supportive coaching message. */
    val coachingMessage: String,
    /** Habits the engine specifically flags as struggling. Empty when no struggle. */
    val struggling: List<StrugglingHabit>,
    /** 1–4 recovery actions. Empty when [shouldRecover] = false. */
    val recoveryActions: List<HabitRecoveryAction>,
    val isCanned: Boolean = false
)

enum class RecoveryTrigger {
    LOW_COMPLETION,
    SKIP_STREAK,
    CONSECUTIVE_FAILS,
    HARD_HABIT_FAILING,
    LATE_NIGHT_FAILURES,
    TOO_MANY_HABITS,
    BURNOUT_RISK
}

enum class RecoveryIntensity {
    /** Minor tweaks — slight time shifts, small duration cuts. */
    LIGHT,
    /** Real reductions — fewer days, shorter sessions, easier variants. */
    MODERATE,
    /** Significant resets — pause habits, split into smaller ones, switch to recovery. */
    AGGRESSIVE
}
