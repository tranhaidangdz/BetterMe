package com.example.betterme.data.sync

import android.util.Log
import com.example.betterme.data.local.datastore.DataStoreManager
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Orchestrator for all [EntitySynchronizer]s. Owns a single mutex so concurrent
 * triggers (app launch + connectivity-resume + periodic worker) collapse into one
 * in-flight sync, and the running pass owns the [SyncStatusRepository] state.
 *
 * One pass:
 *  1. Gate on user id (skip if not signed in).
 *  2. Gate on connectivity. If offline, flip to OFFLINE state and exit early —
 *     Firestore SDK persistence will queue any new writes regardless.
 *  3. Flip status to SYNCING.
 *  4. For each synchronizer: call reconcile; aggregate dirty-counts; capture failures.
 *  5. If all synchronizers report success → setSuccess(now). Otherwise → setError.
 *
 * Failures are logged but not rethrown — sync runs in the background and must not
 * crash the host (worker / app launch coroutine).
 */
class SyncCoordinator(
    private val dataStoreManager: DataStoreManager,
    private val connectivity: ConnectivityObserver,
    private val syncStatusRepository: SyncStatusRepository,
    private val synchronizers: List<EntitySynchronizer>
) {

    private val mutex = Mutex()

    /**
     * Run the full sync pass. Safe to call from anywhere; concurrent callers wait
     * on the mutex and the first one in does the work.
     */
    suspend fun syncAll(): Boolean = mutex.withLock {
        val userId = dataStoreManager.getCurrentUserId().first().orEmpty()
        if (userId.isBlank()) {
            Log.d(TAG, "Skipping sync: no signed-in user")
            syncStatusRepository.setIdle()
            return@withLock true
        }

        if (!connectivity.isOnlineNow()) {
            Log.d(TAG, "Skipping sync: offline. Firestore SDK keeps the write queue.")
            // Still recompute pending so the badge reflects local dirty state.
            updatePendingCount(userId)
            syncStatusRepository.setOffline()
            return@withLock false
        }

        syncStatusRepository.setSyncing()
        Log.i(TAG, "Sync pass starting for uid=$userId (${synchronizers.size} synchronizers)")

        var allOk = true
        var firstError: String? = null
        val started = System.currentTimeMillis()
        synchronizers.forEach { synchronizer ->
            try {
                val ok = synchronizer.reconcile(userId)
                if (!ok) {
                    allOk = false
                    firstError = firstError ?: "${synchronizer.name}: partial failure"
                }
            } catch (e: Exception) {
                Log.w(TAG, "Synchronizer ${synchronizer.name} threw", e)
                allOk = false
                firstError = firstError ?: "${synchronizer.name}: ${e.message ?: e.javaClass.simpleName}"
            }
        }

        updatePendingCount(userId)

        return@withLock if (allOk) {
            syncStatusRepository.setSuccess(started)
            Log.i(TAG, "Sync pass succeeded in ${System.currentTimeMillis() - started}ms")
            true
        } else {
            syncStatusRepository.setError(firstError ?: "Sync failed")
            Log.w(TAG, "Sync pass finished with errors: $firstError")
            false
        }
    }

    private suspend fun updatePendingCount(userId: String) {
        val total = synchronizers.sumOf {
            runCatching { it.dirtyCount(userId) }.getOrDefault(0)
        }
        syncStatusRepository.setPendingCount(total)
    }

    private companion object {
        const val TAG = "SyncCoordinator"
    }
}
