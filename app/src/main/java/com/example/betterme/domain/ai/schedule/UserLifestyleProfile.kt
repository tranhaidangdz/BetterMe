package com.example.betterme.domain.ai.schedule

/**
 * Lifestyle context the analyzer uses to reason about sleep recovery and
 * work-hour conflicts. BetterMe doesn't yet collect these from the user,
 * so the use case constructs a sensible default profile reflecting a typical
 * weekday (10pm–6am sleep, 9–17:30 work). When a future settings screen
 * captures real values, only the use case changes.
 *
 * Times are "HH:mm" 24-hour. Both ranges are wall-clock; sleep that crosses
 * midnight (start > end) is handled by the prompt's analysis rules.
 */
data class UserLifestyleProfile(
    val sleepStart: String = "22:00",
    val sleepEnd: String = "06:00",
    val workStart: String = "09:00",
    val workEnd: String = "17:30"
) {
    companion object {
        /** Default profile shipped while the lifestyle settings UI isn't built yet. */
        val Default = UserLifestyleProfile()
    }
}
