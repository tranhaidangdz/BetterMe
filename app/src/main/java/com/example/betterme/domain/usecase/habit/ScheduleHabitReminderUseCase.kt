package com.example.betterme.domain.usecase.habit

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.example.betterme.data.receiver.HabitReminderReceiver
import java.util.Calendar

/**
 * Production-grade habit reminder scheduler.
 *
 * Uses `AlarmManager.setExactAndAllowWhileIdle` for true alarm-clock-style timing:
 * the OS will wake the device out of Doze to fire the alarm at the configured
 * minute boundary. This is the only API that survives Doze for foreground-quality
 * timing — `WorkManager` periodic jobs drift by 10-15 minutes on Doze, and
 * `setRepeating` is inexact and rounded.
 *
 * Alarm-clock semantics:
 * - One-shot per call. The companion [HabitReminderReceiver] re-arms tomorrow's
 *   slot from inside its `onReceive`, so the daily cadence emerges from
 *   alarm → notification → reschedule rather than from a "repeating" primitive.
 * - PendingIntent uses `FLAG_UPDATE_CURRENT | FLAG_IMMUTABLE` so a re-schedule
 *   (reminder time changed by the user, alarm just fired and we're re-arming, etc.)
 *   replaces the prior alarm at the same `requestCode` (= habitId).
 *
 * Android 12+ permission handling:
 * - From API 31 onward, `setExactAndAllowWhileIdle` requires the
 *   `SCHEDULE_EXACT_ALARM` permission. If the user revoked it (or the OEM denies
 *   it by default), we fall back to `setAndAllowWhileIdle` — inexact but still
 *   reliable to within a few minutes and works without special permission.
 *   Either way, the alarm is queued; the user never sees a silent failure.
 */
class ScheduleHabitReminderUseCase(
    private val context: Context
) {

    /**
     * @param habitId      stable id, used as the PendingIntent requestCode so subsequent
     *                     scheduling for the same habit replaces (not duplicates) any
     *                     prior alarm.
     * @param habitTitle   carried into the receiver via the broadcast intent only for
     *                     logging; the real title is re-loaded from the DB at fire time.
     * @param reminderTime "HH:MM" 24-hour. Blank/null → no-op.
     */
    operator fun invoke(habitId: Int, habitTitle: String, reminderTime: String?) {
        if (reminderTime.isNullOrBlank()) return
        val (hour, minute) = parseHhMm(reminderTime) ?: return

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
            ?: return

        val triggerAtMillis = nextOccurrenceMillis(hour, minute)
        val pendingIntent = buildPendingIntent(habitId)

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent
                    )
                } else {
                    // Permission not granted by user; fall back to inexact but still
                    // doze-aware. Drift can be up to a few minutes — acceptable for a
                    // reminder, and never silently skipped.
                    alarmManager.setAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent
                    )
                }
            } else {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent
                )
            }
            Log.d(TAG, "Scheduled habit $habitId ($habitTitle) at $reminderTime -> $triggerAtMillis")
        } catch (e: SecurityException) {
            // OEM denied exact alarm without an explicit grant flow. Fall through to
            // the inexact path one more time so the reminder still fires.
            Log.w(TAG, "setExactAndAllowWhileIdle denied, falling back to inexact", e)
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent
            )
        }
    }

    private fun buildPendingIntent(habitId: Int): PendingIntent {
        val intent = Intent(context, HabitReminderReceiver::class.java).apply {
            action = HabitReminderReceiver.ACTION_FIRE
            putExtra(HabitReminderReceiver.EXTRA_HABIT_ID, habitId)
        }
        return PendingIntent.getBroadcast(
            context,
            habitId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    /**
     * Returns today at [hour]:[minute] if that's still in the future, otherwise
     * tomorrow at the same minute. Computed against the device's current local time.
     */
    private fun nextOccurrenceMillis(hour: Int, minute: Int): Long {
        val now = Calendar.getInstance()
        val target = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        if (!target.after(now)) target.add(Calendar.DAY_OF_YEAR, 1)
        return target.timeInMillis
    }

    private fun parseHhMm(raw: String): Pair<Int, Int>? {
        val parts = raw.split(":")
        if (parts.size != 2) return null
        val h = parts[0].toIntOrNull() ?: return null
        val m = parts[1].toIntOrNull() ?: return null
        if (h !in 0..23 || m !in 0..59) return null
        return h to m
    }

    private companion object {
        const val TAG = "HabitReminderSched"
    }
}
