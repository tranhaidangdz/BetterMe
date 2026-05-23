package com.example.betterme.data.sync

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Edge-case coverage for the strict last-write-wins decision used by every
 * synchronizer. Each test maps to a concrete scenario from the sync spec:
 *
 *  - offline edits (synced_at < updated_at)
 *  - reconnect conflicts (both sides have a non-null updated_at)
 *  - duplicate prevention (clean local + clean remote with same updated_at → NoOp)
 *  - logout/login restore (no local + remote exists → InsertRemote)
 *  - reinstall recovery (no local + remote exists → InsertRemote on first sync)
 *  - multi-device overwrite (newer remote → ApplyRemote; newer local → PushLocal)
 *  - clean local, no remote (first upload → PushLocal)
 *  - identical sides clean (NoOp)
 *  - completely empty (neither side exists → NoOp)
 */
class LastWriteWinsTest {

    // ============================================================
    // Offline edits — local mutated while disconnected
    // ============================================================

    @Test
    fun `offline edit on fresh row pushes when reconnecting`() {
        // Local row never synced (synced_at = null), remote doesn't exist yet.
        val d = LastWriteWins.decide(
            localUpdatedAt = 1000L,
            localSyncedAt = null,
            remoteUpdatedAt = null
        )
        assertEquals(LastWriteWins.Decision.PushLocal, d)
    }

    @Test
    fun `offline edit on previously-synced row pushes`() {
        // synced_at < updated_at: local was edited after last upload, no remote change since.
        val d = LastWriteWins.decide(
            localUpdatedAt = 2000L,
            localSyncedAt = 1000L,
            remoteUpdatedAt = 1000L
        )
        assertEquals(LastWriteWins.Decision.PushLocal, d)
    }

    // ============================================================
    // Reconnect conflicts — both sides mutated while one was offline
    // ============================================================

    @Test
    fun `concurrent edit with newer remote applies remote`() {
        // Device A edited locally to 1500 but never synced; meanwhile Device B
        // pushed an edit at 2000 to Firestore. A reconnects.
        val d = LastWriteWins.decide(
            localUpdatedAt = 1500L,
            localSyncedAt = 1000L,
            remoteUpdatedAt = 2000L
        )
        assertEquals(LastWriteWins.Decision.ApplyRemote, d)
    }

    @Test
    fun `concurrent edit with newer local pushes local`() {
        // Symmetric: local is the more recent edit.
        val d = LastWriteWins.decide(
            localUpdatedAt = 2000L,
            localSyncedAt = 1000L,
            remoteUpdatedAt = 1500L
        )
        assertEquals(LastWriteWins.Decision.PushLocal, d)
    }

    @Test
    fun `tie on updated_at with dirty local keeps local`() {
        // If both sides have the same updated_at but local is still dirty
        // (just hasn't acked yet), push local — never demote local work without a
        // strictly newer remote.
        val d = LastWriteWins.decide(
            localUpdatedAt = 2000L,
            localSyncedAt = 1000L,
            remoteUpdatedAt = 2000L
        )
        assertEquals(LastWriteWins.Decision.PushLocal, d)
    }

    // ============================================================
    // Duplicate prevention — already converged
    // ============================================================

    @Test
    fun `clean local matching remote is a noop`() {
        // synced_at == updated_at == remote.updated_at → fully converged.
        val d = LastWriteWins.decide(
            localUpdatedAt = 2000L,
            localSyncedAt = 2000L,
            remoteUpdatedAt = 2000L
        )
        assertEquals(LastWriteWins.Decision.NoOp, d)
    }

    @Test
    fun `clean local older than remote applies remote`() {
        // Clean local but remote bumped — pull.
        val d = LastWriteWins.decide(
            localUpdatedAt = 1000L,
            localSyncedAt = 1000L,
            remoteUpdatedAt = 2000L
        )
        assertEquals(LastWriteWins.Decision.ApplyRemote, d)
    }

    // ============================================================
    // Logout / login restore — local empty, remote exists
    // ============================================================

    @Test
    fun `signing in on a new device pulls remote`() {
        val d = LastWriteWins.decide(
            localUpdatedAt = null,
            localSyncedAt = null,
            remoteUpdatedAt = 5000L
        )
        assertEquals(LastWriteWins.Decision.InsertRemote, d)
    }

    // ============================================================
    // Reinstall recovery — local wiped, remote intact
    // ============================================================

    @Test
    fun `reinstalled app with previously-synced data restores from remote`() {
        // Same shape as login restore — after reinstall there is no local row.
        val d = LastWriteWins.decide(
            localUpdatedAt = null,
            localSyncedAt = null,
            remoteUpdatedAt = 10_000L
        )
        assertEquals(LastWriteWins.Decision.InsertRemote, d)
    }

    // ============================================================
    // Multi-device overwrite — explicit "newer remote wins" path
    // ============================================================

    @Test
    fun `device A clean state gets overwritten by device B newer push`() {
        // A previously synced at 1000; B then pushed at 2000. A's next sync pulls.
        val d = LastWriteWins.decide(
            localUpdatedAt = 1000L,
            localSyncedAt = 1000L,
            remoteUpdatedAt = 2000L
        )
        assertEquals(LastWriteWins.Decision.ApplyRemote, d)
    }

    // ============================================================
    // First upload — clean local, no remote yet
    // ============================================================

    @Test
    fun `first ever upload of a clean local row still pushes`() {
        // synced_at == updated_at but remote doesn't exist; sentinel for fresh
        // users where the local row was created and immediately synced once
        // earlier-in-the-session but the Firestore document hadn't been written.
        val d = LastWriteWins.decide(
            localUpdatedAt = 1000L,
            localSyncedAt = 1000L,
            remoteUpdatedAt = null
        )
        assertEquals(LastWriteWins.Decision.PushLocal, d)
    }

    // ============================================================
    // Completely empty
    // ============================================================

    @Test
    fun `no local and no remote is a noop`() {
        val d = LastWriteWins.decide(
            localUpdatedAt = null,
            localSyncedAt = null,
            remoteUpdatedAt = null
        )
        assertEquals(LastWriteWins.Decision.NoOp, d)
    }
}
