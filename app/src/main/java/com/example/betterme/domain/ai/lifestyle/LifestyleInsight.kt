package com.example.betterme.domain.ai.lifestyle

import com.example.betterme.domain.ai.schedule.BurnoutRisk

/**
 * Top-level result returned by the Adaptive Lifestyle Insight Engine — the
 * long-term coaching surface that reads the user's last 14 days of habit
 * behavior and proposes gentle, sustainable adjustments.
 *
 * Reuses [BurnoutRisk] from the schedule analyzer package. The two features
 * share the LOW / MODERATE / HIGH bucket exactly; introducing a parallel enum
 * would split a single concept across two files for no benefit.
 *
 * [isCanned] = true when every OpenRouter model in the fallback chain failed
 * and the repo served a deterministic local "stable baseline" insight. Use
 * cases skip caching canned results so the next session's network attempt is
 * free to produce real coaching.
 */
data class LifestyleInsight(
    val overallTrend: OverallTrend,
    val burnoutRisk: BurnoutRisk,
    /** 0..100. Higher = stronger habit consistency over the last 14 days. */
    val consistencyScore: Int,
    val energyPattern: EnergyPattern,
    /** 0..100. Higher = healthier sleep stability + recovery rhythm. */
    val recoveryScore: Int,
    /** 1-sentence Vietnamese supportive observation. */
    val primaryInsight: String,
    /** ≤ 2-sentence Vietnamese practical coaching message. */
    val coachingMessage: String,
    /** 1–4 items. Parser enforces the upper bound; canned fallback enforces
     *  the lower bound (always at least one suggestion). */
    val adaptiveSuggestions: List<AdaptiveSuggestion>,
    val isCanned: Boolean = false
)

enum class OverallTrend { IMPROVING, STABLE, DECLINING }

enum class EnergyPattern { MORNING_PEAK, AFTERNOON_PEAK, EVENING_PEAK, INCONSISTENT }
