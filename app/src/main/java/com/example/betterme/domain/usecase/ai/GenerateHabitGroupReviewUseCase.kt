package com.example.betterme.domain.usecase.ai

import com.example.betterme.data.local.datastore.DataStoreManager
import com.example.betterme.domain.ai.AiCacheRepository
import com.example.betterme.domain.ai.AiCacheRepository.Companion.TYPE_REVIEW
import com.example.betterme.domain.ai.AiCoachPersonality
import com.example.betterme.domain.ai.AiHabitInsightRepository
import com.example.betterme.domain.ai.AiHabitInsightRepository.AiResult
import com.example.betterme.domain.repository.HabitLogRepository
import com.example.betterme.domain.repository.HabitRepository
import com.example.betterme.utils.DateUtils
import kotlinx.coroutines.flow.first

/**
 * Builds the per-category analytics snapshot, feeds it to the AI repo, returns the
 * coach response.
 *
 * Caching strategy (12h via [AiCacheRepository]):
 * 1. `invoke(forceRefresh=false)` consults the cache first — fresh hit returns
 *    instantly without an OpenRouter call. Repeated screen visits within the
 *    window get the same coaching text.
 * 2. `forceRefresh=true` (user tapped "Tạo lại") bypasses the cache and writes the
 *    new response on top of the old entry.
 * 3. On network failure with a stale entry available, the use case returns the
 *    stale entry through [invokeOffline] so the user still gets *something*.
 *
 * Why this lives as a use case (not inside the VM): the prompt construction is
 * deterministic and reusable — future surfaces (Habit Detail, weekly digest) can
 * call the same path without re-implementing the stats summarization.
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

        val habits = habitRepository.getHabits(userId).first()
            .filter { it.category_id == categoryId }
        if (habits.isEmpty()) {
            return AiResult.Failure("Chưa có thói quen nào trong nhóm này")
        }

        val dayMs = 24L * 60L * 60L * 1000L
        val now = DateUtils.startOfDay()

        var totalPlanned = 0
        var totalDone = 0
        var completedJourneys = 0
        var bestStreakOverall = 0
        val perHabitLines = mutableListOf<String>()

        for (habit in habits) {
            val planned = if (habit.end_date != null) {
                (((habit.end_date - habit.start_date) / dayMs) + 1).toInt().coerceAtLeast(1)
            } else {
                (((now - habit.start_date) / dayMs) + 1).toInt().coerceAtLeast(1)
            }
            val done = habitLogRepository.countCompleted(habit.id)
            val streakDates = habitLogRepository.getLogs(habit.id).first()
                .filter { it.status == "DONE" }
                .map { DateUtils.startOfDay(it.date) }
            val longestStreak = DateUtils.longestStreak(streakDates)
            val rate = if (planned > 0) ((done.toFloat() / planned) * 100).toInt() else 0

            totalPlanned += planned
            totalDone += done
            if (habit.end_date != null && done >= planned) completedJourneys++
            if (longestStreak > bestStreakOverall) bestStreakOverall = longestStreak

            perHabitLines += "- \"${habit.title}\": $done/$planned ngày ($rate%), chuỗi dài nhất $longestStreak ngày"
        }

        val overallRate = if (totalPlanned > 0) ((totalDone.toFloat() / totalPlanned) * 100).toInt() else 0
        val missedDays = (totalPlanned - totalDone).coerceAtLeast(0)

        val statsBlock = buildString {
            appendLine("Tổng số thói quen: ${habits.size}")
            appendLine("Số thói quen đã hoàn thành toàn bộ: $completedJourneys")
            appendLine("Tổng ngày kế hoạch: $totalPlanned, đã check-in: $totalDone")
            appendLine("Tỷ lệ hoàn thành tổng: $overallRate%")
            appendLine("Số ngày bỏ lỡ: $missedDays")
            appendLine("Chuỗi dài nhất ghi nhận: $bestStreakOverall ngày")
            appendLine()
            appendLine("Chi tiết từng thói quen:")
            perHabitLines.forEach { appendLine(it) }
        }.trim()

        val result = aiRepository.reviewHabitGroup(
            categoryName = categoryName,
            stats = statsBlock,
            personality = personality
        )

        // Real model success → persist for the next 12h.
        // Canned success (all models failed) → DO NOT cache. Next visit might have
        // working connectivity; we don't want canned content to occupy the cache.
        if (result is AiResult.Success) {
            if (!result.isCanned) {
                cache.save(categoryId, TYPE_REVIEW, result.text)
            }
            return result
        }

        // Network call failed entirely → if we have *any* prior entry (even past
        // TTL), surface it instead of leaving the user with a blank error card.
        cache.getAny(categoryId, TYPE_REVIEW)?.let { stale ->
            return AiResult.Success(stale.content)
        }
        return result
    }
}
