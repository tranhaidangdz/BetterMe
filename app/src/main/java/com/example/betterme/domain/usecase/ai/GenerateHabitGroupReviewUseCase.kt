package com.example.betterme.domain.usecase.ai

import com.example.betterme.data.local.datastore.DataStoreManager
import com.example.betterme.domain.ai.AiCacheRepository
import com.example.betterme.domain.ai.AiCacheRepository.Companion.TYPE_REVIEW
import com.example.betterme.domain.ai.AiCoachPersonality
import com.example.betterme.domain.ai.AiHabitInsightRepository
import com.example.betterme.domain.ai.AiHabitInsightRepository.AiResult
import com.example.betterme.domain.ai.personalization.CategoryKind
import com.example.betterme.domain.ai.personalization.GroupInsightContext
import com.example.betterme.domain.ai.personalization.PersonalitySignal
import com.example.betterme.domain.repository.HabitLogRepository
import com.example.betterme.domain.repository.HabitRepository
import com.example.betterme.utils.DateUtils
import kotlinx.coroutines.flow.first

/**
 * Builds the per-category insight context, feeds it to the AI repo,
 * returns the coach response.
 *
 * The use case now does the heavy lifting for *both* the live AI path
 * and the canned offline path — it computes:
 *  - per-habit completion lines (which the prompt must reference by
 *    name + which the canned template rotation reads),
 *  - personality signals derived from the full habit / log set,
 *  - the [CategoryKind] for the category,
 *  - a coarse trend label (improving / drifting / holding / no-data).
 *
 * Caching strategy unchanged:
 *  1. `invoke(forceRefresh=false)` consults the cache first.
 *  2. `forceRefresh=true` (user tapped "Tạo lại") bypasses cache.
 *  3. On total failure with a stale entry → surface the stale entry so
 *     the user never sees a blank error.
 *
 * Canned content is never written to cache so the next visit has a
 * chance to get a real response.
 */
