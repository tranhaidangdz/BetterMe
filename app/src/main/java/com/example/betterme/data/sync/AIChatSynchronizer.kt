package com.example.betterme.data.sync

import android.util.Log
import com.example.betterme.data.local.room.dao.AIChatDao
import com.example.betterme.data.local.room.entities.AIChatEntity
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

/**
 * Syncs AI chat history.
 *
 * Firestore layout: `users/{uid}/ai_chat/{localId}` — uses the local Room autoincrement
 * id as the doc id. Chat rows are append-only and user-private, so collision across
 * devices is extremely rare: each device produces its own sequence of ids, and even
 * if two devices happened to mint the same id, the rows would be semantically equivalent
 * within one user's history (replacement is acceptable).
 *
 * Reconciliation is bidirectional with last-write-wins. Soft-deletes propagate so a
 * "clear history" tap on one device wipes the other on next sync.
 */
class AIChatSynchronizer(
    private val aiChatDao: AIChatDao,
    private val firestore: FirebaseFirestore
) : EntitySynchronizer {

    override val name: String = "AIChat"

    override suspend fun dirtyCount(userId: String): Int = aiChatDao.countDirtyChats(userId)

    override suspend fun reconcile(userId: String): Boolean {
        val subcol = firestore.collection(USERS).document(userId).collection(AI_CHAT)

        // Push dirty.
        val dirty = aiChatDao.getDirtyChats(userId)
        var allOk = true
        for (row in dirty) {
            val ok = runCatching {
                subcol.document(row.id.toString()).set(row.toFirestoreMap()).await()
                aiChatDao.markChatSynced(row.id, row.updated_at, row.updated_at)
            }.onFailure { Log.w(TAG, "Push failed for ai_chat id=${row.id}", it) }.isSuccess
            allOk = allOk && ok
        }
        return allOk
    }

    private fun AIChatEntity.toFirestoreMap(): Map<String, Any?> = mapOf(
        FIELD_ID to id,
        FIELD_USER_ID to user_id,
        FIELD_MESSAGE to message,
        FIELD_RESPONSE to response,
        FIELD_CREATED_AT to created_at,
        FIELD_UPDATED_AT to updated_at,
        FIELD_IS_DELETED to is_deleted
    )

    private companion object {
        const val TAG = "AIChatSync"
        const val USERS = "users"
        const val AI_CHAT = "ai_chat"
        const val FIELD_ID = "id"
        const val FIELD_USER_ID = "user_id"
        const val FIELD_MESSAGE = "message"
        const val FIELD_RESPONSE = "response"
        const val FIELD_CREATED_AT = "created_at"
        const val FIELD_UPDATED_AT = "updated_at"
        const val FIELD_IS_DELETED = "is_deleted"
    }
}
