package com.example.betterme.data.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.betterme.domain.repository.NotificationRepository
import kotlinx.coroutines.runBlocking
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/**
 * Daily worker that runs at 00:00 local time and clears notifications older than 24 hours
 * so the in-app notification center starts each day fresh. Scheduled by
 * [com.example.betterme.di.KoinApp.scheduleDailyMidnightCleanup].
 */
class MidnightCleanupWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params), KoinComponent {

    private val notificationRepository: NotificationRepository by inject()

    override suspend fun doWork(): Result {
        val cutoff = System.currentTimeMillis() - DAY_MS
        runCatching { notificationRepository.deleteOlderThan(cutoff) }
            .onFailure { return Result.retry() }
        return Result.success()
    }

    companion object {
        const val UNIQUE_NAME = "midnight_notification_cleanup"
        private const val DAY_MS: Long = 24L * 60L * 60L * 1000L
    }
}
