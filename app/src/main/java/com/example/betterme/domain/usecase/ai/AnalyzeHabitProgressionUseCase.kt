package com.example.betterme.domain.usecase.ai

import android.util.Log
import com.example.betterme.data.local.datastore.DataStoreManager
import com.example.betterme.domain.ai.AiCacheRepository
import com.example.betterme.domain.ai.AiCacheRepository.Companion.TTL_MS_SCHEDULE
import com.example.betterme.domain.ai.AiCacheRepository.Companion.TYPE_HABIT_PROGRESSION
import com.example.betterme.domain.ai.AiHabitInsightRepository
import com.example.betterme.domain.ai.progression.HabitProgressionAnalysis
import com.example.betterme.domain.ai.progression.HabitProgressionInput
import com.example.betterme.domain.ai.progression.ProgressionLifestyleAnchors
import com.example.betterme.domain.ai.progression.ProgressionPace
import com.example.betterme.domain.ai.progression.ProgressionStatRow
import com.example.betterme.domain.ai.progression.ProgressionTrigger
import com.example.betterme.domain.ai.schedule.HealthyDefaults
import com.example.betterme.domain.ai.schedule.UserLifestyleProfile
import com.example.betterme.domain.repository.HabitLogRepository
import com.example.betterme.domain.repository.HabitRepository
import com.example.betterme.utils.DateUtils
import kotlinx.coroutines.flow.first
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Drives the Smart Habit Progression Engine — the upbeat mirror of
 * [AnalyzeHabitRecoveryUseCase].
 *
 *  1. Pulls active habits + computes per-habit success stats (7d / 14d
 *     completion rate, current streak).
 *  2. Runs deterministic GATES *against progression*. Progression only
 *     fires when ALL of these hold:
 *       - Every active habit has 14d completion ≥ 60% (no struggle anywhere).
 *       - At least one habit has 14d completion ≥ 85% (the vibrant set).
 *       - activeHabitCount ≤ 8 (not overloaded).
 *       - hardHabitCount ≤ 1 (never push to ≥2 HARD).
 *       - No recovery triggers fire (mutual exclusion with Recovery Engine).
 *     If any gate fails → short-circuit to baseline `shouldProgress = false`
 *     WITHOUT calling Gemini.
 *  3. Cache-first read keyed by the fingerprint of progression signals.
 *     24h TTL. When all AI models fail the repo throws
 *     [com.example.betterme.domain.ai.AiUnavailableException] — the VM
 *     surfaces a retry-able error state and nothing is cached.
 *
 * Progression actions in this iteration are all advisory: HabitEntity
 * doesn't store duration / frequency / difficulty, so the UI shows a
 * "Tham khảo" hint rather than a real Apply CTA. [applyAction] exists for
 * API symmetry with [AnalyzeHabitRecoveryUseCase.applyAction] but always
 * returns false today — no materialization path is honest yet.
 */
