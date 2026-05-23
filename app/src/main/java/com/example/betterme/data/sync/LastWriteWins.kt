package com.example.betterme.data.sync

/**
 * Pure decision helpers for last-write-wins reconciliation.
 *
 * Both sides carry an `updated_at` (millis since epoch). The newer side wins; ties
 * go to the local side so a quick sync immediately after a local edit doesn't
 * spuriously demote the local change. The local side is also considered dirty
 * (eligible for push) when its `synced_at` is null OR strictly less than its
 * `updated_at`.
 *
 * Kept in its own file so unit tests can cover the matrix without touching
 * Firestore, Room, or coroutines.
 */
object LastWriteWins {

    /**
     * Result of comparing a local row's `(updated_at, synced_at)` with a remote
     * row's `updated_at`.
     */
    sealed class Decision {
        /** Local is dirty and at least as recent as remote — push it. */
        data object PushLocal : Decision()

        /** Remote is newer than local — apply remote to Room. */
        data object ApplyRemote : Decision()

        /** Local and remote agree (or remote absent and local clean) — no-op. */
        data object NoOp : Decision()

        /** Remote exists but local doesn't — insert remote into Room. */
        data object InsertRemote : Decision()
    }

    /**
     * Decide what to do for one (local, remote) pair. `remoteUpdatedAt = null` means
     * the remote document doesn't exist (e.g., fresh install, never synced).
     * `localUpdatedAt = null` means there's no local row.
     */
    fun decide(
        localUpdatedAt: Long?,
        localSyncedAt: Long?,
        remoteUpdatedAt: Long?
    ): Decision {
        // Neither side exists — nothing to do.
        if (localUpdatedAt == null && remoteUpdatedAt == null) return Decision.NoOp

        // Only remote exists — pull it in.
        if (localUpdatedAt == null) return Decision.InsertRemote

        // Only local exists — push it (unconditionally; first upload).
        if (remoteUpdatedAt == null) return Decision.PushLocal

        // Both exist. Resolve by updated_at.
        return when {
            remoteUpdatedAt > localUpdatedAt -> Decision.ApplyRemote
            // Local is dirty and at least as new as remote — push.
            localSyncedAt == null || localSyncedAt < localUpdatedAt -> Decision.PushLocal
            // Local is clean and == remote — already converged.
            else -> Decision.NoOp
        }
    }
}
