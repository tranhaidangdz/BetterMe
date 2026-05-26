package com.example.betterme.domain.usecase.ai

import android.util.Log
import com.example.betterme.data.local.datastore.DataStoreManager
import com.example.betterme.domain.ai.AiCacheRepository
import com.example.betterme.domain.ai.AiCacheRepository.Companion.TTL_MS_SCHEDULE
import com.example.betterme.domain.ai.AiCacheRepository.Companion.TYPE_HABIT_RECOVERY
import com.example.betterme.domain.ai.AiHabitInsightRepository
import com.example.betterme.domain.ai.recovery.HabitRecoveryAction
import com.example.betterme.domain.ai.recovery.HabitRecoveryAnalysis
import com.example.betterme.domain.ai.recovery.HabitRecoveryInput
import com.example.betterme.domain.ai.recovery.HabitStatRow
import com.example.betterme.domain.ai.recovery.LifestyleAnchors
import com.example.betterme.domain.ai.recovery.RecoveryActionType
import com.example.betterme.domain.ai.recovery.RecoveryIntensity
import com.example.betterme.domain.ai.recovery.RecoveryTrigger
import com.example.betterme.domain.ai.recovery.StrugglingHabit
import com.example.betterme.domain.ai.schedule.HealthyDefaults
import com.example.betterme.domain.ai.schedule.UserLifestyleProfile
import com.example.betterme.domain.repository.HabitLogRepository
import com.example.betterme.domain.repository.HabitRepository
import com.example.betterme.domain.usecase.habit.ScheduleHabitReminderUseCase
import com.example.betterme.utils.DateUtils
import kotlinx.coroutines.flow.first
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.concurrent.TimeUnit

/**
 * Drives the Adaptive Habit Recovery Engine.
 *
 *  1. Pulls active habits + computes per-habit struggle stats (7d / 14d
 *     completion rate, current miss streak).
 *  2. Runs deterministic trigger detection against those stats (the same
 *     thresholds the spec lists: 14d < 40%, miss streak ≥ 3, HARD failing,
 *     late-night failures, > 8 active habits). When NO trigger fires,
 *     short-circuits to a baseline analysis WITHOUT calling Gemini —
 *     healthy users never burn quota.
 *  3. Cache-first read keyed by the fingerprint of struggle signals (which
 *     habits, which triggers, which stats). 24h TTL. Skips cache.save when
 *     the repo serves a canned analysis so connectivity recovery isn't
 *     blocked.
 *
 * [applyAction] materializes a [HabitRecoveryAction] into the data layer
 * where possible:
 *   - CHANGE_TIME       → updates the target habit's `reminder_time` and
 *                          re-arms the alarm.
 *   - PAUSE_TEMPORARILY → sets the habit's `end_date` to today, effectively
 *                          ending the journey.
 *   - others            → no-op at the data layer; UI keeps the suggestion
 *                          visible as advice.
 */
