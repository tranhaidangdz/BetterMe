package com.example.betterme.domain.usecase.ai

import android.util.Log
import com.example.betterme.data.local.datastore.DataStoreManager
import com.example.betterme.data.local.room.entities.HabitEntity
import com.example.betterme.domain.ai.AiCacheRepository
import com.example.betterme.domain.ai.AiCacheRepository.Companion.TTL_MS_SCHEDULE
import com.example.betterme.domain.ai.AiCacheRepository.Companion.TYPE_SCHEDULE_ANALYSIS
import com.example.betterme.domain.ai.AiHabitInsightRepository
import com.example.betterme.domain.ai.ScheduleHabitInput
import com.example.betterme.domain.ai.schedule.BurnoutRisk
import com.example.betterme.domain.ai.schedule.ConflictType
import com.example.betterme.domain.ai.schedule.EnergyLevel
import com.example.betterme.domain.ai.schedule.OptimizedHabitTime
import com.example.betterme.domain.ai.schedule.ScheduleAnalysis
import com.example.betterme.domain.ai.schedule.ScheduleConflict
import com.example.betterme.domain.ai.schedule.UserLifestyleProfile
import com.example.betterme.domain.repository.HabitRepository
import kotlinx.coroutines.flow.first
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Drives the AI Schedule Conflict Analyzer flow end-to-end:
 *
 * 1. Loads the user's habits from Room and filters to those with a reminder.
 * 2. Translates each habit into a [ScheduleHabitInput] — supplies default
 *    `difficulty=MEDIUM`, `priority=MEDIUM`, `estimatedMinutes=30` because the
 *    current `HabitEntity` schema doesn't yet carry those fields. When the
 *    Add Habit form starts collecting them, only this translator changes.
 * 3. Computes a stable fingerprint over the habit IDs + reminder times +
 *    lifestyle profile. Cache key uses the fingerprint's hash as a synthetic
 *    categoryId so distinct schedules don't collide on a single row. 24h TTL.
 * 4. Cache hit → decode the stored JSON and return.
 * 5. Cache miss / [forceRefresh] → call the repo; on real-model success, persist
 *    the JSON for next time. When all AI models fail the repo throws
 *    [com.example.betterme.domain.ai.AiUnavailableException] — the VM surfaces
 *    a retry-able error state and nothing is cached.
 */
