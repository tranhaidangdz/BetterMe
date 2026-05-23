package com.example.betterme.domain.sync

/**
 * Snapshot of the sync subsystem's current state. Surfaced as a [kotlinx.coroutines.flow.StateFlow]
 * by [SyncStatusRepository] so UI surfaces ("Đang đồng bộ…", "Ngoại tuyến — chờ kết nối")
 * can react reactively.
 */
data class SyncStatus(
    val state: State = State.IDLE,
    /**
     * Wall-clock of the last successful end-to-end sync (push + pull). Null means the
     * app has never finished a successful sync since install.
     */
    val lastSyncedAt: Long? = null,
    /**
     * Number of dirty rows queued for upload across all synced entity types. 0 means
     * the local store has reached Firestore.
     */
    val pendingCount: Int = 0,
    /** Last failure surface for the user. Null when the last attempt succeeded. */
    val lastError: String? = null
) {
    enum class State {
        /** No sync currently running and no pending work. */
        IDLE,

        /** A sync attempt is in progress (push + pull cycle running). */
        SYNCING,

        /** Network is unreachable; queued writes are waiting for connectivity. */
        OFFLINE,

        /** Last attempt failed. Pending work remains queued for retry. */
        ERROR
    }
}
