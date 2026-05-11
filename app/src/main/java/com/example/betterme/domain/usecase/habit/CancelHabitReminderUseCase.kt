package com.example.betterme.domain.usecase.habit

import android.app.AlarmManager
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.example.betterme.data.receiver.HabitReminderReceiver

/**
 * Cancels any pending alarm + dismisses any active notification for a habit.
 *
 * Called when:
 * - The user deletes a habit (no future reminders should fire).
 * - The habit's `reminder_time` is cleared to null (user disabled the reminder).
 * - The habit's journey reached 100% (completion lockout — no more reminders).
 * - A reminder time *changed* (cancel old, then schedule new in the call site).
 *
 * Idempotent: calling it for a habit that has no pending alarm is a no-op. Uses
 * the same habitId-based requestCode that [ScheduleHabitReminderUseCase] used, so
 * the PendingIntent lookup matches by identity.
 */
class CancelHabitReminderUseCase(
    private val context: Context
) {
    operator fun invoke(habitId: Int) {
        if (habitId <= 0) return

        val intent = Intent(context, HabitReminderReceiver::class.java).apply {
            action = HabitReminderReceiver.ACTION_FIRE
            putExtra(HabitReminderReceiver.EXTRA_HABIT_ID, habitId)
        }
        // FLAG_NO_CREATE returns null when there's nothing to cancel — saves us
        // from creating a throwaway PendingIntent just to immediately destroy it.
        val pi = PendingIntent.getBroadcast(
            context,
            habitId,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pi != null) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
            alarmManager?.cancel(pi)
            pi.cancel()
        }

        // Also dismiss any already-posted notification — the user shouldn't see a
        // stale reminder after they delete the habit it referenced.
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        nm?.cancel(HabitReminderReceiver.NOTIFICATION_ID_BASE + habitId)
    }
}
