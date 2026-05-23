package com.example.betterme.data.sync

import android.util.Log
import com.example.betterme.data.local.room.dao.UserDao
import com.example.betterme.data.local.room.entities.UserEntity
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

/**
 * Syncs the signed-in user's profile row.
 *
 * Firestore layout: `users/{uid}` — a single document per user with the full
 * [UserEntity] field set plus `updated_at` for conflict resolution.
 *
 * Reconciliation strategy (last-write-wins by `updated_at`):
 *  1. Read the remote doc.
 *  2. If remote `updated_at` > local `updated_at`: apply remote to Room.
 *  3. If local is dirty and (local `updated_at` >= remote `updated_at` or remote absent):
 *     push local to Firestore. Stamp `synced_at` on success.
 *  4. Otherwise: no-op.
 */
class UserProfileSynchronizer(
    private val userDao: UserDao,
    private val firestore: FirebaseFirestore
) : EntitySynchronizer {

    override val name: String = "UserProfile"

    override suspend fun dirtyCount(userId: String): Int = userDao.countDirtyUsers(userId)

    override suspend fun reconcile(userId: String): Boolean {
        val local = userDao.getUserById(userId)
        val docRef = firestore.collection(USERS).document(userId)

        val remoteSnap = try {
            docRef.get().await()
        } catch (e: Exception) {
            Log.w(TAG, "Remote read failed for $userId", e)
            return false
        }
        val remoteUpdatedAt: Long = remoteSnap.getLong(FIELD_UPDATED_AT) ?: 0L

        if (local == null) {
            // No local row but remote may exist (e.g., fresh install on a returning user).
            if (remoteSnap.exists()) {
                val incoming = remoteSnap.toUserEntity(userId) ?: return false
                userDao.insertUser(incoming.copy(synced_at = remoteUpdatedAt))
            }
            return true
        }

        val localUpdatedAt = local.updated_at
        return when {
            remoteUpdatedAt > localUpdatedAt -> {
                // Remote wins. Apply with synced_at = remoteUpdatedAt so the row
                // immediately reads as clean from the local store.
                val incoming = remoteSnap.toUserEntity(userId) ?: return false
                userDao.updateUser(incoming.copy(synced_at = remoteUpdatedAt))
                true
            }
            local.synced_at == null || local.synced_at < localUpdatedAt -> {
                // Local is dirty (and at least as recent as remote). Push it.
                runCatching {
                    docRef.set(local.toFirestoreMap()).await()
                    userDao.markUserSynced(userId, localUpdatedAt, localUpdatedAt)
                }.onFailure { e -> Log.w(TAG, "Push failed for $userId", e) }
                    .isSuccess
            }
            else -> true
        }
    }

    private fun UserEntity.toFirestoreMap(): Map<String, Any?> = mapOf(
        FIELD_ID to id,
        FIELD_NAME to name,
        FIELD_EMAIL to email,
        FIELD_PHOTO_URL to photoUrl,
        FIELD_CREATED_AT to created_at,
        FIELD_COINS to coins,
        FIELD_LEVEL to level,
        FIELD_XP to xp,
        FIELD_UPDATED_AT to updated_at
    )

    private fun com.google.firebase.firestore.DocumentSnapshot.toUserEntity(uid: String): UserEntity? {
        if (!exists()) return null
        return UserEntity(
            id = uid,
            name = getString(FIELD_NAME).orEmpty(),
            email = getString(FIELD_EMAIL).orEmpty(),
            photoUrl = getString(FIELD_PHOTO_URL).orEmpty(),
            created_at = getLong(FIELD_CREATED_AT) ?: System.currentTimeMillis(),
            coins = (getLong(FIELD_COINS) ?: 0L).toInt(),
            level = (getLong(FIELD_LEVEL) ?: 1L).toInt(),
            xp = (getLong(FIELD_XP) ?: 0L).toInt(),
            updated_at = getLong(FIELD_UPDATED_AT) ?: 0L,
            synced_at = null // overwritten by caller
        )
    }

    private companion object {
        const val TAG = "UserProfileSync"
        const val USERS = "users"
        const val FIELD_ID = "id"
        const val FIELD_NAME = "name"
        const val FIELD_EMAIL = "email"
        const val FIELD_PHOTO_URL = "photoUrl"
        const val FIELD_CREATED_AT = "created_at"
        const val FIELD_COINS = "coins"
        const val FIELD_LEVEL = "level"
        const val FIELD_XP = "xp"
        const val FIELD_UPDATED_AT = "updated_at"
    }
}
