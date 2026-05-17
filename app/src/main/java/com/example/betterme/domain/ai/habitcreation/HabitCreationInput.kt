package com.example.betterme.domain.ai.habitcreation

/**
 * Input bundle the AI Habit Creation Assistant receives. Mirrors the prompt's
 * INPUT FORMAT section one-to-one; the runtime user-prompt builder serializes
 * this into the exact lines the model expects.
 *
 * [newHabit] is the habit the user is about to save (form state). Everything
 * else describes the *context* against which it should be evaluated — the
 * user's current load, recent completion rates, and lifestyle anchors.
 *
 * Defaults reflect what's actually tracked in BetterMe today:
 *  - `difficulty = "MEDIUM"` because `HabitEntity` doesn't yet store difficulty
 *  - `durationMinutes = 30` ditto
 *  - `frequency = "daily"` ditto
 * The translator (in the use case) is the one place to update once those
 * fields are wired through the Add Habit form.
 */
data class HabitCreationInput(
    val newHabit: NewHabit,
    val activeHabits: List<ExistingHabit>,
    /** Titles only — context that the user has tried these in the past. */
    val completedHabitTitles: List<String>,
    val archivedHabitTitles: List<String>,
    /** Per-habit completion rate over the last 14 days (0..100). */
    val recentCompletion: List<HabitCompletionRollup>,
    val lifestyle: LifestyleAnchors
)

data class NewHabit(
    val title: String,
    /** Localized Vietnamese category name (e.g. "Vận động & thể chất"). May be empty. */
    val categoryName: String,
    /** HH:mm 24-hour, or empty when the user hasn't picked a reminder. */
    val reminderTime: String,
    val durationMinutes: Int = 30,
    val difficulty: String = "MEDIUM",
    val frequency: String = "daily"
)

data class ExistingHabit(
    val title: String,
    val categoryName: String,
    val reminderTime: String,
    val durationMinutes: Int = 30,
    val difficulty: String = "MEDIUM",
    val frequency: String = "daily"
)

data class HabitCompletionRollup(
    val title: String,
    /** 0..100. Percentage of active days with a DONE log in the last 14 days. */
    val completionRate: Int
)

data class LifestyleAnchors(
    val sleepStart: String,
    val sleepEnd: String,
    val workStart: String,
    val workEnd: String
)