class AnalyzeHabitRecoveryUseCase(
    private val dataStoreManager: DataStoreManager,
    private val habitRepository: HabitRepository,
    private val habitLogRepository: HabitLogRepository,
    private val aiRepository: AiHabitInsightRepository,
    private val cache: AiCacheRepository,
    private val scheduleHabitReminder: ScheduleHabitReminderUseCase
) {

    suspend operator fun invoke(
        lifestyle: UserLifestyleProfile? = null,
        burnoutRiskHigh: Boolean = false,
        forceRefresh: Boolean = false
    ): HabitRecoveryAnalysis {
        val userId = dataStoreManager.getCurrentUserId().first().orEmpty()
        if (userId.isBlank()) return baselineNoRecovery()
        val effectiveLifestyle = lifestyle ?: UserLifestyleProfile.Default

        val todayMs = DateUtils.startOfDay()
        val allHabits = habitRepository.getHabits(userId).first()
        val activeHabits = allHabits.filter { it.end_date == null || it.end_date >= todayMs }
        if (activeHabits.isEmpty()) return baselineNoRecovery()

        // Per-habit struggle stats.
        val stats = activeHabits.map { habit ->
            val logs = habitLogRepository.getLogs(habit.id).first()
            val daysSinceStart = (((todayMs - habit.start_date) / DAY_MS) + 1)
                .toInt().coerceAtLeast(1)

            val rate7 = completionInWindow(logs, todayMs, 7, daysSinceStart, offsetDays = 0)
            val rate14 = completionInWindow(logs, todayMs, 14, daysSinceStart, offsetDays = 0)
            // "Last week" = days 7-14 ago. Used for the week-over-week delta
            // signal in the prompt — see RECOVERY_SYSTEM_PROMPT.
            val prevWeek = completionInWindow(
                logs, todayMs, windowDays = 7, daysSinceStart = daysSinceStart, offsetDays = 7
            )
            val missStreak = computeMissStreak(logs, todayMs)
            HabitStatRow(
                title = habit.title,
                reminderTime = habit.reminder_time.orEmpty(),
                difficulty = "MEDIUM", // until HabitEntity carries difficulty
                completionRate7d = rate7,
                completionRate14d = rate14,
                previousWeekCompletionRate = prevWeek,
                missStreak = missStreak
            )
        }

        // Trigger detection — deterministic, matches the spec's thresholds.
        val triggers = mutableSetOf<RecoveryTrigger>()
        val strugglingTitles = mutableSetOf<String>()

        stats.forEach { row ->
            if (row.completionRate14d < 40) {
                triggers += RecoveryTrigger.LOW_COMPLETION
                strugglingTitles += row.title
            }
            if (row.missStreak >= 3) {
                triggers += RecoveryTrigger.SKIP_STREAK
                strugglingTitles += row.title
            }
            if (row.missStreak >= 4) {
                triggers += RecoveryTrigger.CONSECUTIVE_FAILS
                strugglingTitles += row.title
            }
            if (row.difficulty == "HARD" && row.completionRate14d < 50) {
                triggers += RecoveryTrigger.HARD_HABIT_FAILING
                strugglingTitles += row.title
            }
        }
        val lateNight = stats.filter { row ->
            val hour = row.reminderTime.split(":").firstOrNull()?.toIntOrNull() ?: -1
            hour >= HealthyDefaults.HARD_HABIT_LATEST_HOUR
        }
        if (lateNight.any { it.completionRate14d < 60 }) {
            triggers += RecoveryTrigger.LATE_NIGHT_FAILURES
            strugglingTitles += lateNight.filter { it.completionRate14d < 60 }.map { it.title }
        }
        if (activeHabits.size > 8) {
            triggers += RecoveryTrigger.TOO_MANY_HABITS
        }
        if (burnoutRiskHigh) {
            triggers += RecoveryTrigger.BURNOUT_RISK
        }

        if (triggers.isEmpty()) {
            // Healthy user — no struggle signals. Don't burn AI tokens.
            return baselineNoRecovery()
        }

        val input = HabitRecoveryInput(
            allHabitStats = stats,
            strugglingTitles = strugglingTitles.toList(),
            detectedTriggers = triggers.toList(),
            activeHabitCount = activeHabits.size,
            lateNightHabitTitles = lateNight.map { it.title },
            lifestyle = LifestyleAnchors(
                sleepStart = effectiveLifestyle.sleepStart,
                sleepEnd = effectiveLifestyle.sleepEnd,
                workStart = effectiveLifestyle.workStart,
                workEnd = effectiveLifestyle.workEnd
            )
        )

        val cacheKey = fingerprint(input).hashCode()
        if (!forceRefresh) {
            cache.getFresh(cacheKey, TYPE_HABIT_RECOVERY, TTL_MS_SCHEDULE)?.let { cachedJson ->
                decode(cachedJson)?.let { return it }
            }
        }

        val result = aiRepository.analyzeHabitRecovery(input)
        cache.save(cacheKey, TYPE_HABIT_RECOVERY, encode(result))
        return result
    }

    /**
     * Materialize a recovery action into the data layer where possible.
     * Returns true when an actual mutation landed (UI flips the card to
     * "Đã áp dụng"); false when the action is advisory-only and the user
     * needs to act manually elsewhere.
     */
    suspend fun applyAction(action: HabitRecoveryAction): Boolean {
        val userId = dataStoreManager.getCurrentUserId().first().orEmpty()
        if (userId.isBlank()) return false
        val targetTitle = action.targetHabit?.trim() ?: return false
        val habits = habitRepository.getHabits(userId).first()
        val target = habits.firstOrNull { it.title.trim() == targetTitle } ?: return false

        return when (action.type) {
            RecoveryActionType.CHANGE_TIME -> {
                val newTime = action.suggestedValue.takeIf { it.matches(HHMM_REGEX) }
                    ?: return false
                if (target.reminder_time == newTime) return false
                habitRepository.updateHabit(target.copy(reminder_time = newTime))
                scheduleHabitReminder(
                    habitId = target.id,
                    habitTitle = target.title,
                    reminderTime = newTime
                )
                true
            }
            RecoveryActionType.PAUSE_TEMPORARILY -> {
                val todayStart = DateUtils.startOfDay()
                if (target.end_date != null && target.end_date <= todayStart) return false
                habitRepository.updateHabit(target.copy(end_date = todayStart))
                true
            }
            // Other action types (REDUCE_DIFFICULTY / FREQUENCY / DURATION,
            // SWITCH / SPLIT / ADD_RECOVERY) don't have backing entity fields
            // in BetterMe today — they remain advisory until those fields
            // land in HabitEntity. UI keeps the card visible but the "Đã áp
            // dụng" success state won't flip.
            else -> false
        }
    }

    /**
     * Completion rate inside a [windowDays] window ending today, capped by
     * the habit's actual age so a 3-day-old habit isn't judged against 14.
     */
    /**
     * Completion rate inside a [windowDays] window ending at `today - offsetDays`,
     * capped by the habit's actual age so a 3-day-old habit isn't judged
     * against 14. [offsetDays] = 0 means "the most recent windowDays days";
     * [offsetDays] = 7 with windowDays = 7 means "the seven days BEFORE the
     * last seven days" — i.e. the prior week for week-over-week deltas.
     *
     * Returns 0 when the offset window is older than the habit's start —
     * a freshly-created habit doesn't have a meaningful "last week".
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
     * Consecutive days without a DONE log ending at the last full day. If
     * the user already checked in today, miss streak is 0 (we don't count
     * today against them). Capped at 14 days for the prompt input.
     */
    private fun computeMissStreak(
        logs: List<com.example.betterme.data.local.room.entities.HabitLogEntity>,
        todayMs: Long
    ): Int {
        val doneDays = logs.filter { it.status == "DONE" }.map { it.date }.toSet()
        var cursor = todayMs
        // If today is already done, miss streak is 0 — don't count today.
        if (cursor in doneDays) return 0
        var streak = 0
        for (i in 1..14) {
            cursor -= DAY_MS
            if (cursor in doneDays) break
            streak++
        }
        return streak
    }

    /**
     * Stable fingerprint of the trigger inputs. Anything that could change
     * the analysis (stats, triggers, lifestyle) is in the hash; ephemeral
     * things (today's date) are not, so the cache survives normal daily
     * check-ins.
     */
    private fun fingerprint(input: HabitRecoveryInput): String = buildString {
        input.allHabitStats.sortedBy { it.title }.forEach { row ->
            append(
                "${row.title}@${row.completionRate7d}/${row.completionRate14d}" +
                    "/p${row.previousWeekCompletionRate}/m${row.missStreak};"
            )
        }
        append("||t:")
        append(input.detectedTriggers.sortedBy { it.name }.joinToString(",") { it.name })
        append("||sl:${input.lifestyle.sleepStart}-${input.lifestyle.sleepEnd}")
        append("||wk:${input.lifestyle.workStart}-${input.lifestyle.workEnd}")
    }

    private fun baselineNoRecovery(): HabitRecoveryAnalysis = HabitRecoveryAnalysis(
        shouldRecover = false,
        triggerReasons = emptyList(),
        overallTone = RecoveryIntensity.LIGHT,
        coachingMessage = "",
        struggling = emptyList(),
        recoveryActions = emptyList(),
    )

    private fun encode(a: HabitRecoveryAnalysis): String =
        json.encodeToString(HabitRecoveryCacheDto.fromDomain(a))

    private fun decode(raw: String): HabitRecoveryAnalysis? = try {
        json.decodeFromString<HabitRecoveryCacheDto>(raw).toDomain()
    } catch (e: Exception) {
        Log.w("AnalyzeRecovery", "Failed to decode cached recovery", e)
        null
    }

    // ============================================================
    // Cache wire format
    // ============================================================
    @Serializable
    private data class HabitRecoveryCacheDto(
        val shouldRecover: Boolean,
        val triggerReasons: List<String>,
        val overallTone: String,
        val coachingMessage: String,
        val struggling: List<StrugglingCacheDto>,
        val recoveryActions: List<ActionCacheDto>
    ) {
        fun toDomain(): HabitRecoveryAnalysis = HabitRecoveryAnalysis(
            shouldRecover = shouldRecover,
            triggerReasons = triggerReasons.mapNotNull {
                runCatching { RecoveryTrigger.valueOf(it) }.getOrNull()
            },
            overallTone = runCatching { RecoveryIntensity.valueOf(overallTone) }
                .getOrDefault(RecoveryIntensity.LIGHT),
            coachingMessage = coachingMessage,
            struggling = struggling.map {
                StrugglingHabit(
                    title = it.title,
                    completionRate7d = it.completionRate7d,
                    completionRate14d = it.completionRate14d,
                    missStreak = it.missStreak,
                    recoveryReason = it.recoveryReason
                )
            },
            recoveryActions = recoveryActions.mapNotNull { a ->
                val type = runCatching { RecoveryActionType.valueOf(a.type) }.getOrNull()
                    ?: return@mapNotNull null
                HabitRecoveryAction(
                    type = type,
                    targetHabit = a.targetHabit,
                    title = a.title,
                    description = a.description,
                    suggestedValue = a.suggestedValue
                )
            },
        )

        companion object {
            fun fromDomain(a: HabitRecoveryAnalysis) = HabitRecoveryCacheDto(
                shouldRecover = a.shouldRecover,
                triggerReasons = a.triggerReasons.map { it.name },
                overallTone = a.overallTone.name,
                coachingMessage = a.coachingMessage,
                struggling = a.struggling.map {
                    StrugglingCacheDto(it.title, it.completionRate7d, it.completionRate14d, it.missStreak, it.recoveryReason)
                },
                recoveryActions = a.recoveryActions.map {
                    ActionCacheDto(it.type.name, it.targetHabit, it.title, it.description, it.suggestedValue)
                }
            )
        }
    }

    @Serializable
    private data class StrugglingCacheDto(
        val title: String,
        val completionRate7d: Int,
        val completionRate14d: Int,
        val missStreak: Int,
        val recoveryReason: String
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
        val HHMM_REGEX = Regex("^([01]\\d|2[0-3]):[0-5]\\d$")
    }
}
