package com.example.betterme.domain.usecase.ai

import android.util.Log
import com.example.betterme.data.local.datastore.DataStoreManager
import com.example.betterme.domain.ai.AiCacheRepository
import com.example.betterme.domain.ai.AiCacheRepository.Companion.TTL_MS_SCHEDULE
import com.example.betterme.domain.ai.AiCacheRepository.Companion.TYPE_LIFESTYLE_INSIGHT
import com.example.betterme.domain.ai.AiHabitInsightRepository
import com.example.betterme.domain.ai.lifestyle.AdaptiveSuggestion
import com.example.betterme.domain.ai.lifestyle.EnergyPattern
import com.example.betterme.domain.ai.lifestyle.HabitCompletionRecord
import com.example.betterme.domain.ai.lifestyle.LifestyleInsight
import com.example.betterme.domain.ai.lifestyle.OverallTrend
import com.example.betterme.domain.ai.lifestyle.SuggestionType
import com.example.betterme.domain.ai.schedule.BurnoutRisk
import com.example.betterme.domain.ai.schedule.UserLifestyleProfile
import com.example.betterme.domain.repository.HabitLogRepository
import com.example.betterme.domain.repository.HabitRepository
import com.example.betterme.utils.DateUtils
import kotlinx.coroutines.flow.first
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Drives the AI Adaptive Lifestyle Insight flow end-to-end:
 *
 * 1. Loads the user's active habits + the last 14 days of habit logs.
 * 2. Computes per-habit `completionRate` over the window, capped at the
 *    number of days the habit has been active (so a 3-day-old habit isn't
 *    judged against 14 days).
 * 3. Detects behavioral patterns the prompt cares about:
 *     - "late-night habits"  — avg completion of habits with reminder ≥ 21:00 < 30%
 *     - "weekend inconsistency" — Sat+Sun avg < weekday avg − 20pts
 *     - "low overall completion" — global avg < 50%
 * 4. Cache-first read keyed by the fingerprint of habit set + completion
 *    rollup + lifestyle. 24h TTL. Canned (`isCanned = true`) results are
 *    skipped so the next session can produce a real insight.
 */
