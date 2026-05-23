package com.example.betterme.data.worker

import android.content.Context
import android.util.Log
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkerParameters
import com.example.betterme.data.sync.SyncCoordinator
import org.koin.core.context.GlobalContext
import java.util.concurrent.TimeUnit

/**
 * Background sync worker. Calls [SyncCoordinator.syncAll] from a coroutine context
 * supplied by WorkManager so it survives process death and uses WorkManager's
 * exponential-backoff retry on failure.
 *
 * Three triggers in this codebase:
 *  1. App launch — one-shot via [enqueueOneShot] from KoinApp.onCreate.
 *  2. Periodic — every 30 min via [schedulePeriodic] from KoinApp.onCreate.
 *  3. Connectivity returns — one-shot from a network callback in KoinApp.
 *
 * Constraints require [NetworkType.CONNECTED]; offline triggers wait for the
 * device to come back online before the worker runs.
 *
 * Retry policy: WorkManager's default exponential backoff (30s base, 5h cap) via
 * `Result.retry()` on transient failures. Permanent failures (e.g., not signed in,
 * no Firestore access) return `Result.success()` — the next trigger will retry
 * with fresh state.
 */
class SyncWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        return try {
            // Resolve from Koin's global registry — CoroutineWorker is instantiated
            // by WorkManager via its no-arg factory, so constructor-injection isn't
            // available here without a WorkerFactory.
            val coordinator = GlobalContext.get().get<SyncCoordinator>()
            val ok = coordinator.syncAll()
            if (ok) Result.success() else Result.retry()
        } catch (e: Exception) {
            Log.w(TAG, "SyncWorker threw — retrying", e)
            Result.retry()
        }
    }

    companion object {
        const val TAG = "SyncWorker"
        const val PERIODIC_UNIQUE_NAME = "betterme_sync_periodic"
        const val ONE_SHOT_UNIQUE_NAME = "betterme_sync_one_shot"

        fun periodicConstraints(): Constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        fun periodicRequest() =
            PeriodicWorkRequestBuilder<SyncWorker>(30, TimeUnit.MINUTES)
                .setConstraints(periodicConstraints())
                .addTag(PERIODIC_UNIQUE_NAME)
                .build()

        fun oneShotRequest() =
            OneTimeWorkRequestBuilder<SyncWorker>()
                .setConstraints(periodicConstraints())
                .addTag(ONE_SHOT_UNIQUE_NAME)
                .build()
    }
}
