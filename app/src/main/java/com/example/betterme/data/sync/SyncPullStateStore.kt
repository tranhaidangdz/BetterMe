package com.example.betterme.data.sync

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

/**
 * Per-entity "last successful pull" timestamp store.
 *
 * Used by synchronizers that pull large append-only collections (`habit_logs`,
 * `challenge_logs`, `ai_chat`) to avoid re-fetching the entire history on
 * every sync pass. The synchronizer queries Firestore with
 * `whereGreaterThanOrEqualTo("updated_at", lastPullTs)` so only rows that
 * changed since the previous successful pull come down the wire.
 *
 * Storage: DataStore Preferences, one long key per entity. The key is
 * scoped by user id so signing in to a different account on the same device
 * does not leak the previous user's pull cursor.
 *
 * Semantics:
 *  - First pull (no value stored)              → returns 0L → fetch everything
 *  - Successful pull at server time T          → store T
 *  - Failed pull (no rows persisted)           → do not advance cursor
 *  - Sign-out / account switch                 → clear the key set
 *
 * After a successful pull the synchronizer calls [markPulled] with the largest
 * `updated_at` it observed in the result set. We deliberately store
 * `lastUpdatedAtSeen` rather than `System.currentTimeMillis()` to avoid wall
 * clock skew between the device and Firestore from missing rows in the next
 * delta query.
 */
class SyncPullStateStore(
    private val dataStore: DataStore<Preferences>
) {

    suspend fun getLastPulledAt(userId: String, entity: Entity): Long {
        if (userId.isBlank()) return 0L
        return runCatching {
            dataStore.data.map { it[key(userId, entity)] ?: 0L }.first()
        }.getOrDefault(0L)
    }

    suspend fun markPulled(userId: String, entity: Entity, lastUpdatedAtSeen: Long) {
        if (userId.isBlank() || lastUpdatedAtSeen <= 0L) return
        runCatching {
            dataStore.edit { prefs ->
                val current = prefs[key(userId, entity)] ?: 0L
                if (lastUpdatedAtSeen > current) {
                    prefs[key(userId, entity)] = lastUpdatedAtSeen
                }
            }
        }
    }

    /** Wipe every per-entity cursor — call on sign-out. */
    suspend fun clearAll(userId: String) {
        runCatching {
            dataStore.edit { prefs ->
                Entity.entries.forEach { e -> prefs.remove(key(userId, e)) }
            }
        }
    }

    private fun key(userId: String, entity: Entity) =
        longPreferencesKey("sync_pull_ts__${entity.storageKey}__$userId")

    enum class Entity(val storageKey: String) {
        HABIT_LOG("habit_log"),
        CHALLENGE_LOG("challenge_log"),
        AI_CHAT("ai_chat")
    }
}
