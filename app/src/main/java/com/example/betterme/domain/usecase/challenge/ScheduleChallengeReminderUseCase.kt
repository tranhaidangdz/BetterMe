package com.example.betterme.domain.usecase.challenge

import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.example.betterme.data.local.room.entities.ReminderEntity
import com.example.betterme.data.worker.ChallengeReminderWorker
import com.example.betterme.domain.repository.ReminderRepository
import java.util.Calendar
import java.util.concurrent.TimeUnit

/**
 * Schedules a daily reminder for a UserChallenge at the given local hour:minute.
 * Replaces any existing reminder for the same target. Persists a [ReminderEntity] row
 * with the WorkManager request UUID so it can be cancelled later.
 */
class ScheduleChallengeReminderUseCase(
    private val workManager: WorkManager,
    private val reminderRepository: ReminderRepository
) {

    suspend operator fun invoke(
        userChallengeId: Int,
        challengeTitle: String,
        hourOfDay: Int = 8,
        minute: Int = 0
    ) {
        val targetType = "USER_CHALLENGE"
        val initialDelayMs = computeInitialDelayMs(hourOfDay, minute)

        val data = workDataOf(
            ChallengeReminderWorker.KEY_TARGET_ID to userChallengeId,
            ChallengeReminderWorker.KEY_TITLE to "Thử thách \"$challengeTitle\"",
            ChallengeReminderWorker.KEY_TEXT to "Đến giờ check-in rồi! Hãy giữ chuỗi của bạn.",
            ChallengeReminderWorker.KEY_DEEPLINK_USER_CHALLENGE_ID to userChallengeId
        )

        val request = PeriodicWorkRequestBuilder<ChallengeReminderWorker>(
            24, TimeUnit.HOURS
        )
            .setInitialDelay(initialDelayMs, TimeUnit.MILLISECONDS)
            .setInputData(data)
            .addTag(TAG)
            .build()

        workManager.enqueueUniquePeriodicWork(
            ChallengeReminderWorker.uniqueWorkName(userChallengeId),
            ExistingPeriodicWorkPolicy.UPDATE,
            request
        )

        // Upsert the reminder row.
        val timeStr = "%02d:%02d".format(hourOfDay, minute)
        val existing = reminderRepository.getActiveByTarget(targetType, userChallengeId)
        if (existing != null) {
            reminderRepository.updateReminder(
                existing.copy(time = timeStr, is_active = true, work_id = request.id.toString())
            )
        } else {
            reminderRepository.addReminder(
                ReminderEntity(
                    target_type = targetType,
                    target_id = userChallengeId,
                    time = timeStr,
                    is_active = true,
                    work_id = request.id.toString()
                )
            )
        }
    }

    private fun computeInitialDelayMs(hourOfDay: Int, minute: Int): Long {
        val now = Calendar.getInstance()
        val target = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hourOfDay)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        if (!target.after(now)) {
            target.add(Calendar.DAY_OF_YEAR, 1)
        }
        return (target.timeInMillis - now.timeInMillis).coerceAtLeast(0L)
    }

    companion object {
        const val TAG = "challenge_reminder"
    }
}
