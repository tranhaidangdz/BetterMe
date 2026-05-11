package com.example.betterme.domain.usecase.habit

import com.example.betterme.data.local.datastore.DataStoreManager
import com.example.betterme.domain.repository.HabitLogRepository
import com.example.betterme.domain.repository.HabitRepository
import kotlinx.coroutines.flow.first

/**
 * Reschedules every active habit reminder for the *current user*.
 *
 * Used by:
 * - [com.example.betterme.data.receiver.HabitBootReceiver] after reboot / timezone
 *   change / app upgrade, since `AlarmManager` state is wiped in all three cases.
 * - Manually after sign-in if we want a freshly-signed-in user's reminders armed
 *   without waiting for them to add or edit a habit.
 *
 * Filters at the boundary:
 * - Skip habits with blank `reminder_time` (user opted out).
 * - Skip habits whose journey is complete (DONE log count ≥ planned duration).
 *   These are locked from check-in anyway — no point waking the user up for them.
 *
 * Uses the same [ScheduleHabitReminderUseCase] code path the UI calls, so there is
 * exactly one place where habit alarms are armed.
 */
class RescheduleAllHabitRemindersUseCase(
    private val dataStoreManager: DataStoreManager,
    private val habitRepository: HabitRepository,
    private val habitLogRepository: HabitLogRepository,
    private val scheduleHabitReminder: ScheduleHabitReminderUseCase
) {

    suspend operator fun invoke() {
        val userId = dataStoreManager.getCurrentUserId().first().orEmpty()
        if (userId.isBlank()) return

        val habits = habitRepository.getHabits(userId).first()
        val DAY_MS = 24L * 60L * 60L * 1000L

        for (habit in habits) {
            val reminder = habit.reminder_time
            if (reminder.isNullOrBlank()) continue

            // Skip habits whose journey is already complete — they're locked from
            // check-in by the UI, so a reminder would be misleading.
            val plannedDuration = if (habit.end_date != null) {
                (((habit.end_date - habit.start_date) / DAY_MS) + 1).toInt().coerceAtLeast(1)
            } else {
                Int.MAX_VALUE
            }
            val doneCount = habitLogRepository.countCompleted(habit.id)
            if (doneCount >= plannedDuration) continue

            scheduleHabitReminder(habit.id, habit.title, reminder)
        }
    }
}
