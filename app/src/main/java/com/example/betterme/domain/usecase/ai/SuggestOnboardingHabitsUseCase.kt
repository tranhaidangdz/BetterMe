package com.example.betterme.domain.usecase.ai

import android.util.Log
import com.example.betterme.data.local.datastore.DataStoreManager
import com.example.betterme.data.local.room.entities.CategoryEntity
import com.example.betterme.data.local.room.entities.HabitEntity
import com.example.betterme.domain.ai.AiCacheRepository
import com.example.betterme.domain.ai.AiCacheRepository.Companion.TTL_MS_SCHEDULE
import com.example.betterme.domain.ai.AiCacheRepository.Companion.TYPE_ONBOARDING
import com.example.betterme.domain.ai.AiHabitInsightRepository
import com.example.betterme.domain.ai.onboarding.Difficulty
import com.example.betterme.domain.ai.onboarding.HabitCategoryKey
import com.example.betterme.domain.ai.onboarding.OnboardingProfile
import com.example.betterme.domain.ai.onboarding.OnboardingSuggestedHabit
import com.example.betterme.domain.ai.onboarding.OnboardingSuggestion
import com.example.betterme.domain.ai.onboarding.Priority
import com.example.betterme.domain.ai.schedule.EnergyLevel
import com.example.betterme.domain.ai.schedule.UserLifestyleProfile
import com.example.betterme.domain.repository.CategoryRepository
import com.example.betterme.domain.repository.HabitRepository
import com.example.betterme.domain.usecase.habit.ScheduleHabitReminderUseCase
import com.example.betterme.utils.DateUtils
import kotlinx.coroutines.flow.first
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.concurrent.TimeUnit

/**
 * Drives the AI onboarding suggestion flow:
 *
 * 1. Cache-first read keyed by the profile fingerprint (goals + categories +
 *    experience + activity + existing habits + lifestyle). 24h TTL — long
 *    enough that revisits during the same onboarding session don't burn quota.
 * 2. Calls [AiHabitInsightRepository.suggestOnboardingHabits].
 * 3. Persists real-model output to the cache. When all AI models fail the repo
 *    throws [com.example.betterme.domain.ai.AiUnavailableException] — the VM
 *    surfaces a retry-able error state and nothing is cached.
 *
 * The [accept] entry point materializes a chosen [OnboardingSuggestedHabit]
 * as a real [HabitEntity]: resolves the AI's `HabitCategoryKey` enum against
 * the user's actual `CategoryRepository` rows via substring-keyword matching,
 * inserts the row with day-aligned start/end dates, then arms the reminder.
 */
