package com.example.betterme.data.sync

import android.util.Log
import com.example.betterme.data.local.room.dao.UserSettingsDao
import com.example.betterme.data.local.room.entities.UserSettingsEntity
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

/**
 * Syncs the per-user settings row: lifestyle profile + onboarding flags.
 *
 * Firestore layout: `users/{uid}/settings/default` — a single document per user.
 * The fixed doc id `default` keeps the subcollection trivially queryable without
 * a list operation.
 *
 * Bidirectional, last-write-wins via `updated_at`. Settings rarely conflict (one
 * row per user, mutated on form-submit) but the bidirectional path is essential
 * for the "sign in on a new device" flow: the user's onboarding flags + lifestyle
 * profile reach them instantly without re-onboarding.
 */
class UserSettingsSynchronizer(
    private val userSettingsDao: UserSettingsDao,
    private val firestore: FirebaseFirestore
) : EntitySynchronizer {

    override val name: String = "UserSettings"

    override suspend fun dirtyCount(userId: String): Int = userSettingsDao.countDirty(userId)

    override suspend fun reconcile(userId: String): Boolean {
        val docRef = firestore.collection(USERS).document(userId).collection(SETTINGS).document(DOC_ID)

        val remoteSnap = try {
            docRef.get().await()
        } catch (e: Exception) {
            Log.w(TAG, "Remote read failed for $userId", e)
            return false
        }
        val remoteUpdatedAt: Long? = if (remoteSnap.exists()) remoteSnap.getLong(FIELD_UPDATED_AT) else null

        val local = userSettingsDao.get(userId)
        val decision = LastWriteWins.decide(
            localUpdatedAt = local?.updated_at,
            localSyncedAt = local?.synced_at,
            remoteUpdatedAt = remoteUpdatedAt
        )

        return when (decision) {
            LastWriteWins.Decision.NoOp -> true

            LastWriteWins.Decision.InsertRemote -> {
                val incoming = remoteSnap.toUserSettingsEntity(userId) ?: return false
                userSettingsDao.upsert(incoming.copy(synced_at = incoming.updated_at))
                true
            }

            LastWriteWins.Decision.ApplyRemote -> {
                val incoming = remoteSnap.toUserSettingsEntity(userId) ?: return false
                userSettingsDao.upsert(incoming.copy(synced_at = incoming.updated_at))
                true
            }

            LastWriteWins.Decision.PushLocal -> {
                val toPush = local ?: return true
                runCatching {
                    docRef.set(toPush.toFirestoreMap()).await()
                    userSettingsDao.markSynced(userId, toPush.updated_at, toPush.updated_at)
                }.onFailure { Log.w(TAG, "Push failed for $userId", it) }.isSuccess
            }
        }
    }

    private fun UserSettingsEntity.toFirestoreMap(): Map<String, Any?> = mapOf(
        FIELD_USER_ID to user_id,
        FIELD_SLEEP_START to sleep_start,
        FIELD_SLEEP_END to sleep_end,
        FIELD_SLEEP_DURATION_TARGET_HOURS to sleep_duration_target_hours,
        FIELD_WORK_START to work_start,
        FIELD_WORK_END to work_end,
        FIELD_BREAKFAST to breakfast,
        FIELD_LUNCH to lunch,
        FIELD_DINNER to dinner,
        FIELD_ACTIVITY_LEVEL to activity_level,
        FIELD_HAS_SELECTED_HABITS to has_selected_habits,
        FIELD_IS_FIRST_TIME to is_first_time,
        FIELD_CREATED_AT to created_at,
        FIELD_UPDATED_AT to updated_at
    )

    private fun com.google.firebase.firestore.DocumentSnapshot.toUserSettingsEntity(uid: String): UserSettingsEntity? {
        if (!exists()) return null
        return UserSettingsEntity(
            user_id = uid,
            sleep_start = getString(FIELD_SLEEP_START) ?: "23:00",
            sleep_end = getString(FIELD_SLEEP_END) ?: "07:00",
            sleep_duration_target_hours = (getLong(FIELD_SLEEP_DURATION_TARGET_HOURS) ?: 8L).toInt(),
            work_start = getString(FIELD_WORK_START) ?: "08:30",
            work_end = getString(FIELD_WORK_END) ?: "17:30",
            breakfast = getString(FIELD_BREAKFAST) ?: "07:30",
            lunch = getString(FIELD_LUNCH) ?: "12:00",
            dinner = getString(FIELD_DINNER) ?: "18:30",
            activity_level = getString(FIELD_ACTIVITY_LEVEL) ?: "MODERATE",
            has_selected_habits = getBoolean(FIELD_HAS_SELECTED_HABITS) ?: false,
            is_first_time = getBoolean(FIELD_IS_FIRST_TIME) ?: true,
            created_at = getLong(FIELD_CREATED_AT) ?: System.currentTimeMillis(),
            updated_at = getLong(FIELD_UPDATED_AT) ?: 0L,
            synced_at = null
        )
    }

    private companion object {
        const val TAG = "UserSettingsSync"
        const val USERS = "users"
        const val SETTINGS = "settings"
        const val DOC_ID = "default"
        const val FIELD_USER_ID = "user_id"
        const val FIELD_SLEEP_START = "sleep_start"
        const val FIELD_SLEEP_END = "sleep_end"
        const val FIELD_SLEEP_DURATION_TARGET_HOURS = "sleep_duration_target_hours"
        const val FIELD_WORK_START = "work_start"
        const val FIELD_WORK_END = "work_end"
        const val FIELD_BREAKFAST = "breakfast"
        const val FIELD_LUNCH = "lunch"
        const val FIELD_DINNER = "dinner"
        const val FIELD_ACTIVITY_LEVEL = "activity_level"
        const val FIELD_HAS_SELECTED_HABITS = "has_selected_habits"
        const val FIELD_IS_FIRST_TIME = "is_first_time"
        const val FIELD_CREATED_AT = "created_at"
        const val FIELD_UPDATED_AT = "updated_at"
    }
}
