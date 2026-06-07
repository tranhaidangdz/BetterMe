package com.example.betterme.data.sync

import android.util.Log
import com.example.betterme.data.local.room.dao.UserCategoryDao
import com.example.betterme.data.local.room.entities.UserCategoryEntity
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

/**
 * Syncs the user's interest-category selections (`user_categories` ↔
 * `users/{uid}/user_categories/{category_id}`).
 *
 * Doc id is the stable global `category_id` (CategoryEntity is seeded identically on
 * every device), so a user picking "Sức khoẻ" on Device A converges to the same
 * document on Device B with no dedupe gymnastics. The local autoincrement `id` is
 * irrelevant remotely.
 *
 * Soft-deletes propagate via `is_deleted`. Last-write-wins by `updated_at`.
 */
class UserCategorySynchronizer(
    private val dao: UserCategoryDao,
    private val firestore: FirebaseFirestore
) : EntitySynchronizer {

    override val name: String = "UserCategory"

    override suspend fun dirtyCount(userId: String): Int = dao.countDirty(userId)

    override suspend fun reconcile(userId: String): Boolean {
        val subcol = firestore.collection(USERS).document(userId).collection(USER_CATEGORIES)

        val remoteDocs = try {
            subcol.get().await().documents
        } catch (e: Exception) {
            Log.w(TAG, "Remote read failed", e)
            return false
        }

        // Pull pass.
        for (snap in remoteDocs) {
            val incoming = snap.toEntity(userId) ?: continue
            val local = dao.find(userId, incoming.category_id)
            if (local == null) {
                dao.insert(
                    incoming.copy(
                        id = 0,
                        synced_at = incoming.updated_at
                    )
                )
                continue
            }
            if (incoming.updated_at > local.updated_at) {
                dao.update(
                    local.copy(
                        created_at = incoming.created_at,
                        updated_at = incoming.updated_at,
                        synced_at = incoming.updated_at,
                        is_deleted = incoming.is_deleted
                    )
                )
            }
        }

        // Push pass.
        val remoteByCategory = remoteDocs.associateBy { it.id.toIntOrNull() ?: -1 }
        val dirty = dao.getDirty(userId)
        var allOk = true
        for (row in dirty) {
            val remote = remoteByCategory[row.category_id]
            val remoteUpdatedAt = remote?.getLong(FIELD_UPDATED_AT) ?: 0L
            if (row.updated_at < remoteUpdatedAt) continue
            val ok = runCatching {
                subcol.document(row.category_id.toString())
                    .set(row.toFirestoreMap())
                    .await()
                dao.markSynced(row.id, row.updated_at, row.updated_at)
            }.onFailure {
                Log.w(TAG, "Push failed for user_category cat=${row.category_id}", it)
            }.isSuccess
            allOk = allOk && ok
        }
        return allOk
    }

    private fun UserCategoryEntity.toFirestoreMap(): Map<String, Any?> = mapOf(
        FIELD_CATEGORY_ID to category_id,
        FIELD_USER_ID to user_id,
        FIELD_CREATED_AT to created_at,
        FIELD_UPDATED_AT to updated_at,
        FIELD_IS_DELETED to is_deleted
    )

    private fun DocumentSnapshot.toEntity(uid: String): UserCategoryEntity? {
        if (!exists()) return null
        val categoryId = (getLong(FIELD_CATEGORY_ID) ?: id.toLongOrNull())?.toInt() ?: return null
        val createdAt = getLong(FIELD_CREATED_AT) ?: System.currentTimeMillis()
        return UserCategoryEntity(
            id = 0,
            user_id = uid,
            category_id = categoryId,
            created_at = createdAt,
            updated_at = getLong(FIELD_UPDATED_AT) ?: createdAt,
            synced_at = null,
            is_deleted = getBoolean(FIELD_IS_DELETED) ?: false
        )
    }

    private companion object {
        const val TAG = "UserCategorySync"
        const val USERS = "users"
        const val USER_CATEGORIES = "user_categories"
        const val FIELD_CATEGORY_ID = "category_id"
        const val FIELD_USER_ID = "user_id"
        const val FIELD_CREATED_AT = "created_at"
        const val FIELD_UPDATED_AT = "updated_at"
        const val FIELD_IS_DELETED = "is_deleted"
    }
}
