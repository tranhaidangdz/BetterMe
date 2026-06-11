package com.example.betterme.data.sync

import android.util.Log
import com.example.betterme.data.local.room.dao.HabitDao
import com.example.betterme.data.local.room.dao.HabitLogDao
import com.example.betterme.data.local.room.entities.HabitLogEntity
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await

/**
 * Bidirectional sync for daily habit check-in logs.
 *
 * Firestore layout: `users/{uid}/habit_logs/{habitId}_{date}` — composite doc id
 * mirrors the natural unique-per-day key `(habit_id, date)`. Two devices logging
 * the same habit on the same day land on the same doc id, so only one row exists
 * post-sync.
 *
 * Direction: bidirectional. Push uses a batched Firestore write (up to
 * [BATCH_LIMIT] per commit) so a user with hundreds of dirty rows after a long
 * offline run flushes in a small number of round-trips. Pull is incremental —
 * delta query against the per-user cursor in [SyncPullStateStore], so each
 * pass only downloads rows whose `updated_at` is newer than the last
 * successful pull. After a fresh install + first sign-in this fetches the
 * entire history; afterwards it's a small constant cost per pass.
 *
 * Conflict resolution: last-write-wins by `updated_at`. Soft-deletes propagate
 * via the `is_deleted` flag; we never hard-delete the remote doc so a device
 * coming back online weeks later still sees the tombstone.
 *
 * Foreign-key safety: the parent `habits` row must already exist locally
 * before we insert a remote log. [SyncCoordinator] runs HabitSynchronizer
 * before this one, so by the time we pull logs the parent habit ids are
 * resolvable. Any remote log whose habit was deleted everywhere is dropped.
 */
