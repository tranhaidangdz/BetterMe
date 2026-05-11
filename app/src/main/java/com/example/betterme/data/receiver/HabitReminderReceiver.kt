package com.example.betterme.data.receiver

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.betterme.MainActivity
import com.example.betterme.R
import com.example.betterme.data.local.datastore.DataStoreManager
import com.example.betterme.data.local.room.entities.NotificationEntity
import com.example.betterme.domain.repository.HabitRepository
import com.example.betterme.domain.repository.NotificationRepository
import com.example.betterme.domain.usecase.habit.ScheduleHabitReminderUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/**
 * Fires when the AlarmManager exact alarm for a habit reminder triggers.
 *
 * Responsibilities (in order):
 * 1. Load the habit from Room. If it's gone (deleted) or its journey is complete,
 *    drop the alarm and return — never post a notification for a habit the user
 *    can no longer interact with.
 * 2. Post a HIGH-importance notification on the dedicated habit-reminders channel
 *    (sound, vibration, full-screen capable). The notification's PendingIntent
 *    carries EXTRA_OPEN_HABIT_ID so MainActivity can route the tap straight to
 *    the Habit Detail screen.
 * 3. Re-arm tomorrow's alarm via [ScheduleHabitReminderUseCase]. Because alarms
 *    only fire once, this re-arm step is what makes the reminder feel "daily".
 *
 * The receiver runs on the main thread with an implicit goAsync()-style pattern via
 * [CoroutineScope] — Android guarantees ~10s before the receiver process is killed,
 * which is more than enough for the Room read + alarm reschedule.
 */
class HabitReminderReceiver : BroadcastReceiver(), KoinComponent {

    private val habitRepository: HabitRepository by inject()
    private val scheduleHabitReminder: ScheduleHabitReminderUseCase by inject()
    private val dataStoreManager: DataStoreManager by inject()
    private val notificationRepository: NotificationRepository by inject()

    override fun onReceive(context: Context, intent: Intent) {
        val habitId = intent.getIntExtra(EXTRA_HABIT_ID, -1)
        if (habitId <= 0) return

        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val habit = habitRepository.getHabitById(habitId)
                if (habit == null || habit.reminder_time.isNullOrBlank()) return@launch

                // Drop the alarm silently when the signed-in user differs from the
                // habit owner — prevents reminders armed under a previous account
                // from leaking into the current account's notification tray.
                val currentUserId = dataStoreManager.getCurrentUserId().first().orEmpty()
                if (currentUserId.isNotBlank() && currentUserId != habit.user_id) return@launch

                val motivationalLine = pickMotivationalLine(habitId)
                postNotification(context, habitId, habit.title, motivationalLine)

                // Persist into the in-app inbox so the Home bell + notification
                // center reflect this reminder even after the system tray dismiss.
                // The system push is fire-and-forget; this row is the authoritative
                // record the user reads later in the app.
                runCatching {
                    notificationRepository.insert(
                        NotificationEntity(
                            user_id = habit.user_id,
                            title = "Đến giờ rồi — \"${habit.title}\"",
                            message = motivationalLine,
                            type = "HABIT_REMINDER",
                            habit_id = habitId,
                            reminder_time_label = habit.reminder_time
                        )
                    )
                }

                // Re-arm for tomorrow at the same time. ScheduleHabitReminderUseCase
                // computes the next occurrence relative to "now", so calling it after
                // the alarm fires lands tomorrow's slot automatically.
                scheduleHabitReminder(habitId, habit.title, habit.reminder_time)
            } finally {
                pending.finish()
            }
        }
    }

    private fun postNotification(
        context: Context,
        habitId: Int,
        title: String,
        motivationalLine: String
    ) {
        val launchIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_OPEN_HABIT_ID, habitId)
        }
        val tapIntent = PendingIntent.getActivity(
            context,
            habitId,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_challenge)
            .setContentTitle("Đến giờ rồi — \"$title\"")
            .setContentText(motivationalLine)
            .setStyle(NotificationCompat.BigTextStyle().bigText(motivationalLine))
            .setContentIntent(tapIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            builder.setFullScreenIntent(tapIntent, false)
        }

        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(NOTIFICATION_ID_BASE + habitId, builder.build())
    }

    /**
     * Rotates through a small bank of motivational lines so the same habit shows
     * different copy across days. Deterministic per (habitId × day-of-year) so the
     * line within one day is stable.
     */
    private fun pickMotivationalLine(habitId: Int): String {
        val lines = listOf(
            "Hành động nhỏ hôm nay, kết quả lớn ngày mai.",
            "Mỗi check-in là một viên gạch xây nên phiên bản tốt hơn.",
            "Bạn đã quyết tâm — bây giờ chỉ cần làm thôi.",
            "Sự nhất quán đánh bại sự hoàn hảo.",
            "5 phút bắt đầu hơn 0 phút hoàn hảo.",
            "Tương lai của bạn được tạo ra từ những thói quen hôm nay.",
            "Đừng phá vỡ chuỗi — bạn đang trên đà!"
        )
        val dayOfYear = java.util.Calendar.getInstance()
            .get(java.util.Calendar.DAY_OF_YEAR)
        return lines[((habitId * 31 + dayOfYear) % lines.size).coerceAtLeast(0)]
    }

    companion object {
        const val CHANNEL_ID = "habit_reminders"
        const val CHANNEL_NAME = "Nhắc thói quen"
        const val CHANNEL_DESCRIPTION = "Thông báo đúng giờ cho mỗi thói quen bạn đang theo dõi"

        const val EXTRA_HABIT_ID = "extra_habit_id"
        const val EXTRA_OPEN_HABIT_ID = "open_habit_id"

        const val ACTION_FIRE = "com.example.betterme.HABIT_REMINDER_FIRE"

        /** Notification id base; per-habit id added to keep concurrent reminders distinct. */
        const val NOTIFICATION_ID_BASE = 7000
    }
}
