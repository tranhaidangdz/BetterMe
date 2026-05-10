package com.example.betterme.data.worker

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.betterme.MainActivity
import com.example.betterme.R

/**
 * Worker that posts a reminder notification for a challenge.
 *
 * Used both as periodic worker (daily challenge reminders) and one-shot worker (upcoming
 * challenge start reminders).
 */
class ChallengeReminderWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val targetId = inputData.getInt(KEY_TARGET_ID, -1)
        if (targetId < 0) return Result.failure()

        val title = inputData.getString(KEY_TITLE) ?: "Thử thách"
        val text = inputData.getString(KEY_TEXT)
            ?: "Đến giờ check-in thử thách rồi!"
        val deeplinkId = inputData.getInt(KEY_DEEPLINK_USER_CHALLENGE_ID, -1)
            .takeIf { it > 0 }

        val ctx = applicationContext

        val launchIntent = Intent(ctx, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            if (deeplinkId != null) {
                putExtra(EXTRA_OPEN_USER_CHALLENGE_ID, deeplinkId)
            }
        }
        val pendingIntent = PendingIntent.getActivity(
            ctx,
            targetId,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(ctx, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_challenge)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        val nm = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(NOTIFICATION_ID_BASE + targetId, notification)

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

        const val EXTRA_OPEN_USER_CHALLENGE_ID = "open_user_challenge_id"

        private const val NOTIFICATION_ID_BASE = 9000

        fun uniqueWorkName(userChallengeId: Int) = "challenge_reminder_$userChallengeId"
        fun startReminderWorkName(challengeId: Int) = "challenge_start_$challengeId"
    }
}