class HabitLogSynchronizer(
    private val habitLogDao: HabitLogDao,
    private val habitDao: HabitDao,
    private val firestore: FirebaseFirestore,
    private val pullState: SyncPullStateStore
) : EntitySynchronizer {

    override val name: String = "HabitLog"

    override suspend fun dirtyCount(userId: String): Int =
        habitLogDao.countDirtyHabitLogs(userId)

    override suspend fun reconcile(userId: String): Boolean {
        val subcol = firestore.collection(USERS).document(userId).collection(HABIT_LOGS)

        var pullOk = pull(userId, subcol)
        val pushOk = push(userId, subcol)
        return pullOk && pushOk
    }

    // ---------- PULL ----------
    private suspend fun pull(
        userId: String,
        subcol: com.google.firebase.firestore.CollectionReference
    ): Boolean {
        val lastPulledAt = pullState.getLastPulledAt(userId, SyncPullStateStore.Entity.HABIT_LOG)

        val snapshot = try {
            subcol
                .whereGreaterThanOrEqualTo(FIELD_UPDATED_AT, lastPulledAt)
                .orderBy(FIELD_UPDATED_AT, Query.Direction.ASCENDING)
                .get()
                .await()
        } catch (e: Exception) {
            Log.w(TAG, "Pull failed (lastPulledAt=$lastPulledAt)", e)
            return false
        }

        if (snapshot.documents.isEmpty()) return true

        var maxSeen = lastPulledAt
        for (snap in snapshot.documents) {
            val incoming = snap.toEntity() ?: continue

            // Parent habit must exist locally. Habits are synced before logs by
            // SyncCoordinator order, so this normally resolves; if not, drop.
            val parent = habitDao.getHabitById(incoming.habit_id)
            if (parent == null) {
                Log.d(TAG, "Drop orphan log: habit_id=${incoming.habit_id} not in Room")
                continue
            }

            val existing = habitLogDao.getLogByDate(incoming.habit_id, incoming.date)
            when {
                existing == null -> {
                    // New row from another device — insert with id=0 so Room
                    // assigns a fresh autoinc; mark synced immediately.
                    habitLogDao.insertLog(
                        incoming.copy(id = 0, synced_at = incoming.updated_at)
                    )
                }
                incoming.updated_at > existing.updated_at -> {
                    habitLogDao.updateLog(
                        existing.copy(
                            status = incoming.status,
                            note = incoming.note,
                            image = incoming.image,
                            latitude = incoming.latitude,
                            longitude = incoming.longitude,
                            updated_at = incoming.updated_at,
                            synced_at = incoming.updated_at,
                            is_deleted = incoming.is_deleted
                        )
                    )
                }
                // else: local is newer — push pass will handle.
            }

            if (incoming.updated_at > maxSeen) maxSeen = incoming.updated_at
        }

        pullState.markPulled(userId, SyncPullStateStore.Entity.HABIT_LOG, maxSeen)
        return true
    }

    // ---------- PUSH (batched) ----------
    private suspend fun push(
        userId: String,
        subcol: com.google.firebase.firestore.CollectionReference
    ): Boolean {
        val dirty = habitLogDao.getDirtyHabitLogs(userId)
        if (dirty.isEmpty()) return true

        var allOk = true
        for (chunk in dirty.chunked(BATCH_LIMIT)) {
            val batch = firestore.batch()
            for (row in chunk) {
                batch.set(subcol.document(docId(row)), row.toFirestoreMap())
            }
            val ok = runCatching { batch.commit().await() }
                .onFailure { Log.w(TAG, "Batch commit failed (size=${chunk.size})", it) }
                .isSuccess

            if (ok) {
                // Stamp synced_at only for rows in the committed batch. CAS by
                // updated_at protects against a concurrent edit during commit.
                for (row in chunk) {
                    habitLogDao.markHabitLogSynced(row.id, row.updated_at, row.updated_at)
                }
            } else {
                allOk = false
            }
        }
        return allOk
    }

    // ---------- mapping ----------
    private fun docId(row: HabitLogEntity) = "${row.habit_id}_${row.date}"

    private fun HabitLogEntity.toFirestoreMap(): Map<String, Any?> = mapOf(
        FIELD_HABIT_ID to habit_id,
        FIELD_DATE to date,
        FIELD_STATUS to status,
        FIELD_NOTE to note,
        FIELD_IMAGE to image,
        FIELD_CREATED_AT to created_at,
        FIELD_LATITUDE to latitude,
        FIELD_LONGITUDE to longitude,
        FIELD_UPDATED_AT to updated_at,
        FIELD_IS_DELETED to is_deleted
    )

    private fun DocumentSnapshot.toEntity(): HabitLogEntity? {
        if (!exists()) return null
        val habitId = (getLong(FIELD_HABIT_ID) ?: return null).toInt()
        val date = getLong(FIELD_DATE) ?: return null
        val updatedAt = getLong(FIELD_UPDATED_AT) ?: 0L
        return HabitLogEntity(
            id = 0,
            habit_id = habitId,
            date = date,
            status = getString(FIELD_STATUS) ?: "DONE",
            note = getString(FIELD_NOTE),
            image = getString(FIELD_IMAGE),
            created_at = getLong(FIELD_CREATED_AT) ?: System.currentTimeMillis(),
            latitude = getDouble(FIELD_LATITUDE),
            longitude = getDouble(FIELD_LONGITUDE),
            updated_at = updatedAt,
            synced_at = null,
            is_deleted = getBoolean(FIELD_IS_DELETED) ?: false
        )
    }

    private companion object {
        const val TAG = "HabitLogSync"
        const val USERS = "users"
        const val HABIT_LOGS = "habit_logs"

        /** Firestore hard limit is 500 ops per batch; we leave headroom for safety. */
        const val BATCH_LIMIT = 400

        const val FIELD_HABIT_ID = "habit_id"
        const val FIELD_DATE = "date"
        const val FIELD_STATUS = "status"
        const val FIELD_NOTE = "note"
        const val FIELD_IMAGE = "image"
        const val FIELD_CREATED_AT = "created_at"
        const val FIELD_LATITUDE = "latitude"
        const val FIELD_LONGITUDE = "longitude"
        const val FIELD_UPDATED_AT = "updated_at"
        const val FIELD_IS_DELETED = "is_deleted"
    }
}