class AnalyzeScheduleUseCase(
    private val dataStoreManager: DataStoreManager,
    private val habitRepository: HabitRepository,
    private val aiRepository: AiHabitInsightRepository,
    private val cache: AiCacheRepository
) {

    suspend operator fun invoke(
        profile: UserLifestyleProfile? = null,
        forceRefresh: Boolean = false
    ): ScheduleAnalysis {
        val userId = dataStoreManager.getCurrentUserId().first().orEmpty()
        if (userId.isBlank()) {
            // No signed-in user → no habits to analyze. The repo's empty-data
            // branch would still kick in, but skipping the call is cheaper.
            return aiRepository.analyzeSchedule(profile, emptyList())
        }

        val habits = habitRepository.getHabits(userId).first()
        val inputs = habits.mapNotNull(::toScheduleInput)
        val effectiveProfile = profile ?: UserLifestyleProfile.Default
        val fingerprint = fingerprint(inputs, effectiveProfile)
        val cacheKey = fingerprint.hashCode()

        if (!forceRefresh) {
            cache.getFresh(cacheKey, TYPE_SCHEDULE_ANALYSIS, TTL_MS_SCHEDULE)?.let { cachedJson ->
                decode(cachedJson)?.let { return it }
            }
        }

        val result = aiRepository.analyzeSchedule(effectiveProfile, inputs)
        cache.save(cacheKey, TYPE_SCHEDULE_ANALYSIS, encode(result))
        return result
    }

    /**
     * Defaults applied for fields the schema doesn't yet store: MEDIUM
     * difficulty + MEDIUM priority + 30-minute duration. Once the Add Habit
     * form captures these, only this function changes.
     */
    private fun toScheduleInput(habit: HabitEntity): ScheduleHabitInput? {
        val reminder = habit.reminder_time?.takeIf { it.isNotBlank() } ?: return null
        return ScheduleHabitInput(
            id = habit.id,
            title = habit.title,
            reminderTime = reminder,
            difficulty = "MEDIUM",
            priority = "MEDIUM",
            estimatedMinutes = 30
        )
    }

    /**
     * Deterministic fingerprint of the inputs that drive the analysis. Any
     * change in habit set, reminder times, sleep, or work windows produces a
     * different fingerprint and therefore a cache miss — no manual cache
     * invalidation needed when the user edits a habit.
     */
    private fun fingerprint(habits: List<ScheduleHabitInput>, profile: UserLifestyleProfile): String =
        buildString {
            habits.sortedBy { it.id }.forEach { append("${it.id}@${it.reminderTime};") }
            append("s:${profile.sleepStart}-${profile.sleepEnd};")
            append("w:${profile.workStart}-${profile.workEnd}")
        }

    private fun encode(analysis: ScheduleAnalysis): String =
        json.encodeToString(ScheduleAnalysisCacheDto.fromDomain(analysis))

    private fun decode(raw: String): ScheduleAnalysis? = try {
        json.decodeFromString<ScheduleAnalysisCacheDto>(raw).toDomain()
    } catch (e: Exception) {
        Log.w("AnalyzeSchedule", "Failed to decode cached analysis", e)
        null
    }

    // ============================================================
    // CACHE DTO
    //
    // Lives in the use case (not the repo) because the repo's
    // ScheduleAnalysisDto is private. Keeping a tiny mirror here decouples
    // cache storage from the wire-side parsing types so the repo's DTOs can
    // change without breaking a previously-cached row's schema.
    // ============================================================
    @Serializable
    private data class ScheduleAnalysisCacheDto(
        val hasConflict: Boolean,
        val scheduleScore: Int,
        val energyLevel: String,
        val burnoutRisk: String,
        val summary: String,
        val positiveFeedback: String,
        val conflicts: List<ConflictCacheDto>,
        val optimizedSchedule: List<OptimizedCacheDto>
    ) {
        fun toDomain(): ScheduleAnalysis = ScheduleAnalysis(
            hasConflict = hasConflict,
            scheduleScore = scheduleScore,
            energyLevel = runCatching { EnergyLevel.valueOf(energyLevel) }.getOrDefault(EnergyLevel.MODERATE),
            burnoutRisk = runCatching { BurnoutRisk.valueOf(burnoutRisk) }.getOrDefault(BurnoutRisk.LOW),
            summary = summary,
            positiveFeedback = positiveFeedback,
            conflicts = conflicts.mapNotNull { it.toDomain() },
            optimizedSchedule = optimizedSchedule.map { OptimizedHabitTime(it.habit, it.suggestedTime) },
            // Cached entries are always real model results (we don't cache canned
            // ones); flag them as not-canned so the UI shows the live treatment.
        )

        companion object {
            fun fromDomain(a: ScheduleAnalysis): ScheduleAnalysisCacheDto = ScheduleAnalysisCacheDto(
                hasConflict = a.hasConflict,
                scheduleScore = a.scheduleScore,
                energyLevel = a.energyLevel.name,
                burnoutRisk = a.burnoutRisk.name,
                summary = a.summary,
                positiveFeedback = a.positiveFeedback,
                conflicts = a.conflicts.map { ConflictCacheDto(it.type.name, it.habitA, it.habitB, it.issue, it.suggestion) },
                optimizedSchedule = a.optimizedSchedule.map { OptimizedCacheDto(it.habit, it.suggestedTime) }
            )
        }
    }

    @Serializable
    private data class ConflictCacheDto(
        val type: String,
        val habitA: String,
        val habitB: String?,
        val issue: String,
        val suggestion: String
    ) {
        fun toDomain(): ScheduleConflict? {
            val parsedType = runCatching { ConflictType.valueOf(type) }.getOrNull() ?: return null
            return ScheduleConflict(parsedType, habitA, habitB, issue, suggestion)
        }
    }

    @Serializable
    private data class OptimizedCacheDto(
        val habit: String,
        val suggestedTime: String
    )

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }
}
