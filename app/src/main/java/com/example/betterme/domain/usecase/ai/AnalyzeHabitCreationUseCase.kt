package com.example.betterme.domain.usecase.ai

import android.util.Log
import com.example.betterme.data.local.datastore.DataStoreManager
import com.example.betterme.domain.ai.AiCacheRepository
import com.example.betterme.domain.ai.AiCacheRepository.Companion.TTL_MS_SCHEDULE
import com.example.betterme.domain.ai.AiCacheRepository.Companion.TYPE_HABIT_CREATION_ANALYSIS
import com.example.betterme.domain.ai.AiHabitInsightRepository
import com.example.betterme.domain.ai.habitcreation.CreationRiskLevel
import com.example.betterme.domain.ai.habitcreation.CreationSuggestionType
import com.example.betterme.domain.ai.habitcreation.ExistingHabit
import com.example.betterme.domain.ai.habitcreation.HabitCompletionRollup
import com.example.betterme.domain.ai.habitcreation.HabitCreationAnalysis
import com.example.betterme.domain.ai.habitcreation.HabitCreationInput
import com.example.betterme.domain.ai.habitcreation.HabitCreationSuggestion
import com.example.betterme.domain.ai.habitcreation.HabitCreationWarning
import com.example.betterme.domain.ai.habitcreation.LifestyleAnchors
import com.example.betterme.domain.ai.habitcreation.NewHabit
import com.example.betterme.domain.ai.habitcreation.WarningType
import com.example.betterme.domain.ai.schedule.UserLifestyleProfile
import com.example.betterme.domain.repository.CategoryRepository
import com.example.betterme.domain.repository.HabitLogRepository
import com.example.betterme.domain.repository.HabitRepository
import com.example.betterme.utils.DateUtils
import kotlinx.coroutines.flow.first
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Drives the AI Habit Creation Assistant.
 *
 * 1. Pulls the user's habits and bucketizes them into active / archived /
 *    completed (active = `end_date >= today` or null; archived = ended past;
 *    completed = treated as previously-finished journey, surfaced separately
 *    in the prompt for context).
 * 2. Computes a 14-day per-habit completion rollup so the AI can reason about
 *    whether the user is currently *succeeding* or *struggling* — relevant
 *    when judging whether another habit is sustainable to add.
 * 3. Cache-first read keyed by the fingerprint of (new habit + existing habit
 *    ids + reminder times + lifestyle). 24h TTL — re-tapping Save with the
 *    same form within the day is a free local read.
 * 4. Cache write is skipped when `isCanned = true` so connectivity recovery
 *    isn't blocked by stale offline content.
 */
