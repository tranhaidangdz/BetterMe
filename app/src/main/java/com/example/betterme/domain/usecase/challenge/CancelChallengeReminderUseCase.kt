package com.example.betterme.domain.usecase.challenge

import androidx.work.WorkManager
import com.example.betterme.data.worker.ChallengeReminderWorker
import com.example.betterme.domain.repository.ReminderRepository

class CancelChallengeReminderUseCase(
    private val workManager: WorkManager,
    private val reminderRepository: ReminderRepository
) {

    suspend operator fun invoke(userChallengeId: Int) {
        workManager.cancelUniqueWork(ChallengeReminderWorker.uniqueWorkName(userChallengeId))
        reminderRepository.deleteByTarget("USER_CHALLENGE", userChallengeId)
    }
}
