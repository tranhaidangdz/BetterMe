package com.example.betterme.data.sync

/**
 * Two-way sync strategy for a single entity type.
 *
 * Implementations own:
 *  - the mapping between the local Room shape and the remote Firestore document shape
 *  - the dirty-row query (`synced_at < updated_at` OR `synced_at IS NULL`)
 *  - per-row upload (`set(...)`) including soft-delete propagation via `is_deleted`
 *  - the remote pull (Firestore read of all docs owned by the current user)
 *  - last-write-wins merge on conflict (whichever side has the larger `updated_at`)
 *
 * The coordinator runs `reconcile(userId)` on every sync pass. Returning false from
 * `reconcile` signals a transient failure (network blip, transaction collision) so
 * the worker can retry.
 */
interface EntitySynchronizer {
    val name: String

    /**
     * Count of rows currently dirty (locally modified, not yet pushed). Used to
     * surface the pending-count badge in [SyncStatus].
     */
    suspend fun dirtyCount(userId: String): Int

    /**
     * Run one push+pull cycle for the user. Implementations should be defensive —
     * a thrown exception fails this synchronizer but not the whole sync pass; the
     * coordinator catches and continues with the next synchronizer.
     *
     * @return true on full success, false on partial failure (some rows synced,
     * others to retry next pass).
     */
    suspend fun reconcile(userId: String): Boolean
}