class AnalyzeHabitCreationUseCase(
    private val dataStoreManager: DataStoreManager,
    private val habitRepository: HabitRepository,
    private val habitLogRepository: HabitLogRepository,
    private val categoryRepository: CategoryRepository,
    private val aiRepository: AiHabitInsightRepository,
    private val cache: AiCacheRepository
) {

    suspend operator fun invoke(
        newTitle: String,
        newCategoryId: Int?,
        newReminderTime: String,
        newDurationMinutes: Int = 30,
        newDifficulty: String = "MEDIUM",
        newFrequency: String = "daily",
        lifestyle: UserLifestyleProfile? = null,
        forceRefresh: Boolean = false
    ): HabitCreationAnalysis {
        val userId = dataStoreManager.getCurrentUserId().first().orEmpty()
        val effectiveLifestyle = lifestyle ?: UserLifestyleProfile.Default
        if (userId.isBlank() || newTitle.isBlank()) {
            // Nothing meaningful to compare against — skip the AI call entirely
            // and return a friendly LOW-risk baseline. Saves a wasted network
            // round-trip when the user is signed out or hasn't typed a title.
            return baselineEmptyAnalysis()
        }

        val allHabits = habitRepository.getHabits(userId).first()
        val todayMs = DateUtils.startOfDay()
        val active = allHabits.filter { it.end_date == null || it.end_date >= todayMs }
        val archived = allHabits.filter { it.end_date != null && it.end_date < todayMs }

        val categoryMap = runCatching {
            categoryRepository.getAll().first().associateBy { it.id }
        }.getOrDefault(emptyMap())
        val categoryName = newCategoryId?.let { categoryMap[it]?.name } ?: ""

        val activeForPrompt = active.map { h ->
            ExistingHabit(
                title = h.title,
                categoryName = h.category_id?.let { categoryMap[it]?.name }.orEmpty(),
                reminderTime = h.reminder_time.orEmpty(),
                durationMinutes = 30,
                difficulty = "MEDIUM",
                frequency = "daily"
            )
        }

        val completion = active.map { habit ->
            val logs = habitLogRepository.getLogs(habit.id).first()
            val windowStart = todayMs - WINDOW_DAYS_MS
            val doneInWindow = logs.count { it.status == "DONE" && it.date in windowStart..todayMs }
            val habitDays = (((todayMs - habit.start_date) / DAY_MS) + 1)
                .toInt().coerceIn(1, WINDOW_DAYS)
            val rate = ((doneInWindow.toFloat() / habitDays.toFloat()) * 100f)
                .toInt().coerceIn(0, 100)
            HabitCompletionRollup(title = habit.title, completionRate = rate)
        }

        val input = HabitCreationInput(
            newHabit = NewHabit(
                title = newTitle,
                categoryName = categoryName,
                reminderTime = newReminderTime,
                durationMinutes = newDurationMinutes,
                difficulty = newDifficulty,
                frequency = newFrequency
            ),
            activeHabits = activeForPrompt,
            completedHabitTitles = archived.filter { (it.end_date ?: 0L) < todayMs }.map { it.title },
            archivedHabitTitles = archived.map { it.title },
            recentCompletion = completion,
            lifestyle = LifestyleAnchors(
                sleepStart = effectiveLifestyle.sleepStart,
                sleepEnd = effectiveLifestyle.sleepEnd,
                workStart = effectiveLifestyle.workStart,
                workEnd = effectiveLifestyle.workEnd
            )
        )

        val cacheKey = fingerprint(input).hashCode()
        if (!forceRefresh) {
            cache.getFresh(cacheKey, TYPE_HABIT_CREATION_ANALYSIS, TTL_MS_SCHEDULE)?.let { cachedJson ->
                decode(cachedJson)?.let { return it }
            }
        }

        val result = aiRepository.analyzeHabitCreation(input)
        if (!result.isCanned) {
            cache.save(cacheKey, TYPE_HABIT_CREATION_ANALYSIS, encode(result))
        }
        return result
    }

    private fun baselineEmptyAnalysis(): HabitCreationAnalysis = HabitCreationAnalysis(
        shouldWarn = false,
        overallRisk = CreationRiskLevel.LOW,
        warnings = emptyList(),
        suggestions = emptyList(),
        encouragement = "Chúc bạn duy trì đều đặn — một thói quen nhỏ tốt hơn không có thói quen nào.",
        isCanned = false
    )

    /**
     * Stable fingerprint of the inputs that drive the AI call. Anything that
     * could change the analysis (new habit content, existing habits with
     * reminder times, lifestyle anchors) is in the hash; ephemeral things
     * (today's date, completion rates that update every day) are deliberately
     * NOT in the hash so the cache survives normal daily check-ins.
     */
    private fun fingerprint(input: HabitCreationInput): String = buildString {
        val n = input.newHabit
        append("n:${n.title}|${n.categoryName}|${n.reminderTime}|${n.durationMinutes}|${n.difficulty}|${n.frequency}")
        append("||a:")
        input.activeHabits.sortedBy { it.title }.forEach { h ->
            append("${h.title}@${h.reminderTime};")
        }
        append("||sl:${input.lifestyle.sleepStart}-${input.lifestyle.sleepEnd}")
        append("||wk:${input.lifestyle.workStart}-${input.lifestyle.workEnd}")
    }

    private fun encode(a: HabitCreationAnalysis): String =
        json.encodeToString(HabitCreationCacheDto.fromDomain(a))

    private fun decode(raw: String): HabitCreationAnalysis? = try {
        json.decodeFromString<HabitCreationCacheDto>(raw).toDomain()
    } catch (e: Exception) {
        Log.w("AnalyzeHabitCreation", "Failed to decode cached analysis", e)
        null
    }

    // ============================================================
    // Cache wire format
    // ============================================================
    @Serializable
    private data class HabitCreationCacheDto(
        val shouldWarn: Boolean,
        val overallRisk: String,
        val warnings: List<WarningCacheDto>,
        val suggestions: List<SuggestionCacheDto>,
        val encouragement: String
    ) {
        fun toDomain(): HabitCreationAnalysis = HabitCreationAnalysis(
            shouldWarn = shouldWarn,
            overallRisk = runCatching { CreationRiskLevel.valueOf(overallRisk) }
                .getOrDefault(CreationRiskLevel.LOW),
            warnings = warnings.mapNotNull { it.toDomain() },
            suggestions = suggestions.mapNotNull { it.toDomain() },
            encouragement = encouragement,
            isCanned = false
        )

        companion object {
            fun fromDomain(a: HabitCreationAnalysis) = HabitCreationCacheDto(
                shouldWarn = a.shouldWarn,
                overallRisk = a.overallRisk.name,
                warnings = a.warnings.map { WarningCacheDto(it.type.name, it.message) },
                suggestions = a.suggestions.map { SuggestionCacheDto(it.type.name, it.message) },
                encouragement = a.encouragement
            )
        }
    }

    @Serializable
    private data class WarningCacheDto(val type: String, val message: String) {
        fun toDomain(): HabitCreationWarning? {
            val parsed = runCatching { WarningType.valueOf(type) }.getOrNull() ?: return null
            return HabitCreationWarning(parsed, message)
        }
    }

    @Serializable
    private data class SuggestionCacheDto(val type: String, val message: String) {
        fun toDomain(): HabitCreationSuggestion? {
            val parsed = runCatching { CreationSuggestionType.valueOf(type) }.getOrNull() ?: return null
            return HabitCreationSuggestion(parsed, message)
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
