package com.example.betterme.presentation.sync

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.betterme.data.sync.SyncCoordinator
import com.example.betterme.data.sync.SyncStatusRepository
import com.example.betterme.domain.sync.SyncStatus
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * Thin façade over [SyncStatusRepository] so Compose screens can observe sync state
 * without depending on the data layer directly. Also exposes a one-shot manual
 * "sync now" action backed by [SyncCoordinator] for pull-to-refresh and the
 * "thử đồng bộ lại" retry button on the error chip.
 */
class SyncStatusViewModel(
    syncStatusRepository: SyncStatusRepository,
    private val syncCoordinator: SyncCoordinator
) : ViewModel() {

    val status: StateFlow<SyncStatus> = syncStatusRepository.status

    /**
     * Fire-and-forget manual sync. The coordinator's mutex collapses overlapping
     * triggers, so a user mashing the button doesn't cause concurrent pushes —
     * the second call just observes the first one's result.
     */
    fun syncNow() {
        viewModelScope.launch { syncCoordinator.syncAll() }
    }
}