class GenerateHabitGroupReviewUseCase(
    private val dataStoreManager: DataStoreManager,
    private val habitRepository: HabitRepository,
    private val habitLogRepository: HabitLogRepository,
    private val aiRepository: AiHabitInsightRepository,
    private val cache: AiCacheRepository
) {

    suspend operator fun invoke(
        categoryId: Int,
        categoryName: String,
        personality: AiCoachPersonality = AiCoachPersonality.Default,
        forceRefresh: Boolean = false
    ): AiResult {
        if (!forceRefresh) {
            cache.getFresh(categoryId, TYPE_REVIEW)?.let { cached ->
                return AiResult.Success(cached)
            }
        }

        val userId = dataStoreManager.getCurrentUserId().first().orEmpty()
        if (userId.isBlank()) {
            return AiResult.Failure("Vui lòng đăng nhập để dùng AI")
        }

        val allHabits = habitRepository.getHabits(userId).first()
        val habits = allHabits.filter { it.category_id == categoryId }
        if (habits.isEmpty()) {
            return AiResult.Failure("Chưa có thói quen nào trong nhóm này")
        }

        val todayMs = DateUtils.startOfDay()

        // ── Per-habit rollup (this category) ────────────────────────
        var totalPlanned = 0
        var totalDone = 0
        var completedJourneys = 0
        var bestStreakOverall = 0
        var rate7Sum = 0
        var rate14Sum = 0
        var habitsWithEnoughHistory = 0
        val perHabitLines = mutableListOf<String>()
        val habitTitles = mutableListOf<String>()
        // Build the log map once — it feeds both the per-habit stats and
        // the PersonalitySignal derivation pass below.
        val logsByHabitId = allHabits.associate { habit ->
            habit.id to habitLogRepository.getLogs(habit.id).first()
        }

        for (habit in habits) {
            val planned = if (habit.end_date != null) {
                (((habit.end_date - habit.start_date) / DAY_MS) + 1).toInt().coerceAtLeast(1)
            } else {
                (((todayMs - habit.start_date) / DAY_MS) + 1).toInt().coerceAtLeast(1)
            }
            val done = habitLogRepository.countCompleted(habit.id)
            val logs = logsByHabitId[habit.id].orEmpty()
            val doneDays = logs.filter { it.status == "DONE" }
                .map { DateUtils.startOfDay(it.date) }
            val longestStreak = DateUtils.longestStreak(doneDays)
            val rate = if (planned > 0) ((done.toFloat() / planned) * 100).toInt() else 0

            totalPlanned += planned
            totalDone += done
            if (habit.end_date != null && done >= planned) completedJourneys++
            if (longestStreak > bestStreakOverall) bestStreakOverall = longestStreak

            val daysSinceStart = (((todayMs - habit.start_date) / DAY_MS) + 1)
                .toInt().coerceAtLeast(1)
            if (daysSinceStart >= 14) {
                rate7Sum += completionInWindow(doneDays.toSet(), todayMs, 7, daysSinceStart, 0)
                rate14Sum += completionInWindow(doneDays.toSet(), todayMs, 14, daysSinceStart, 0)
                habitsWithEnoughHistory++
            }

            habitTitles += habit.title
            perHabitLines += "- \"${habit.title}\"" +
                (habit.reminder_time?.let { " ($it)" } ?: "") +
                ": $done/$planned ngày ($rate%), chuỗi dài nhất $longestStreak ngày"
        }

        val overallRate = if (totalPlanned > 0) ((totalDone.toFloat() / totalPlanned) * 100).toInt() else 0
        val missedDays = (totalPlanned - totalDone).coerceAtLeast(0)

        // ── Personality + trend signals ─────────────────────────────
        val signals = PersonalitySignal.derive(allHabits, logsByHabitId, todayMs)
        val trendLabel = when {
            habitsWithEnoughHistory == 0 -> "no-data"
            rate7Sum >= rate14Sum + 10 * habitsWithEnoughHistory -> "improving"
            rate7Sum + 10 * habitsWithEnoughHistory <= rate14Sum -> "drifting"
            else -> "holding"
        }

        // ── Time-of-day hints for this category's habits ────────────
        val reminderHours = habits.mapNotNull {
            it.reminder_time?.split(":")?.firstOrNull()?.toIntOrNull()
        }
        val timeOfDayHints = buildList {
            if (reminderHours.isEmpty()) {
                add("no-reminders-set")
            } else {
                val morning = reminderHours.count { it < 12 }
                val afternoon = reminderHours.count { it in 12..17 }
                val evening = reminderHours.count { it >= 18 }
                val total = reminderHours.size
                if (morning * 2 >= total) add("morning-heavy")
                if (afternoon * 2 >= total) add("afternoon-heavy")
                if (evening * 2 >= total) add("evening-heavy")
                if (reminderHours.any { it >= 21 }) add("has-late-night")
            }
        }

        val context = GroupInsightContext(
            categoryName = categoryName,
            categoryKind = CategoryKind.classify(categoryName),
            habitCount = habits.size,
            overallRate = overallRate,
            completedJourneys = completedJourneys,
            missedDays = missedDays,
            bestStreak = bestStreakOverall,
            perHabitLines = perHabitLines,
            habitTitles = habitTitles,
            signals = signals,
            timeOfDayHints = timeOfDayHints,
            trendLabel = trendLabel
        )

        val result = aiRepository.reviewHabitGroup(
            context = context,
            personality = personality
        )

        // Real model success → persist for the next 12h.
        if (result is AiResult.Success) {
            cache.save(categoryId, TYPE_REVIEW, result.text)
            return result
        }

        cache.getAny(categoryId, TYPE_REVIEW)?.let { stale ->
            return AiResult.Success(stale.content)
        }
        return result
    }

    private fun completionInWindow(
        doneDays: Set<Long>,
        todayMs: Long,
        windowDays: Int,
        daysSinceStart: Int,
        offsetDays: Int
    ): Int {
        if (daysSinceStart <= offsetDays) return 0
        val windowEnd = todayMs - offsetDays * DAY_MS
        val windowStart = windowEnd - windowDays * DAY_MS
        val done = doneDays.count { it in windowStart..windowEnd }
        val denom = minOf(windowDays, daysSinceStart - offsetDays).coerceAtLeast(1)
        return (done * 100 / denom).coerceIn(0, 100)
    }

    private companion object {
        const val DAY_MS: Long = 24L * 60L * 60L * 1000L
    }
}
