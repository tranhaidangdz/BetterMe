package com.example.betterme.data.sync

import android.util.Log
import com.example.betterme.data.local.room.dao.HabitDao
import com.example.betterme.data.local.room.entities.HabitEntity
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

/**
 * Syncs habit definitions.
 *
 * Firestore layout: `users/{uid}/habits/{habitId}` — uses the local Room
 * autoincrement id as the doc id. Unlike challenges (which have stable seeded ids),
 * habits are user-created so their ids are inherently per-device. The synchronizer
 * matches strictly by id; cross-device duplicates would require explicit dedupe
 * by title+start_date, which is outside scope for last-write-wins.
 *
 * The trade-off: a user signing in on Device B after creating "Drink water" on
 * Device A will pull A's habits with their original ids preserved on B. New habits
 * created on B before sync arrives will get fresh local ids; on next sync, A pulls
 * those rows. Same titles on both devices coexist as separate rows — the user can
 * manually clean up. Acceptable for this iteration.
 *
 * Soft-deletes propagate via `is_deleted`.
 */
class HabitSynchronizer(
    private val habitDao: HabitDao,
    private val firestore: FirebaseFirestore
) : EntitySynchronizer {

    override val name: String = "Habit"

    override suspend fun dirtyCount(userId: String): Int = habitDao.countDirtyHabits(userId)

    override suspend fun reconcile(userId: String): Boolean {
        val subcol = firestore.collection(USERS).document(userId).collection(HABITS)

        val remoteDocs = try {
            subcol.get().await().documents
        } catch (e: Exception) {
            Log.w(TAG, "Remote read failed", e)
            return false
        }

        val remoteById: Map<Int, com.google.firebase.firestore.DocumentSnapshot> =
            remoteDocs.associateBy { it.id.toIntOrNull() ?: -1 }.filterKeys { it > 0 }

        // Pull: apply remote rows that beat (or have no) local counterpart.
        for ((habitId, snap) in remoteById) {
            val incoming = snap.toHabitEntity(userId) ?: continue
            val local = habitDao.getHabitById(habitId)
            if (local == null) {
                habitDao.insertHabit(incoming.copy(id = habitId, synced_at = incoming.updated_at))
                continue
            }
            if (incoming.updated_at > local.updated_at) {
                habitDao.updateHabit(incoming.copy(id = habitId, synced_at = incoming.updated_at))
            }
        }

        // Push: send every dirty local row whose updated_at >= remote.
        val dirty = habitDao.getDirtyHabits(userId)
        var allOk = true
        for (row in dirty) {
            val remote = remoteById[row.id]
            val remoteUpdatedAt = remote?.getLong(FIELD_UPDATED_AT) ?: 0L
            if (row.updated_at < remoteUpdatedAt) continue
            val ok = runCatching {
                subcol.document(row.id.toString()).set(row.toFirestoreMap()).await()
                habitDao.markHabitSynced(row.id, row.updated_at, row.updated_at)
            }.onFailure { Log.w(TAG, "Push failed for habit id=${row.id}", it) }.isSuccess
            allOk = allOk && ok
        }
        return allOk
    }

    private fun HabitEntity.toFirestoreMap(): Map<String, Any?> = mapOf(
        FIELD_ID to id,
        FIELD_USER_ID to user_id,
        FIELD_CATEGORY_ID to category_id,
        FIELD_TITLE to title,
        FIELD_DESCRIPTION to description,
        FIELD_START_DATE to start_date,
        FIELD_END_DATE to end_date,
        FIELD_REMINDER_TIME to reminder_time,
        FIELD_CREATED_AT to created_at,
        FIELD_UPDATED_AT to updated_at,
        FIELD_IS_DELETED to is_deleted
    )

    private fun com.google.firebase.firestore.DocumentSnapshot.toHabitEntity(uid: String): HabitEntity? {
        if (!exists()) return null
        return HabitEntity(
            id = (getLong(FIELD_ID) ?: 0L).toInt(),
            user_id = uid,
            category_id = getLong(FIELD_CATEGORY_ID)?.toInt(),
            title = getString(FIELD_TITLE).orEmpty(),
            description = getString(FIELD_DESCRIPTION),
            start_date = getLong(FIELD_START_DATE) ?: System.currentTimeMillis(),
            end_date = getLong(FIELD_END_DATE),
            reminder_time = getString(FIELD_REMINDER_TIME),
            created_at = getLong(FIELD_CREATED_AT) ?: System.currentTimeMillis(),
            updated_at = getLong(FIELD_UPDATED_AT) ?: 0L,
            synced_at = null,
            is_deleted = getBoolean(FIELD_IS_DELETED) ?: false
        )
    }

    private companion object {
        const val TAG = "HabitSync"
        const val USERS = "users"
        const val HABITS = "habits"
        const val FIELD_ID = "id"
        const val FIELD_USER_ID = "user_id"
        const val FIELD_CATEGORY_ID = "category_id"
        const val FIELD_TITLE = "title"
        const val FIELD_DESCRIPTION = "description"
        const val FIELD_START_DATE = "start_date"
        const val FIELD_END_DATE = "end_date"
        const val FIELD_REMINDER_TIME = "reminder_time"
        const val FIELD_CREATED_AT = "created_at"
        const val FIELD_UPDATED_AT = "updated_at"
        const val FIELD_IS_DELETED = "is_deleted"
    }
}
