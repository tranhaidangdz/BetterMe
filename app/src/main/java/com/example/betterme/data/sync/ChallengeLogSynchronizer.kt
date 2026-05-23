package com.example.betterme.data.sync

import android.util.Log
import com.example.betterme.data.local.room.dao.ChallengeLogDao
import com.example.betterme.data.local.room.dao.UserChallengeDao
import com.example.betterme.data.local.room.entities.ChallengeLogEntity
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

/**
 * Syncs daily check-in logs.
 *
 * Firestore layout: `users/{uid}/challenge_logs/{challengeId}_{date}` — composite
 * doc id using the **stable challenge_id** and the day-start millis. Matches the
 * local unique index on `(user_challenge_id, date)` but reframed against the
 * stable challenge_id so the doc id is portable across devices.
 *
 * One pass is **push-only**: this synchronizer is upload-focused because logs are
 * the heaviest growing table and full bidirectional pull on every cycle is
 * expensive. A later iteration can add per-user pull (e.g., on first device boot
 * to seed history); for now, logs are produced on a single device per check-in
 * and rarely conflict.
 *
 * The local row's `user_challenge_id` is a Room autoincrement that varies per
 * device, so the upload payload carries `challenge_id` (resolved via the parent
 * row) instead. This keeps the Firestore document idempotent across reinstalls.
 */
class ChallengeLogSynchronizer(
    private val challengeLogDao: ChallengeLogDao,
    private val userChallengeDao: UserChallengeDao,
    private val firestore: FirebaseFirestore
) : EntitySynchronizer {

    override val name: String = "ChallengeLog"

    override suspend fun dirtyCount(userId: String): Int = challengeLogDao.countDirtyLogs(userId)

    override suspend fun reconcile(userId: String): Boolean {
        val subcol = firestore.collection(USERS).document(userId).collection(CHALLENGE_LOGS)
        val dirty = challengeLogDao.getDirtyLogs(userId)
        if (dirty.isEmpty()) return true

        var allOk = true
        for (log in dirty) {
            // Resolve the stable challenge_id from the parent user_challenge row.
            // If the parent disappeared (e.g., user abandoned and the row was
            // hard-deleted before we ever soft-delete), skip this orphan log.
            val parent = userChallengeDao.getById(log.user_challenge_id)
            if (parent == null) {
                Log.w(TAG, "Orphan log id=${log.id} — parent uc_id=${log.user_challenge_id} missing")
                continue
            }
            val docId = "${parent.challenge_id}_${log.date}"

            val ok = runCatching {
                subcol.document(docId).set(log.toFirestoreMap(parent.challenge_id)).await()
                challengeLogDao.markLogSynced(log.id, log.updated_at, log.updated_at)
            }.onFailure { e ->
                Log.w(TAG, "Push failed for log id=${log.id} docId=$docId", e)
            }.isSuccess
            allOk = allOk && ok
        }
        return allOk
    }

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

    private companion object {
        const val TAG = "ChallengeLogSync"
        const val USERS = "users"
        const val CHALLENGE_LOGS = "challenge_logs"
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
