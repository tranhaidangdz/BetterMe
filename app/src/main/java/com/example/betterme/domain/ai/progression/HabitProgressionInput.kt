package com.example.betterme.domain.ai.progression

/**
 * Input bundle the progression engine receives. Built by
 * [com.example.betterme.domain.usecase.ai.AnalyzeHabitProgressionUseCase]
 * after deterministic gates pass (every habit ≥ 60%, ≥1 at ≥ 85%, no
 * recovery triggers, headroom).
 *
 * Mirror of [com.example.betterme.domain.ai.recovery.HabitRecoveryInput]
 * with two intentional differences:
 *
 *  1. Uses its own [ProgressionStatRow] shape rather than reusing the
 *     recovery package's `HabitStatRow` — cross-feature DTO reuse is
 *     the kind of coupling that becomes painful when one side wants
 *     to add a field. Duplication is the cheaper price.
 *  2. Carries [vibrantTitles] (well-performing habits) instead of
 *     `strugglingTitles` — same purpose, opposite signal.
 */
data class HabitProgressionInput(
    val allHabitStats: List<ProgressionStatRow>,
    /** Habits at completionRate14d >= 85 — the focus set for AI suggestions. */
    val vibrantTitles: List<String>,
    val detectedTriggers: List<ProgressionTrigger>,
    val activeHabitCount: Int,
    /** Number of HARD-difficulty habits already active. Caps suggestions
     *  — the model is told never to push to HARD if this is already >= 1. */
    val hardHabitCount: Int,
    val lifestyle: ProgressionLifestyleAnchors
)

data class ProgressionStatRow(
    val title: String,
    /** "HH:mm" or empty when no reminder is set. */
    val reminderTime: String,
    /** EASY | MEDIUM | HARD — defaulted to MEDIUM until HabitEntity stores difficulty. */
    val difficulty: String,
    val completionRate7d: Int,
    val completionRate14d: Int,
    /** Current consecutive-done streak in days. */
    val currentStreak: Int
)

/**
 * Sleep / work anchors fed into the prompt so the model can avoid
 * suggesting times that collide with the user's protected windows.
 */
data class ProgressionLifestyleAnchors(
    val sleepStart: String,
    val sleepEnd: String,
    val workStart: String,
    val workEnd: String
)
