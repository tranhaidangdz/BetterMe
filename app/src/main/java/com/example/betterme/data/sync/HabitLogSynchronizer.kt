package com.example.betterme.data.sync

import android.util.Log
import com.example.betterme.data.local.room.dao.HabitLogDao
import com.example.betterme.data.local.room.entities.HabitLogEntity
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

/**
 * Syncs daily habit check-in logs.
 *
 * Firestore layout: `users/{uid}/habit_logs/{habitId}_{date}` — composite doc id
 * mirrors the natural unique-per-day key `(habit_id, date)`. This guarantees
 * idempotent upload: two devices checking in on the same habit on the same day
 * land on the same doc id, and only one row exists post-sync.
 *
 * Push-only this iteration — habit logs are heavy-write, rarely-deleted, and the
 * single-device produces-and-uploads pattern is dominant. Bidirectional pull can
 * be added when the cross-device-restore flow is exercised.
 */
class HabitLogSynchronizer(
    private val habitLogDao: HabitLogDao,
    private val firestore: FirebaseFirestore
) : EntitySynchronizer {

    override val name: String = "HabitLog"

    override suspend fun dirtyCount(userId: String): Int = habitLogDao.countDirtyHabitLogs(userId)

    override suspend fun reconcile(userId: String): Boolean {
        val subcol = firestore.collection(USERS).document(userId).collection(HABIT_LOGS)
        val dirty = habitLogDao.getDirtyHabitLogs(userId)
        if (dirty.isEmpty()) return true

        var allOk = true
        for (log in dirty) {
            val docId = "${log.habit_id}_${log.date}"
            val ok = runCatching {
                subcol.document(docId).set(log.toFirestoreMap()).await()
                habitLogDao.markHabitLogSynced(log.id, log.updated_at, log.updated_at)
            }.onFailure { Log.w(TAG, "Push failed for habit_log id=${log.id} docId=$docId", it) }
                .isSuccess
            allOk = allOk && ok
        }
        return allOk
    }

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

    private companion object {
        const val TAG = "HabitLogSync"
        const val USERS = "users"
        const val HABIT_LOGS = "habit_logs"
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