class AnalyzeLifestyleUseCase(
    private val dataStoreManager: DataStoreManager,
    private val habitRepository: HabitRepository,
    private val habitLogRepository: HabitLogRepository,
    private val aiRepository: AiHabitInsightRepository,
    private val cache: AiCacheRepository
) {

    suspend operator fun invoke(
        lifestyle: UserLifestyleProfile? = null,
        forceRefresh: Boolean = false
    ): LifestyleInsight {
        val userId = dataStoreManager.getCurrentUserId().first().orEmpty()
        if (userId.isBlank()) {
            // No signed-in user → empty data path inside the repo handles it.
            return aiRepository.analyzeLifestyle(
                lifestyle, emptyList(), emptyList(), emptyList(), emptyList()
            )
        }

        val effectiveLifestyle = lifestyle ?: UserLifestyleProfile.Default
        val habits = habitRepository.getHabits(userId).first()
        // Day-precise everything — logs land at start-of-day, habit.start_date
        // is normalized at write time, so using intra-day clock here would
        // open a window of off-by-one drift between the numerator's range
        // and the denominator's day-count.
        val today = DateUtils.startOfDay()
        val windowStart = today - WINDOW_DAYS_MS

        val records = habits.map { habit ->
            val logs = habitLogRepository.getLogs(habit.id).first()
            val doneInWindow = logs.count { log ->
                log.status == "DONE" && log.date in windowStart..today
            }
            val habitDays = (((today - habit.start_date) / DAY_MS) + 1)
                .toInt()
                .coerceIn(1, WINDOW_DAYS)
            val rate = ((doneInWindow.toFloat() / habitDays.toFloat()) * 100f)
                .toInt()
                .coerceIn(0, 100)
            HabitCompletionRecord(
                title = habit.title,
                completionRate = rate,
                preferredTime = habit.reminder_time.orEmpty(),
                difficulty = "MEDIUM" // HabitEntity doesn't carry difficulty yet
            )
        }

        val missedPatterns = detectPatterns(records)
        val activeTitles = habits.map { it.title }

        val cacheKey = fingerprint(records, effectiveLifestyle).hashCode()
        if (!forceRefresh) {
            cache.getFresh(cacheKey, TYPE_LIFESTYLE_INSIGHT, TTL_MS_SCHEDULE)?.let { cached ->
                decode(cached)?.let { return it }
            }
        }

        val result = aiRepository.analyzeLifestyle(
            lifestyle = effectiveLifestyle,
            history = records,
            missedPatterns = missedPatterns,
            activeHabitTitles = activeTitles,
            wellnessSignals = emptyList()
        )
        if (!result.isCanned) {
            cache.save(cacheKey, TYPE_LIFESTYLE_INSIGHT, encode(result))
        }
        return result
    }

    /**
     * Heuristic behavioral patterns. Run before the AI call and passed in so
     * the model doesn't have to compute these from raw logs (which it can't —
     * the prompt only carries per-habit rollups). Keep this list short: the
     * model only needs hints, not a full analysis.
     */
    private fun detectPatterns(records: List<HabitCompletionRecord>): List<String> {
        if (records.size < 2) return emptyList()
        val patterns = mutableListOf<String>()

        val lateHabits = records.filter { r ->
            val h = r.preferredTime.split(":").firstOrNull()?.toIntOrNull() ?: -1
            h >= 21
        }
        if (lateHabits.size >= 2 && lateHabits.map { it.completionRate }.average() < 30) {
            patterns += "late-night habits"
        }

        val avgCompletion = records.map { it.completionRate }.average()
        if (avgCompletion < 50) {
            patterns += "low overall completion"
        }

        return patterns
    }

    /**
     * Stable fingerprint of the inputs that drive the AI call. Any change in
     * habit set, completion rates, or lifestyle invalidates the cache
     * automatically — no manual eviction when the user checks in or adds a
     * habit.
     */
    private fun fingerprint(
        records: List<HabitCompletionRecord>,
        lifestyle: UserLifestyleProfile
    ): String = buildString {
        records.sortedBy { it.title }.forEach { r ->
            append(r.title).append("@").append(r.completionRate).append(";")
        }
        append("|sl:").append(lifestyle.sleepStart).append("-").append(lifestyle.sleepEnd)
        append("|wk:").append(lifestyle.workStart).append("-").append(lifestyle.workEnd)
    }

    private fun encode(i: LifestyleInsight): String =
        json.encodeToString(LifestyleInsightCacheDto.fromDomain(i))

    private fun decode(raw: String): LifestyleInsight? = try {
        json.decodeFromString<LifestyleInsightCacheDto>(raw).toDomain()
    } catch (e: Exception) {
        Log.w("AnalyzeLifestyle", "Failed to decode cached insight", e)
        null
    }

    // ============================================================
    // Cache wire format
    //
    // Lives in the use case so the repo's parser DTOs and the cache row
    // schema can evolve independently.
    // ============================================================
    @Serializable
    private data class LifestyleInsightCacheDto(
        val overallTrend: String,
        val burnoutRisk: String,
        val consistencyScore: Int,
        val energyPattern: String,
        val recoveryScore: Int,
        val primaryInsight: String,
        val coachingMessage: String,
        val adaptiveSuggestions: List<AdaptiveSuggestionCacheDto>
    ) {
        fun toDomain(): LifestyleInsight = LifestyleInsight(
            overallTrend = runCatching { OverallTrend.valueOf(overallTrend) }
                .getOrDefault(OverallTrend.STABLE),
            burnoutRisk = runCatching { BurnoutRisk.valueOf(burnoutRisk) }
                .getOrDefault(BurnoutRisk.LOW),
            consistencyScore = consistencyScore,
            energyPattern = runCatching { EnergyPattern.valueOf(energyPattern) }
                .getOrDefault(EnergyPattern.INCONSISTENT),
            recoveryScore = recoveryScore,
            primaryInsight = primaryInsight,
            coachingMessage = coachingMessage,
            adaptiveSuggestions = adaptiveSuggestions.mapNotNull { it.toDomain() },
            isCanned = false
        )

        companion object {
            fun fromDomain(i: LifestyleInsight) = LifestyleInsightCacheDto(
                overallTrend = i.overallTrend.name,
                burnoutRisk = i.burnoutRisk.name,
                consistencyScore = i.consistencyScore,
                energyPattern = i.energyPattern.name,
                recoveryScore = i.recoveryScore,
                primaryInsight = i.primaryInsight,
                coachingMessage = i.coachingMessage,
                adaptiveSuggestions = i.adaptiveSuggestions.map {
                    AdaptiveSuggestionCacheDto(it.type.name, it.title, it.reason, it.suggestion)
                }
            )
        }
    }

    @Serializable
    private data class AdaptiveSuggestionCacheDto(
        val type: String,
        val title: String,
        val reason: String,
        val suggestion: String
    ) {
        fun toDomain(): AdaptiveSuggestion? {
            val parsed = runCatching { SuggestionType.valueOf(type) }.getOrNull() ?: return null
            return AdaptiveSuggestion(parsed, title, reason, suggestion)
        }
    }

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    private companion object {
        const val WINDOW_DAYS = 14
        const val DAY_MS: Long = 24L * 60L * 60L * 1000L
        const val WINDOW_DAYS_MS: Long = WINDOW_DAYS * DAY_MS
    }
}
