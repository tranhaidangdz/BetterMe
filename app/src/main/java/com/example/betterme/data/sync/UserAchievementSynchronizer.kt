package com.example.betterme.data.sync

import android.util.Log
import com.example.betterme.data.local.room.dao.UserAchievementDao
import com.example.betterme.data.local.room.entities.UserAchievementEntity
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

/**
 * Syncs earned badges (`user_achievements` ↔ `users/{uid}/badges/{achievement_id}`).
 *
 * Design notes:
 *  - Doc id is the stable `achievement_id` (matches the global `achievements` catalog),
 *    not the local autoincrement `id`. Achievements are seeded identically on every
 *    device, so the foreign key resolves cross-device. Cross-device dedupe is implicit:
 *    the same badge from two devices collapses to the same Firestore doc.
 *  - Local Room enforces `UNIQUE(user_id, achievement_id)` so the IGNORE insert
 *    strategy on pull-side never duplicates rows.
 *  - Last-write-wins by `updated_at`. Soft-deletes (`is_deleted=true`) propagate so
 *    a badge revoked on one device disappears on the other after sync.
 *
 * Throughput: badge unlocks are bursty during demo / onboarding (40 badges in
 * seconds) but the steady state is small (a few per week). The full pull-then-push
 * pass per pass remains cheap.
 */
class UserAchievementSynchronizer(
    private val dao: UserAchievementDao,
    private val firestore: FirebaseFirestore
) : EntitySynchronizer {

    override val name: String = "UserAchievement"

    override suspend fun dirtyCount(userId: String): Int = dao.countDirty(userId)

    override suspend fun reconcile(userId: String): Boolean {
        val subcol = firestore.collection(USERS).document(userId).collection(BADGES)

        val remoteDocs = try {
            subcol.get().await().documents
        } catch (e: Exception) {
            Log.w(TAG, "Remote read failed", e)
            return false
        }

        // Pull: apply remote rows that the local copy doesn't have or that beat it.
        for (snap in remoteDocs) {
            val incoming = snap.toEntity(userId) ?: continue
            val local = dao.findByAchievement(userId, incoming.achievement_id)
            if (local == null) {
                dao.insert(
                    incoming.copy(
                        id = 0, // let Room assign a fresh local id
                        synced_at = incoming.updated_at
                    )
                )
                continue
            }
            if (incoming.updated_at > local.updated_at) {
                dao.update(
                    local.copy(
                        achieved_at = incoming.achieved_at,
                        source_user_challenge_id = incoming.source_user_challenge_id ?: local.source_user_challenge_id,
                        updated_at = incoming.updated_at,
                        synced_at = incoming.updated_at,
                        is_deleted = incoming.is_deleted
                    )
                )
            }
        }

        // Push: every dirty local row whose updated_at >= remote.
        val remoteByAchievement = remoteDocs.associateBy { it.id.toIntOrNull() ?: -1 }
        val dirty = dao.getDirty(userId)
        var allOk = true
        for (row in dirty) {
            val remote = remoteByAchievement[row.achievement_id]
            val remoteUpdatedAt = remote?.getLong(FIELD_UPDATED_AT) ?: 0L
            if (row.updated_at < remoteUpdatedAt) continue
            val ok = runCatching {
                subcol.document(row.achievement_id.toString())
                    .set(row.toFirestoreMap())
                    .await()
                dao.markSynced(row.id, row.updated_at, row.updated_at)
            }.onFailure {
                Log.w(TAG, "Push failed for badge achievement=${row.achievement_id}", it)
            }.isSuccess
            allOk = allOk && ok
        }
        return allOk
    }

    private fun UserAchievementEntity.toFirestoreMap(): Map<String, Any?> = mapOf(
        FIELD_ACHIEVEMENT_ID to achievement_id,
        FIELD_USER_ID to user_id,
        FIELD_ACHIEVED_AT to achieved_at,
        FIELD_SOURCE_USER_CHALLENGE_ID to source_user_challenge_id,
        FIELD_UPDATED_AT to updated_at,
        FIELD_IS_DELETED to is_deleted
    )

    private fun DocumentSnapshot.toEntity(uid: String): UserAchievementEntity? {
        if (!exists()) return null
        val achievementId = (getLong(FIELD_ACHIEVEMENT_ID) ?: id.toLongOrNull())?.toInt() ?: return null
        val achievedAt = getLong(FIELD_ACHIEVED_AT) ?: System.currentTimeMillis()
        val updatedAt = getLong(FIELD_UPDATED_AT) ?: achievedAt
        return UserAchievementEntity(
            id = 0,
            achievement_id = achievementId,
            user_id = uid,
            achieved_at = achievedAt,
            source_user_challenge_id = getLong(FIELD_SOURCE_USER_CHALLENGE_ID)?.toInt(),
            updated_at = updatedAt,
            synced_at = null,
            is_deleted = getBoolean(FIELD_IS_DELETED) ?: false
        )
    }

    private companion object {
        const val TAG = "BadgeSync"
        const val USERS = "users"
        const val BADGES = "badges"
        const val FIELD_ACHIEVEMENT_ID = "achievement_id"
        const val FIELD_USER_ID = "user_id"
        const val FIELD_ACHIEVED_AT = "achieved_at"
        const val FIELD_SOURCE_USER_CHALLENGE_ID = "source_user_challenge_id"
        const val FIELD_UPDATED_AT = "updated_at"
        const val FIELD_IS_DELETED = "is_deleted"
    }
}
