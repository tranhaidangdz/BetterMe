package com.example.betterme.data.sync

import android.util.Log
import com.example.betterme.data.local.room.dao.ChallengeLogDao
import com.example.betterme.data.local.room.dao.UserChallengeDao
import com.example.betterme.data.local.room.entities.ChallengeLogEntity
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await

/**
 * Bidirectional sync for daily challenge check-in logs.
 *
 * Firestore layout: `users/{uid}/challenge_logs/{challengeId}_{date}` —
 * composite doc id using the **stable** `challenge_id` (from the seeded
 * catalog) and the day-start millis. We deliberately do not use the local
 * `user_challenge_id` (autoinc, per-device) because then the same logical
 * check-in would be a different doc on each device.
 *
 * Direction: bidirectional, batched push, incremental pull (delta query against
 * the cursor in [SyncPullStateStore]). Same shape as [HabitLogSynchronizer]
 * but with one extra resolution step: when pulling, the doc carries
 * `challenge_id` (stable) which we map to the local `user_challenge_id`
 * (autoinc) before insert. That lookup goes through [UserChallengeDao];
 * if the user's UserChallenge row does not exist locally yet (sync order
 * places UserChallenges before logs, so this is rare) we drop the log
 * rather than violate the FK.
 *
 * Conflict resolution: last-write-wins by `updated_at`. Soft deletes propagate
 * via `is_deleted`.
 */
class ChallengeLogSynchronizer(
    private val challengeLogDao: ChallengeLogDao,
    private val userChallengeDao: UserChallengeDao,
    private val firestore: FirebaseFirestore,
    private val pullState: SyncPullStateStore
) : EntitySynchronizer {

    override val name: String = "ChallengeLog"

    override suspend fun dirtyCount(userId: String): Int =
        challengeLogDao.countDirtyLogs(userId)

    override suspend fun reconcile(userId: String): Boolean {
        val subcol = firestore.collection(USERS).document(userId).collection(CHALLENGE_LOGS)
        val pullOk = pull(userId, subcol)
        val pushOk = push(userId, subcol)
        return pullOk && pushOk
    }

    // ---------- PULL ----------
    private suspend fun pull(
        userId: String,
        subcol: com.google.firebase.firestore.CollectionReference
    ): Boolean {
        val lastPulledAt = pullState.getLastPulledAt(userId, SyncPullStateStore.Entity.CHALLENGE_LOG)

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
            val parsed = snap.toLogPlus() ?: continue
            val (incoming, challengeId) = parsed

            // Resolve stable challenge_id → local user_challenge_id (autoinc).
            // UserChallenges are synced before logs by SyncCoordinator order.
            val uc = userChallengeDao.getByUserAndChallenge(userId, challengeId)
            if (uc == null) {
                Log.d(TAG, "Drop orphan log: no UserChallenge for challenge_id=$challengeId")
                continue
            }

            val existing = challengeLogDao.getLogByDate(uc.id, incoming.date)
            when {
                existing == null -> {
                    challengeLogDao.insert(
                        incoming.copy(
                            id = 0,
                            user_challenge_id = uc.id,
                            synced_at = incoming.updated_at
                        )
                    )
                }
                incoming.updated_at > existing.updated_at -> {
                    challengeLogDao.update(
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
            }

            if (incoming.updated_at > maxSeen) maxSeen = incoming.updated_at
        }

        pullState.markPulled(userId, SyncPullStateStore.Entity.CHALLENGE_LOG, maxSeen)
        return true
    }

    // ---------- PUSH (batched) ----------
    private suspend fun push(
        userId: String,
        subcol: com.google.firebase.firestore.CollectionReference
    ): Boolean {
        val dirty = challengeLogDao.getDirtyLogs(userId)
        if (dirty.isEmpty()) return true

        // Resolve user_challenge_id → challenge_id for the doc id + payload.
        // Pre-fetch parents to avoid N round-trips through the DAO during commit.
        val parentByUcId = HashMap<Int, Int>()  // local uc.id → stable challenge_id
        for (row in dirty) {
            if (parentByUcId.containsKey(row.user_challenge_id)) continue
            val parent = userChallengeDao.getById(row.user_challenge_id)
            if (parent != null) parentByUcId[row.user_challenge_id] = parent.challenge_id
        }

        var allOk = true
        for (chunk in dirty.chunked(BATCH_LIMIT)) {
            val batch = firestore.batch()
            val committable = mutableListOf<ChallengeLogEntity>()
            for (row in chunk) {
                val challengeId = parentByUcId[row.user_challenge_id]
                if (challengeId == null) {
                    Log.w(TAG, "Skip orphan log id=${row.id} uc_id=${row.user_challenge_id}")
                    continue
                }
                batch.set(
                    subcol.document("${challengeId}_${row.date}"),
                    row.toFirestoreMap(challengeId)
                )
                committable += row
            }
            if (committable.isEmpty()) continue

            val ok = runCatching { batch.commit().await() }
                .onFailure { Log.w(TAG, "Batch commit failed (size=${committable.size})", it) }
                .isSuccess
            if (ok) {
                for (row in committable) {
                    challengeLogDao.markLogSynced(row.id, row.updated_at, row.updated_at)
                }
            } else {
                allOk = false
            }
        }
        return allOk
    }

    // ---------- mapping ----------
    private fun ChallengeLogEntity.toFirestoreMap(challengeId: Int): Map<String, Any?> = mapOf(
        FIELD_CHALLENGE_ID to challengeId,
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

    /** Returns (entity-with-zero-local-id, stable challenge_id from the doc). */
    private fun DocumentSnapshot.toLogPlus(): Pair<ChallengeLogEntity, Int>? {
        if (!exists()) return null
        val challengeId = (getLong(FIELD_CHALLENGE_ID) ?: return null).toInt()
        val date = getLong(FIELD_DATE) ?: return null
        val log = ChallengeLogEntity(
            id = 0,
            user_challenge_id = 0,                  // resolved by caller
            date = date,
            status = getString(FIELD_STATUS) ?: "DONE",
            note = getString(FIELD_NOTE),
            image = getString(FIELD_IMAGE),
            created_at = getLong(FIELD_CREATED_AT) ?: System.currentTimeMillis(),
            latitude = getDouble(FIELD_LATITUDE),
            longitude = getDouble(FIELD_LONGITUDE),
            updated_at = getLong(FIELD_UPDATED_AT) ?: 0L,
            synced_at = null,
            is_deleted = getBoolean(FIELD_IS_DELETED) ?: false
        )
        return log to challengeId
    }

    private companion object {
        const val TAG = "ChallengeLogSync"
        const val USERS = "users"
        const val CHALLENGE_LOGS = "challenge_logs"
        const val BATCH_LIMIT = 400

        const val FIELD_CHALLENGE_ID = "challenge_id"
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