class AnalyzeHabitProgressionUseCase(
    private val dataStoreManager: DataStoreManager,
    private val habitRepository: HabitRepository,
    private val habitLogRepository: HabitLogRepository,
    private val aiRepository: AiHabitInsightRepository,
    private val cache: AiCacheRepository
) {

    suspend operator fun invoke(
        lifestyle: UserLifestyleProfile? = null,
        forceRefresh: Boolean = false
    ): HabitProgressionAnalysis {
        val userId = dataStoreManager.getCurrentUserId().first().orEmpty()
        if (userId.isBlank()) return baselineNoProgress()
        val effectiveLifestyle = lifestyle ?: UserLifestyleProfile.Default

        val todayMs = DateUtils.startOfDay()
        val allHabits = habitRepository.getHabits(userId).first()
        val activeHabits = allHabits.filter { it.end_date == null || it.end_date >= todayMs }
        if (activeHabits.isEmpty()) return baselineNoProgress()

        // Per-habit success stats.
        val stats = activeHabits.map { habit ->
            val logs = habitLogRepository.getLogs(habit.id).first()
            val daysSinceStart = (((todayMs - habit.start_date) / DAY_MS) + 1)
                .toInt().coerceAtLeast(1)

            val rate7 = completionInWindow(logs, todayMs, 7, daysSinceStart, offsetDays = 0)
            val rate14 = completionInWindow(logs, todayMs, 14, daysSinceStart, offsetDays = 0)
            // Days 7-14 ago — "last week" window for the prompt's
            // week-over-week trend signal.
            val prevWeek = completionInWindow(
                logs, todayMs, windowDays = 7, daysSinceStart = daysSinceStart, offsetDays = 7
            )
            val streak = computeCurrentStreak(logs, todayMs)
            ProgressionStatRow(
                title = habit.title,
                reminderTime = habit.reminder_time.orEmpty(),
                difficulty = "MEDIUM", // until HabitEntity carries difficulty
                completionRate7d = rate7,
                completionRate14d = rate14,
                previousWeekCompletionRate = prevWeek,
                currentStreak = streak
            )
        }

        // ── Gate 1: no struggle anywhere ────────────────────────────
        // If ANY habit is below 60% over 14 days, the user is in
        // recovery territory, not progression territory.
        val anyStruggling = stats.any { it.completionRate14d < STRUGGLE_FLOOR }
        if (anyStruggling) return baselineNoProgress()

        // ── Gate 2: at least one vibrant habit ──────────────────────
        val vibrant = stats.filter { it.completionRate14d >= VIBRANT_THRESHOLD }
        if (vibrant.isEmpty()) return baselineNoProgress()

        // ── Gate 3: not overloaded ──────────────────────────────────
        if (activeHabits.size > MAX_ACTIVE_FOR_GROWTH) return baselineNoProgress()

        // ── Gate 4: HARD count cap ──────────────────────────────────
        val hardCount = stats.count { it.difficulty == "HARD" }
        if (hardCount > MAX_HARD_ALLOWED) return baselineNoProgress()

        // ── Gate 5: mutual exclusion with Recovery ──────────────────
        // Recovery uses miss streak ≥ 3 / completion < 40 / >8 habits /
        // late-night failures / HARD failing. We've already excluded the
        // bottom signals above; this catches late-night failures explicitly.
        val lateNightFailing = stats.any { row ->
            val hour = row.reminderTime.split(":").firstOrNull()?.toIntOrNull() ?: -1
            hour >= HealthyDefaults.HARD_HABIT_LATEST_HOUR && row.completionRate14d < 60
        }
        if (lateNightFailing) return baselineNoProgress()

        // Build progression triggers.
        val triggers = mutableSetOf<ProgressionTrigger>()
        triggers += ProgressionTrigger.HIGH_COMPLETION
        triggers += ProgressionTrigger.NO_RECOVERY_NEEDED
        if (vibrant.any { it.currentStreak >= STABLE_STREAK_DAYS }) {
            triggers += ProgressionTrigger.STABLE_STREAK
        }
        if (stats.all { it.currentStreak > 0 || it.completionRate7d >= 80 }) {
            triggers += ProgressionTrigger.NO_MISS_STREAK
        }
        if (activeHabits.size <= HEADROOM_THRESHOLD) {
            triggers += ProgressionTrigger.HEADROOM_FOR_GROWTH
        }

        val input = HabitProgressionInput(
            allHabitStats = stats,
            vibrantTitles = vibrant.map { it.title },
            detectedTriggers = triggers.toList(),
            activeHabitCount = activeHabits.size,
            hardHabitCount = hardCount,
            lifestyle = ProgressionLifestyleAnchors(
                sleepStart = effectiveLifestyle.sleepStart,
                sleepEnd = effectiveLifestyle.sleepEnd,
                workStart = effectiveLifestyle.workStart,
                workEnd = effectiveLifestyle.workEnd
            )
        )

        val cacheKey = fingerprint(input).hashCode()
        if (!forceRefresh) {
            cache.getFresh(cacheKey, TYPE_HABIT_PROGRESSION, TTL_MS_SCHEDULE)?.let { cachedJson ->
                decode(cachedJson)?.let { return it }
            }
        }

        val result = aiRepository.analyzeHabitProgression(input)
        cache.save(cacheKey, TYPE_HABIT_PROGRESSION, encode(result))
        return result
    }

    /**
     * Apply path is intentionally a no-op in this iteration. HabitEntity
     * doesn't carry duration / frequency / difficulty fields today, so
     * none of the progression action types can be materialized honestly.
     * The card surfaces "Tham khảo" instead of an Apply CTA — see
     * [com.example.betterme.presentation.home.progression.HabitProgressionAssistantCard].
     */
    @Suppress("UNUSED_PARAMETER")
    suspend fun applyAction(action: com.example.betterme.domain.ai.progression.HabitProgressionAction): Boolean = false

    /**
     * Completion rate inside a [windowDays] window ending at
     * `today - offsetDays`. [offsetDays] = 0 covers the most recent
     * [windowDays] days; [offsetDays] = 7 with windowDays = 7 covers
     * days 7-14 ago (the prior week) for week-over-week deltas. Returns
     * 0 when the window predates the habit's start so we don't slander
     * fresh habits with a misleading "0% last week".
     */
    private fun completionInWindow(
        logs: List<com.example.betterme.data.local.room.entities.HabitLogEntity>,
        todayMs: Long,
        windowDays: Int,
        daysSinceStart: Int,
        offsetDays: Int = 0
    ): Int {
        if (daysSinceStart <= offsetDays) return 0
        val windowEnd = todayMs - offsetDays * DAY_MS
        val windowStart = windowEnd - windowDays * DAY_MS
        val done = logs.count { it.status == "DONE" && it.date in windowStart..windowEnd }
        val denom = minOf(windowDays, daysSinceStart - offsetDays).coerceAtLeast(1)
        return ((done.toFloat() / denom.toFloat()) * 100f).toInt().coerceIn(0, 100)
    }

    /**
     * Consecutive DONE days ending at today (inclusive). Uses
     * [DateUtils.currentStreak] semantics: today counts when it's DONE;
     * otherwise the streak ends at yesterday. Capped at 60 days for the
     * prompt input.
     */
    private fun computeCurrentStreak(
        logs: List<com.example.betterme.data.local.room.entities.HabitLogEntity>,
        todayMs: Long
    ): Int {
        val doneDays = logs.filter { it.status == "DONE" }
            .map { DateUtils.startOfDay(it.date) }
            .toSet()
        var cursor = todayMs
        var streak = 0
        // Today may not be done yet — that's OK, count back from yesterday.
        if (cursor !in doneDays) cursor -= DAY_MS
        while (streak < 60) {
            if (cursor !in doneDays) break
            streak++
            cursor -= DAY_MS
        }
        return streak
    }

    /**
     * Stable fingerprint of progression-relevant inputs. Changing day
     * doesn't bust the cache; changing stats or lifestyle does.
     */
    private fun fingerprint(input: HabitProgressionInput): String = buildString {
        input.allHabitStats.sortedBy { it.title }.forEach { row ->
            append(
                "${row.title}@${row.completionRate7d}/${row.completionRate14d}" +
                    "/p${row.previousWeekCompletionRate}/s${row.currentStreak};"
            )
        }
        append("||t:")
        append(input.detectedTriggers.sortedBy { it.name }.joinToString(",") { it.name })
        append("||hc:${input.hardHabitCount}")
        append("||ac:${input.activeHabitCount}")
        append("||sl:${input.lifestyle.sleepStart}-${input.lifestyle.sleepEnd}")
        append("||wk:${input.lifestyle.workStart}-${input.lifestyle.workEnd}")
    }

    private fun baselineNoProgress(): HabitProgressionAnalysis = HabitProgressionAnalysis(
        shouldProgress = false,
        triggerReasons = emptyList(),
        overallPace = ProgressionPace.GENTLE,
        coachingMessage = "",
        vibrant = emptyList(),
        progressionActions = emptyList(),
    )

    private fun encode(a: HabitProgressionAnalysis): String =
        json.encodeToString(HabitProgressionCacheDto.fromDomain(a))

    private fun decode(raw: String): HabitProgressionAnalysis? = try {
        json.decodeFromString<HabitProgressionCacheDto>(raw).toDomain()
    } catch (e: Exception) {
        Log.w("AnalyzeProgression", "Failed to decode cached progression", e)
        null
    }

    // ============================================================
    // Cache wire format
    // ============================================================
    @Serializable
    private data class HabitProgressionCacheDto(
        val shouldProgress: Boolean,
        val triggerReasons: List<String>,
        val overallPace: String,
        val coachingMessage: String,
        val vibrant: List<VibrantCacheDto>,
        val progressionActions: List<ActionCacheDto>
    ) {
        fun toDomain(): HabitProgressionAnalysis = HabitProgressionAnalysis(
            shouldProgress = shouldProgress,
            triggerReasons = triggerReasons.mapNotNull {
                runCatching { ProgressionTrigger.valueOf(it) }.getOrNull()
            },
            overallPace = runCatching { ProgressionPace.valueOf(overallPace) }
                .getOrDefault(ProgressionPace.GENTLE),
            coachingMessage = coachingMessage,
            vibrant = vibrant.map {
                com.example.betterme.domain.ai.progression.VibrantHabit(
                    title = it.title,
                    completionRate7d = it.completionRate7d,
                    completionRate14d = it.completionRate14d,
                    currentStreak = it.currentStreak,
                    readinessReason = it.readinessReason
                )
            },
            progressionActions = progressionActions.mapNotNull { a ->
                val type = runCatching {
                    com.example.betterme.domain.ai.progression.ProgressionActionType.valueOf(a.type)
                }.getOrNull() ?: return@mapNotNull null
                com.example.betterme.domain.ai.progression.HabitProgressionAction(
                    type = type,
                    targetHabit = a.targetHabit,
                    title = a.title,
                    description = a.description,
                    suggestedValue = a.suggestedValue
                )
            },
        )

        companion object {
            fun fromDomain(a: HabitProgressionAnalysis) = HabitProgressionCacheDto(
                shouldProgress = a.shouldProgress,
                triggerReasons = a.triggerReasons.map { it.name },
                overallPace = a.overallPace.name,
                coachingMessage = a.coachingMessage,
                vibrant = a.vibrant.map {
                    VibrantCacheDto(it.title, it.completionRate7d, it.completionRate14d, it.currentStreak, it.readinessReason)
                },
                progressionActions = a.progressionActions.map {
                    ActionCacheDto(it.type.name, it.targetHabit, it.title, it.description, it.suggestedValue)
                }
            )
        }
    }

    @Serializable
    private data class VibrantCacheDto(
        val title: String,
        val completionRate7d: Int,
        val completionRate14d: Int,
        val currentStreak: Int,
        val readinessReason: String
    )

    @Serializable
    private data class ActionCacheDto(
        val type: String,
        val targetHabit: String?,
        val title: String,
        val description: String,
        val suggestedValue: String
    )

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    private companion object {
        const val DAY_MS: Long = 24L * 60L * 60L * 1000L

        /** Below 60% means struggle territory — progression is silent. */
        const val STRUGGLE_FLOOR = 60

        /** ≥85% over 14 days = vibrant. From the spec. */
        const val VIBRANT_THRESHOLD = 85

        /** Spec rule: never progress when overloaded (>8 active). */
        const val MAX_ACTIVE_FOR_GROWTH = 8

        /** Spec rule: never push past 1 HARD habit. */
        const val MAX_HARD_ALLOWED = 1

        /** 14-day stretch counts as a "stable streak" trigger. */
        const val STABLE_STREAK_DAYS = 14

        /** ≤ 6 active habits = headroom to safely add a complementary one. */
        const val HEADROOM_THRESHOLD = 6
    }
}
