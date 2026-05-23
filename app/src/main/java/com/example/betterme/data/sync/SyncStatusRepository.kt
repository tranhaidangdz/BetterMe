package com.example.betterme.data.sync

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import com.example.betterme.domain.sync.SyncStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/**
 * Process-wide singleton holding the live [SyncStatus]. Writes from anywhere; reads
 * via [status]. The last successful sync timestamp is persisted to DataStore so it
 * survives process death (useful for "Đã đồng bộ lúc X" labels on next launch).
 *
 * This is a `StateFlow`-based store with no reactive backing — callers explicitly
 * call [setSyncing] / [setSuccess] / [setError] / [setOffline] / [setPendingCount]
 * from the sync coordinator and use cases. The repository is intentionally thin so
 * the sync coordinator owns transition policy.
 */
class SyncStatusRepository(
    private val dataStore: DataStore<Preferences>
) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _status = MutableStateFlow(SyncStatus())
    val status: StateFlow<SyncStatus> = _status.asStateFlow()

    init {
        // Hydrate the persisted last-synced timestamp on first construction so the
        // initial emission already carries it. Pending count + state stay default
        // (IDLE / 0) until the coordinator computes them.
        scope.launch {
            val persisted = runCatching {
                dataStore.data.map { it[LAST_SYNCED_AT_KEY] }.first()
            }.getOrNull()
            if (persisted != null && persisted > 0) {
                _status.value = _status.value.copy(lastSyncedAt = persisted)
            }
        }
    }

    fun setSyncing() {
        _status.value = _status.value.copy(state = SyncStatus.State.SYNCING, lastError = null)
    }

    fun setOffline() {
        _status.value = _status.value.copy(state = SyncStatus.State.OFFLINE)
    }

    fun setIdle() {
        _status.value = _status.value.copy(state = SyncStatus.State.IDLE, lastError = null)
    }

    fun setError(message: String) {
        _status.value = _status.value.copy(state = SyncStatus.State.ERROR, lastError = message)
    }

    /**
     * Marks the end of a fully successful sync pass and persists the timestamp.
     * Resets state to IDLE and lastError to null.
     */
    fun setSuccess(syncedAt: Long) {
        _status.value = _status.value.copy(
            state = SyncStatus.State.IDLE,
            lastSyncedAt = syncedAt,
            lastError = null
        )
        scope.launch {
            runCatching {
                dataStore.edit { it[LAST_SYNCED_AT_KEY] = syncedAt }
            }
        }
    }

    fun setPendingCount(count: Int) {
        _status.value = _status.value.copy(pendingCount = count)
    }

    private companion object {
        val LAST_SYNCED_AT_KEY = longPreferencesKey("sync_last_synced_at")
    }
}
