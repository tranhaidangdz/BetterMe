package com.example.betterme.data.leaderboard

import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.first
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Persists the previous-read rank for each `(challengeId, seasonKey,
 * userId)` so the next read can compute a [com.example.betterme.domain.leaderboard.RankDelta].
 *
 * Storage choice: DataStore Preferences with one JSON-encoded entry
 * per `(challengeId, seasonKey)` keyed on a composite string. The
 * payload is `Map<userId, rank>` so we can compute deltas for ALL
 * visible entries, not just the current user — that's what gives the
 * full list its ↑/↓ chips, not just the user's row.
 *
 * Why not Room: snapshots are ephemeral (we don't need queries, joins,
 * or migrations) and Preferences gives us a single-file lookup keyed by
 * a composite string with zero schema overhead. Worst-case data loss
 * just means one cycle of `New` deltas, which is fine.
 *
 * Concurrency: DataStore handles edit serialization. We do NOT need
 * additional locking around the read-modify-write pattern in [save].
 */
class RankSnapshotStore(
    private val dataStore: DataStore<Preferences>
) {

    /**
     * Read the previous-snapshot `Map<userId, rank>` for one
     * `(challengeId, seasonKey)`. Returns an empty map when nothing
     * was persisted (first ever read for this combo).
     */
    suspend fun load(challengeId: Int, seasonKey: String): Map<String, Int> {
        val key = stringPreferencesKey(prefKey(challengeId, seasonKey))
        return try {
            val raw = dataStore.data.first()[key] ?: return emptyMap()
            json.decodeFromString<Map<String, Int>>(raw)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to decode rank snapshot for $challengeId/$seasonKey", e)
            emptyMap()
        }
    }

    /**
     * Persist the current snapshot's `Map<userId, rank>` so the next
     * read can compute deltas against it. Overwrites the previous
     * value — we only ever need the most recent snapshot.
     */
    suspend fun save(challengeId: Int, seasonKey: String, ranks: Map<String, Int>) {
        val key = stringPreferencesKey(prefKey(challengeId, seasonKey))
        try {
            dataStore.edit { prefs ->
                prefs[key] = json.encodeToString(ranks)
            }
        } catch (e: Exception) {
            // Swallow — snapshot persistence is best-effort. Next read
            // just sees "New" deltas for everything, which is benign.
            Log.w(TAG, "Failed to persist rank snapshot for $challengeId/$seasonKey", e)
        }
    }

    private fun prefKey(challengeId: Int, seasonKey: String): String =
        "rank_snapshot__${challengeId}__$seasonKey"

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    private companion object {
        const val TAG = "RankSnapshotStore"
    }

    /**
     * Helper for callers that hold a Map<userId, rank> in a more
     * structured shape. Kept here so the (de)serialization stays close
     * to the store that owns it.
     */
    @Serializable
    data class StoredRank(val userId: String, val rank: Int)
}
