package com.example.betterme.domain.usecase.ai

import android.util.Log
import com.example.betterme.data.local.datastore.DataStoreManager
import com.example.betterme.domain.ai.schedule.OptimizedHabitTime
import com.example.betterme.domain.repository.HabitRepository
import com.example.betterme.domain.usecase.habit.ScheduleHabitReminderUseCase
import kotlinx.coroutines.flow.first

/**
 * Materializes an AI [OptimizedHabitTime] list into real habit-row updates.
 *
 * For each suggestion:
 *   1. Find the user's habit by title (case-sensitive, trimmed). If no match,
 *      silently skip — we'd rather drop a stale suggestion than fail the apply.
 *   2. Update `reminder_time` on the row via [HabitRepository.updateHabit].
 *   3. Re-arm the AlarmManager alarm via [ScheduleHabitReminderUseCase] so the
 *      new time fires tonight, not tomorrow.
 *
 * Returns the count of suggestions that were actually applied — useful for
 * confirming "Đã áp dụng N đề xuất" in the UI.
 */
class ApplyScheduleSuggestionsUseCase(
    private val dataStoreManager: DataStoreManager,
    private val habitRepository: HabitRepository,
    private val scheduleHabitReminder: ScheduleHabitReminderUseCase
) {
    suspend operator fun invoke(suggestions: List<OptimizedHabitTime>): Int {
        if (suggestions.isEmpty()) return 0
        val userId = dataStoreManager.getCurrentUserId().first().orEmpty()
        if (userId.isBlank()) return 0

        val habits = habitRepository.getHabits(userId).first()
        val byTitle = habits.associateBy { it.title.trim() }

        var applied = 0
        suggestions.forEach { suggestion ->
            val target = byTitle[suggestion.habit.trim()] ?: run {
                Log.d("ApplySchedule", "No habit matched '${suggestion.habit}', skipping")
                return@forEach
            }
            // Skip no-op updates (suggestion already matches current value) so we
            // don't churn the AlarmManager unnecessarily.
            if (target.reminder_time == suggestion.suggestedTime) return@forEach

            habitRepository.updateHabit(target.copy(reminder_time = suggestion.suggestedTime))
            scheduleHabitReminder(
                habitId = target.id,
                habitTitle = target.title,
                reminderTime = suggestion.suggestedTime
            )
            applied++
        }
        return applied
    }
}
