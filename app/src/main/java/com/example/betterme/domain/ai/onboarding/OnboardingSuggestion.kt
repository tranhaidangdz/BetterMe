package com.example.betterme.domain.ai.onboarding

import com.example.betterme.domain.ai.schedule.EnergyLevel

/**
 * Top-level result returned by the AI onboarding suggester.
 *
 * - [summary] / [recommendedFocus] are 1-sentence Vietnamese strings (the
 *   prompt caps each).
 * - [energyProfile] reuses [EnergyLevel] from the schedule analyzer rather
 *   than introducing a parallel enum — the LOW/MODERATE/HIGH bucket is the
 *   same concept in both features.
 * - [habits] is 1–6 items. The parser drops malformed rows and applies the
 *   "max 6" cap, so the UI can render this list without further trimming.
 */
data class OnboardingSuggestion(
    val summary: String,
    val energyProfile: EnergyLevel,
    val recommendedFocus: String,
    val habits: List<OnboardingSuggestedHabit>
)