class SuggestOnboardingHabitsUseCase(
    private val dataStoreManager: DataStoreManager,
    private val aiRepository: AiHabitInsightRepository,
    private val cache: AiCacheRepository,
    private val categoryRepository: CategoryRepository,
    private val habitRepository: HabitRepository,
    private val scheduleHabitReminder: ScheduleHabitReminderUseCase
) {

    /**
     * Fetch (or read from cache) a starter-habit suggestion bundle for the
     * given profile. Always returns an [OnboardingSuggestion] — canned local
     * fallback is served by the repo when every Gemini model fails.
     */
    suspend operator fun invoke(
        profile: OnboardingProfile,
        lifestyle: UserLifestyleProfile? = null,
        forceRefresh: Boolean = false
    ): OnboardingSuggestion {
        val effectiveLifestyle = lifestyle ?: UserLifestyleProfile.Default
        val cacheKey = fingerprint(profile, effectiveLifestyle).hashCode()

        if (!forceRefresh) {
            cache.getFresh(cacheKey, TYPE_ONBOARDING, TTL_MS_SCHEDULE)?.let { cachedJson ->
                decode(cachedJson)?.let { return it }
            }
        }

        val result = aiRepository.suggestOnboardingHabits(profile, effectiveLifestyle)
        cache.save(cacheKey, TYPE_ONBOARDING, encode(result))
        return result
    }

    /**
     * Materialize a chosen AI suggestion as a real habit row. Returns the new
     * habit id on success, or null when no user is signed in.
     *
     * Category resolution: tries each keyword on [HabitCategoryKey.matchKeywords]
     * against every [CategoryEntity.name] (case-insensitive substring). First
     * non-null match wins. If nothing matches the habit is still inserted —
     * just with `category_id = null` (HabitEntity allows it).
     */
    suspend fun accept(suggestion: OnboardingSuggestedHabit): Int? {
        val userId = dataStoreManager.getCurrentUserId().first().orEmpty()
        if (userId.isBlank()) return null

        val categories = runCatching { categoryRepository.getAll().first() }.getOrDefault(emptyList())
        val categoryId = resolveCategoryId(suggestion.category, categories)

        val now = System.currentTimeMillis()
        val todayStart = DateUtils.startOfDay(now)
        val thirtyDays = TimeUnit.DAYS.toMillis(30)

        val entity = HabitEntity(
            user_id = userId,
            category_id = categoryId,
            title = suggestion.title,
            description = suggestion.description.ifBlank { null },
            start_date = todayStart,
            end_date = todayStart + thirtyDays,
            reminder_time = suggestion.reminderTime,
            created_at = now
        )
        val newId = habitRepository.addHabit(entity).toInt()
        scheduleHabitReminder(
            habitId = newId,
            habitTitle = suggestion.title,
            reminderTime = suggestion.reminderTime
        )
        return newId
    }

    /**
     * Convenience: accept many at once. Returns the count actually inserted.
     */
    suspend fun acceptAll(suggestions: List<OnboardingSuggestedHabit>): Int {
        var count = 0
        suggestions.forEach { if (accept(it) != null) count++ }
        return count
    }

    private fun resolveCategoryId(
        key: HabitCategoryKey,
        categories: List<CategoryEntity>
    ): Int? {
        for (keyword in key.matchKeywords) {
            val match = categories.firstOrNull { entity ->
                entity.name.contains(keyword, ignoreCase = true)
            }
            if (match != null) return match.id
        }
        return null
    }

    /**
     * Stable fingerprint of the inputs that drive the AI call. Any change in
     * goals, selected categories, lifestyle, or existing habits invalidates
     * the cache naturally — no manual eviction needed.
     */
    private fun fingerprint(
        profile: OnboardingProfile,
        lifestyle: UserLifestyleProfile
    ): String = buildString {
        append("g:")
        append(profile.goals.sorted().joinToString(","))
        append("|c:")
        append(profile.selectedCategories.map { it.name }.sorted().joinToString(","))
        append("|e:").append(profile.experienceLevel.name)
        append("|a:").append(profile.activityLevel)
        append("|h:")
        append(profile.existingHabitTitles.sorted().joinToString(","))
        append("|w:")
        append(profile.wellnessFlags.sorted().joinToString(","))
        append("|sl:").append(lifestyle.sleepStart).append("-").append(lifestyle.sleepEnd)
        append("|wk:").append(lifestyle.workStart).append("-").append(lifestyle.workEnd)
    }

    private fun encode(s: OnboardingSuggestion): String =
        json.encodeToString(OnboardingCacheDto.fromDomain(s))

    private fun decode(raw: String): OnboardingSuggestion? = try {
        json.decodeFromString<OnboardingCacheDto>(raw).toDomain()
    } catch (e: Exception) {
        Log.w("SuggestOnboarding", "Failed to decode cached suggestion", e)
        null
    }

    // ============================================================
    // Cache wire format
    //
    // Lives in the use case so the repo's private parser DTOs and the
    // cache row schema can evolve independently.
    // ============================================================
    @Serializable
    private data class OnboardingCacheDto(
        val summary: String,
        val energyProfile: String,
        val recommendedFocus: String,
        val habits: List<HabitCacheDto>
    ) {
        fun toDomain(): OnboardingSuggestion = OnboardingSuggestion(
            summary = summary,
            energyProfile = runCatching { EnergyLevel.valueOf(energyProfile) }
                .getOrDefault(EnergyLevel.MODERATE),
            recommendedFocus = recommendedFocus,
            habits = habits.mapNotNull { it.toDomain() },
        )

        companion object {
            fun fromDomain(s: OnboardingSuggestion) = OnboardingCacheDto(
                summary = s.summary,
                energyProfile = s.energyProfile.name,
                recommendedFocus = s.recommendedFocus,
                habits = s.habits.map { HabitCacheDto.fromDomain(it) }
            )
        }
    }

    @Serializable
    private data class HabitCacheDto(
        val title: String,
        val emoji: String,
        val description: String,
        val category: String,
        val difficulty: String,
        val priority: String,
        val estimatedMinutes: Int,
        val reminderTime: String,
        val motivation: String
    ) {
        fun toDomain(): OnboardingSuggestedHabit? {
            val cat = HabitCategoryKey.fromStringOrNull(category) ?: return null
            return OnboardingSuggestedHabit(
                title = title,
                emoji = emoji,
                description = description,
                category = cat,
                difficulty = runCatching { Difficulty.valueOf(difficulty) }.getOrDefault(Difficulty.EASY),
                priority = runCatching { Priority.valueOf(priority) }.getOrDefault(Priority.MEDIUM),
                estimatedMinutes = estimatedMinutes,
                reminderTime = reminderTime,
                motivation = motivation
            )
        }

        companion object {
            fun fromDomain(h: OnboardingSuggestedHabit) = HabitCacheDto(
                title = h.title,
                emoji = h.emoji,
                description = h.description,
                category = h.category.name,
                difficulty = h.difficulty.name,
                priority = h.priority.name,
                estimatedMinutes = h.estimatedMinutes,
                reminderTime = h.reminderTime,
                motivation = h.motivation
            )
        }
    }

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }
}
