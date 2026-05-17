package com.example.betterme.domain.ai.onboarding

/**
 * One AI-generated starter habit. Mirrors the prompt's `habits[]` element
 * shape one-to-one. All strings arrive in Vietnamese; enum-typed fields
 * (difficulty, priority, category) are parser-validated against pinned
 * enums so a misbehaving model can't leak an unexpected value into the UI.
 *
 * [reminderTime] is validated against the strict HH:mm regex in the parser
 * before reaching this model — by the time the UI sees a value, it's safe
 * to feed into `ScheduleHabitReminderUseCase`.
 *
 * [estimatedMinutes] is coerced to 1..120 at parse time so the field never
 * carries a runaway value (the prompt caps at 120; the parser enforces).
 */
data class OnboardingSuggestedHabit(
    val title: String,
    val emoji: String,
    val description: String,
    val category: HabitCategoryKey,
    val difficulty: Difficulty,
    val priority: Priority,
    val estimatedMinutes: Int,
    /** HH:mm 24-hour. */
    val reminderTime: String,
    val motivation: String
)

enum class Difficulty { EASY, MEDIUM, HARD }

enum class Priority { LOW, MEDIUM, HIGH }
