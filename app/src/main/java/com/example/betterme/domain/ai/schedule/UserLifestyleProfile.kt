package com.example.betterme.domain.ai.schedule

/**
 * The user's actual lifestyle context. When a future onboarding screen captures
 * real values, only this data class plus the call site in
 * `AnalyzeScheduleUseCase` need to change — every consumer reads from this one
 * source.
 *
 * Until then, [Default] provides the healthy baseline BetterMe assumes for
 * scheduling analysis: an 8-hour sleep window (23:00 → 07:00), a 9-hour weekday
 * (08:30 → 17:30), and three meals at typical Vietnamese local times. Defaults
 * lean *sustainable*, not "hyper-optimized productivity" — that's a deliberate
 * coaching stance the AI prompt also enforces.
 *
 * All times are 24-hour "HH:mm". Sleep that crosses midnight (start > end) is
 * handled by [com.example.betterme.data.ai.AiHabitInsightRepositoryImpl.sleepDurationMinutes].
 */
data class UserLifestyleProfile(
    val sleepStart: String = "23:00",
    val sleepEnd: String = "07:00",
    /** Coaching target. Used to flag short-sleep schedules, not enforced. */
    val sleepDurationTargetHours: Int = 8,

    val workStart: String = "08:30",
    val workEnd: String = "17:30",

    val breakfast: String = "07:30",
    val lunch: String = "12:00",
    val dinner: String = "18:30",

    /** EASY | MODERATE | INTENSE — drives habit-difficulty defaults during onboarding. */
    val activityLevel: String = "MODERATE"
) {
    companion object {
        /**
         * Healthy baseline used everywhere the user hasn't provided lifestyle
         * data yet. Aligned with BetterMe's coaching philosophy:
         *
         *  - Consistency over intensity.
         *  - Sustainable routines, slowly built.
         *  - No 4 AM productivity-culture defaults.
         */
        val Default = UserLifestyleProfile()
    }
}
