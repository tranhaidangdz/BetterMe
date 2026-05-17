package com.example.betterme.domain.ai.schedule

/**
 * App-wide healthy baseline assumptions used by:
 *
 * - The AI schedule analyzer (passed into the runtime user prompt so the model
 *   can reason about *recommended* habit windows instead of inventing them).
 * - Future onboarding screens (to seed sensible suggestions when the user
 *   hasn't filled in lifestyle data yet).
 *
 * These are intentionally *not* on [UserLifestyleProfile] because they're not
 * user-specific — they're universal recommendations BetterMe ships. The user's
 * own actual lifestyle (sleep/wake/meals) lives on [UserLifestyleProfile].
 *
 * Coaching stance: consistency over intensity, no productivity-culture defaults,
 * no guilt-based language, no 4 AM wake-ups. The AI prompt is told to follow
 * the same stance so these constants and the model's output stay aligned.
 */
object HealthyDefaults {

    // ─── Energy curves (used to advise WHEN to schedule what) ─────
    /** Highest mental energy. Best for HARD focus tasks. */
    const val MORNING_PEAK = "07:00-11:00"

    /** Moderate energy. Good for routine focus work. */
    const val AFTERNOON_PEAK = "13:00-17:00"

    /** Reduced energy. Reserve for light wellness habits. */
    const val LATE_EVENING = "21:00+"

    // ─── Recommended habit windows ────────────────────────────────
    const val WINDOW_WORKOUT = "06:30-08:00 or 17:00-19:00"
    const val WINDOW_MEDITATION = "06:00-08:00 or 20:00-22:00"
    const val WINDOW_READING = "20:00-22:00"
    const val WINDOW_DEEP_WORK = "08:00-11:00 or 14:00-17:00"
    const val WINDOW_WALKING = "12:00-18:00"
    const val WINDOW_JOURNALING = "20:00-22:30"
    const val WINDOW_HYDRATION = "every 2-3 hours"
    const val WINDOW_SLEEP_PREP = "after 21:00 only"

    // ─── Scheduling rules ─────────────────────────────────────────
    /** Hour-of-day after which HARD habits should generally be avoided. */
    const val HARD_HABIT_LATEST_HOUR = 21

    /** Hour-cooldown between bed time and the last intense workout. */
    const val EXERCISE_BEFORE_SLEEP_BUFFER_HOURS = 2

    /** Minimum minutes between consecutive HARD habits to avoid burnout. */
    const val HARD_HABIT_MIN_GAP_MINUTES = 15

    /** Maximum habit count packed inside any 90-minute window. */
    const val MAX_HABITS_PER_90_MINUTES = 3

    /** Max number of starting habits surfaced during onboarding. */
    const val MAX_ONBOARDING_HABITS = 6

    /**
     * Compact one-line summary suitable for embedding in an AI runtime prompt.
     * The model reads it verbatim — keep it short and concrete so it doesn't
     * eat into the response token budget.
     */
    val PROMPT_HINT: String = """
        Healthy reference windows: Workout $WINDOW_WORKOUT;
        Meditation $WINDOW_MEDITATION; Reading $WINDOW_READING;
        Deep work $WINDOW_DEEP_WORK; Walking $WINDOW_WALKING;
        Journaling $WINDOW_JOURNALING; Hydration $WINDOW_HYDRATION;
        Sleep-prep $WINDOW_SLEEP_PREP.
        Energy: morning $MORNING_PEAK high, afternoon $AFTERNOON_PEAK moderate, after 21:00 reduced.
        Avoid HARD habits after $HARD_HABIT_LATEST_HOUR:00 and inside $EXERCISE_BEFORE_SLEEP_BUFFER_HOURS h before sleep.
    """.trimIndent().replace("\n", " ")
}
