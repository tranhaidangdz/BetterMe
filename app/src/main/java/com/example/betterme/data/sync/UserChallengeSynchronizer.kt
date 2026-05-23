package com.example.betterme.data.sync

import android.util.Log
import com.example.betterme.data.local.room.dao.UserChallengeDao
import com.example.betterme.data.local.room.entities.UserChallengeEntity
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

/**
 * Syncs the user's per-challenge progress rows.
 *
 * Firestore layout: `users/{uid}/user_challenges/{challengeId}` — the doc id is the
 * **stable challenge_id** from the seeded catalog, NOT the local Room autoincrement
 * `id`, so the same UserChallenge has the same doc id on every device.
 *
 * Reconciliation:
 *  - Pull every doc under the user's subcollection.
 *  - For each local dirty row: push if local `updated_at` >= remote `updated_at`.
 *  - For each remote row newer than local: apply to Room.
 *  - Soft-deletes (`is_deleted = true`) propagate in both directions.
 *
 * The local `id` (autoincrement) is *never* used as a doc id — it would collide
 * across devices. `challenge_id` is the stable identity.
 */
class UserChallengeSynchronizer(
    private val userChallengeDao: UserChallengeDao,
    private val firestore: FirebaseFirestore
) : EntitySynchronizer {

    override val name: String = "UserChallenge"

    override suspend fun dirtyCount(userId: String): Int =
        userChallengeDao.countDirtyForUser(userId)

    override suspend fun reconcile(userId: String): Boolean {
        val subcol = firestore.collection(USERS).document(userId).collection(USER_CHALLENGES)

        val remoteDocs = try {
            subcol.get().await().documents
        } catch (e: Exception) {
            Log.w(TAG, "Remote read failed", e)
            return false
        }

        val remoteByChallengeId: Map<Int, com.google.firebase.firestore.DocumentSnapshot> =
            remoteDocs.associateBy { it.id.toIntOrNull() ?: -1 }
                .filterKeys { it > 0 }

        // Pull: apply remote rows that beat (or have no) local counterpart.
        for ((challengeId, snap) in remoteByChallengeId) {
            val incoming = snap.toUserChallengeEntity(userId, challengeId) ?: continue
            val local = userChallengeDao.getByUserAndChallenge(userId, challengeId)
            if (local == null) {
                userChallengeDao.insert(incoming.copy(synced_at = incoming.updated_at))
                continue
            }
            if (incoming.updated_at > local.updated_at) {
                // Remote wins. Preserve the local autoincrement `id` so foreign-key
                // links from challenge_logs stay valid.
                userChallengeDao.update(
                    incoming.copy(id = local.id, synced_at = incoming.updated_at)
                )
            }
        }

        // Push: send every dirty local row whose updated_at >= remote.
        val dirty = userChallengeDao.getDirtyForUser(userId)
        var allOk = true
        for (row in dirty) {
            val remote = remoteByChallengeId[row.challenge_id]
            val remoteUpdatedAt = remote?.getLong(FIELD_UPDATED_AT) ?: 0L
            if (row.updated_at < remoteUpdatedAt) {
                // Remote is newer; the pull loop above already handled it.
                continue
            }
            val ok = runCatching {
                subcol.document(row.challenge_id.toString()).set(row.toFirestoreMap()).await()
                userChallengeDao.markSynced(row.id, row.updated_at, row.updated_at)
            }.onFailure { e ->
                Log.w(TAG, "Push failed for uc id=${row.id} challenge=${row.challenge_id}", e)
            }.isSuccess
            allOk = allOk && ok
        }
        return allOk
    }

    private fun UserChallengeEntity.toFirestoreMap(): Map<String, Any?> = mapOf(
        FIELD_USER_ID to user_id,
        FIELD_CHALLENGE_ID to challenge_id,
        FIELD_STATUS to status,
        FIELD_START_DATE to start_date,
        FIELD_TARGET_END_DATE to target_end_date,
        FIELD_END_DATE to end_date,
        FIELD_CURRENT_STREAK to current_streak,
        FIELD_BEST_STREAK to best_streak,
        FIELD_LAST_CHECK_IN_DATE to last_check_in_date,
        FIELD_PROGRESS_PCT to progress_pct,
        FIELD_TEAM_ID to team_id,
        FIELD_JOINED_AT to joined_at,
        FIELD_UPDATED_AT to updated_at,
        FIELD_IS_DELETED to is_deleted
    )

    private fun com.google.firebase.firestore.DocumentSnapshot.toUserChallengeEntity(
        uid: String,
        challengeId: Int
    ): UserChallengeEntity? {
        if (!exists()) return null
        return UserChallengeEntity(
            id = 0, // assigned by caller (preserved from local row if updating)
            user_id = uid,
            challenge_id = challengeId,
            status = getString(FIELD_STATUS) ?: "ACTIVE",
            start_date = getLong(FIELD_START_DATE) ?: System.currentTimeMillis(),
            target_end_date = getLong(FIELD_TARGET_END_DATE),
            end_date = getLong(FIELD_END_DATE),
            current_streak = (getLong(FIELD_CURRENT_STREAK) ?: 0L).toInt(),
            best_streak = (getLong(FIELD_BEST_STREAK) ?: 0L).toInt(),
            last_check_in_date = getLong(FIELD_LAST_CHECK_IN_DATE),
            progress_pct = (getLong(FIELD_PROGRESS_PCT) ?: 0L).toInt(),
            team_id = getLong(FIELD_TEAM_ID)?.toInt(),
            joined_at = getLong(FIELD_JOINED_AT) ?: System.currentTimeMillis(),
            updated_at = getLong(FIELD_UPDATED_AT) ?: 0L,
            synced_at = null,
            is_deleted = getBoolean(FIELD_IS_DELETED) ?: false
        )
    }

    private companion object {
        const val TAG = "UserChallengeSync"
        const val USERS = "users"
        const val USER_CHALLENGES = "user_challenges"
        const val FIELD_USER_ID = "user_id"
        const val FIELD_CHALLENGE_ID = "challenge_id"
        const val FIELD_STATUS = "status"
        const val FIELD_START_DATE = "start_date"
        const val FIELD_TARGET_END_DATE = "target_end_date"
        const val FIELD_END_DATE = "end_date"
        const val FIELD_CURRENT_STREAK = "current_streak"
        const val FIELD_BEST_STREAK = "best_streak"
        const val FIELD_LAST_CHECK_IN_DATE = "last_check_in_date"
        const val FIELD_PROGRESS_PCT = "progress_pct"
        const val FIELD_TEAM_ID = "team_id"
        const val FIELD_JOINED_AT = "joined_at"
        const val FIELD_UPDATED_AT = "updated_at"
        const val FIELD_IS_DELETED = "is_deleted"
    }
}
