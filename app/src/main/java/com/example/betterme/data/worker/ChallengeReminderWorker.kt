package com.example.betterme.data.worker

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.betterme.MainActivity
import com.example.betterme.R
import com.example.betterme.data.local.datastore.DataStoreManager
import com.example.betterme.data.local.room.entities.NotificationEntity
import com.example.betterme.domain.repository.NotificationRepository
import kotlinx.coroutines.flow.first
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/**
 * Worker that posts a reminder notification for a challenge — and now also persists an
 * in-app row in the [NotificationEntity] table so the user can see it from the Home
 * notification center.
 *
 * Used both as a periodic worker (daily challenge check-in reminders) and a one-shot worker
 * (upcoming-challenge start reminders).
 *
 * The notification is posted on a HIGH-importance channel ([CHANNEL_ID]) with sound,
 * vibration, default lights, an alarm-style category and a full-screen intent flag so the
 * OS will treat it like an alarm whenever screen-on permissions allow.
 */
class ChallengeReminderWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params), KoinComponent {

    private val notificationRepository: NotificationRepository by inject()
    private val dataStoreManager: DataStoreManager by inject()

    override suspend fun doWork(): Result {
        val targetId = inputData.getInt(KEY_TARGET_ID, -1)
        if (targetId < 0) return Result.failure()

        val title = inputData.getString(KEY_TITLE) ?: "Thử thách"
        val text = inputData.getString(KEY_TEXT)
            ?: "Đến giờ check-in thử thách rồi!"
        val deeplinkId = inputData.getInt(KEY_DEEPLINK_USER_CHALLENGE_ID, -1)
            .takeIf { it > 0 }
        val challengeId = inputData.getInt(KEY_CHALLENGE_ID, -1).takeIf { it > 0 }
        val type = inputData.getString(KEY_NOTIFICATION_TYPE) ?: TYPE_CHALLENGE_REMINDER
        val reminderTimeLabel = inputData.getString(KEY_REMINDER_TIME_LABEL)

        val ctx = applicationContext

        // 1. System push (alarm-style high priority)
        val launchIntent = Intent(ctx, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            if (deeplinkId != null) putExtra(EXTRA_OPEN_USER_CHALLENGE_ID, deeplinkId)
            if (challengeId != null) putExtra(EXTRA_OPEN_CHALLENGE_ID, challengeId)
        }
        val pendingIntent = PendingIntent.getActivity(
            ctx,
            targetId,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(ctx, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_challenge)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)

        // Full-screen intent gives the notification alarm-style urgency on supported OEMs.
        // Falls back gracefully where the OS denies the privilege.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            builder.setFullScreenIntent(pendingIntent, true)
        }

        val nm = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(NOTIFICATION_ID_BASE + targetId, builder.build())

        // 2. In-app inbox row (Home notification center)
        val userId = dataStoreManager.getCurrentUserId().first().orEmpty()
        if (userId.isNotBlank()) {
            notificationRepository.insert(
                NotificationEntity(
                    user_id = userId,
                    title = title,
                    message = text,
                    type = type,
                    challenge_id = challengeId,
                    user_challenge_id = deeplinkId,
                    reminder_time_label = reminderTimeLabel
                )
            )
        }

        return Result.success()
    }

    companion object {
        const val CHANNEL_ID = "challenge_reminders"
        const val CHANNEL_NAME = "Nhắc thử thách"
        const val CHANNEL_DESCRIPTION = "Thông báo cho thử thách bạn đang tham gia"

        const val KEY_TARGET_ID = "target_id"
        const val KEY_TITLE = "title"
        const val KEY_TEXT = "text"
        const val KEY_DEEPLINK_USER_CHALLENGE_ID = "deeplink_uc_id"
        const val KEY_CHALLENGE_ID = "challenge_id"
        const val KEY_NOTIFICATION_TYPE = "notification_type"
        const val KEY_REMINDER_TIME_LABEL = "reminder_time_label"

        const val EXTRA_OPEN_USER_CHALLENGE_ID = "open_user_challenge_id"
        const val EXTRA_OPEN_CHALLENGE_ID = "open_challenge_id"

        const val TYPE_CHALLENGE_REMINDER = "CHALLENGE_REMINDER"
        const val TYPE_CHALLENGE_START = "CHALLENGE_START"

        private const val NOTIFICATION_ID_BASE = 9000

        fun uniqueWorkName(userChallengeId: Int) = "challenge_reminder_$userChallengeId"
        fun startReminderWorkName(challengeId: Int) = "challenge_start_$challengeId"

        /** AudioAttributes used by the high-priority channel. Public so KoinApp can reuse. */
        fun alarmAudioAttributes(): AudioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_NOTIFICATION_RINGTONE)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        fun defaultAlarmSound(): android.net.Uri =
            RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
    }
}
