package com.example.betterme.domain.ai.schedule

/**
 * Top-level result returned by the Schedule Conflict Analyzer.
 *
 * Shape matches the AI's JSON contract one-to-one (snake-case fields normalized
 * to camelCase by [kotlinx.serialization]'s field-name remap in the parser).
 * Enum fields use string discriminators that are pinned in the prompt, so an
 * unexpected value from a misbehaving free-tier model surfaces as a parse
 * failure rather than a silently-wrong analysis.
 *
 * [isCanned] is true when every OpenRouter model in the fallback chain failed
 * and the repo served a handwritten local analysis. Use cases inspect this
 * flag and skip caching canned results — next session's network attempt is
 * always free to produce a real analysis.
 */
data class ScheduleAnalysis(
    val hasConflict: Boolean,
    /** 0..100. ≥70 = healthy. <50 = needs adjustment soon. */
    val scheduleScore: Int,
    val energyLevel: EnergyLevel,
    val burnoutRisk: BurnoutRisk,
    /** 1-sentence Vietnamese summary. */
    val summary: String,
    /** 1-sentence Vietnamese positive observation; may be empty. */
    val positiveFeedback: String,
    val conflicts: List<ScheduleConflict>,
    val optimizedSchedule: List<OptimizedHabitTime>,
    val isCanned: Boolean = false
)

enum class EnergyLevel { LOW, MODERATE, HIGH }

enum class BurnoutRisk { LOW, MODERATE, HIGH }
