package com.example.betterme.domain.usecase.challenge

import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.example.betterme.data.local.room.entities.ReminderEntity
import com.example.betterme.data.worker.ChallengeReminderWorker
import com.example.betterme.domain.repository.ChallengeRepository
import com.example.betterme.domain.repository.ReminderRepository
import java.util.concurrent.TimeUnit

/**
 * Toggle the one-shot "challenge starts soon" reminder for an upcoming challenge.
 *
 * If currently off → schedules a one-time work to fire at challenge.start_date at 09:00.
 * If currently on → cancels.
 *
 * Returns the new on/off state.
 */
class ToggleStartReminderUseCase(
    private val workManager: WorkManager,
    private val challengeRepository: ChallengeRepository,
    private val reminderRepository: ReminderRepository
) {

    suspend operator fun invoke(challengeId: Int): Boolean {
        val targetType = "CHALLENGE_START"
        val existing = reminderRepository.getActiveByTarget(targetType, challengeId)
        if (existing != null) {
            workManager.cancelUniqueWork(ChallengeReminderWorker.startReminderWorkName(challengeId))
            reminderRepository.deleteByTarget(targetType, challengeId)
            return false
        }

        val challenge = challengeRepository.getById(challengeId) ?: return false
        val startMillis = challenge.start_date ?: return false

        // Fire 9 AM on the start day.
        val nineAm = startMillis + 9L * 60L * 60L * 1000L
        val now = System.currentTimeMillis()
        val delay = (nineAm - now).coerceAtLeast(60_000L)

        val data = workDataOf(
            ChallengeReminderWorker.KEY_TARGET_ID to challengeId,
            ChallengeReminderWorker.KEY_TITLE to "Thử thách \"${challenge.title}\" sắp bắt đầu",
            ChallengeReminderWorker.KEY_TEXT to "Hôm nay là ngày bắt đầu — hãy tham gia ngay!"
        )

        val request = OneTimeWorkRequestBuilder<ChallengeReminderWorker>()
            .setInitialDelay(delay, TimeUnit.MILLISECONDS)
            .setInputData(data)
            .addTag(TAG)
            .build()

        workManager.enqueueUniqueWork(
            ChallengeReminderWorker.startReminderWorkName(challengeId),
            ExistingWorkPolicy.REPLACE,
            request
        )

        reminderRepository.addReminder(
            ReminderEntity(
                target_type = targetType,
                target_id = challengeId,
                time = startMillis.toString(),
                is_active = true,
                work_id = request.id.toString()
            )
        )

        return true
    }

    companion object {
        const val TAG = "challenge_start_reminder"
    }
}
