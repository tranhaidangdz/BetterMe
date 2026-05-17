package com.example.betterme.data.leaderboard

import android.util.Log
import com.example.betterme.domain.leaderboard.ChallengeMeta
import com.example.betterme.domain.leaderboard.LeaderboardEntry
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await

/**
 * Firestore-backed I/O for the monthly challenge leaderboard.
 *
 * ### Layout
 * Documents live under:
 *
 *   /leaderboards/{seasonKey}/challenges/{challengeId}/entries/{userId}
 *
 * The parallel meta path:
 *
 *   /leaderboards/{seasonKey}/challenges/{challengeId}
 *
 * holds participantCount + topScore as plain fields on the doc that
 * sits ABOVE the entries subcollection. This is Firestore-legal (a doc
 * can have its own fields AND a subcollection) and lets the meta read
 * be a single doc fetch.
 *
 * ### Index requirement
 * The leaderboard query (`orderBy totalScore desc limit 50`) on a
 * subcollection works without any composite index in Firestore — single-
 * field ordering on a single collection is automatically indexed. We
 * deliberately avoid `where(...)` filters in the leaderboard query so
 * no extra index is needed at launch.
 *
 * ### Why a thin datasource (not a repository) here
 * The Firestore-specific path / serialization shape stays out of the
 * repository so swapping to a different backend later (a paid
 * RTDB-backed server, a custom REST endpoint) only changes this file.
 */
class FirebaseChallengeLeaderboardDataSource(
    private val firestore: FirebaseFirestore
) {

    /**
     * Read the top-[limit] entries for one challenge in one season,
     * ordered by `totalScore` desc. Doesn't include the current user
     * unless they're in the top [limit]; callers are expected to call
     * [readEntry] separately for the current user when needed.
     */
    suspend fun readTopEntries(
        challengeId: Int,
        seasonKey: String,
        limit: Long
    ): List<LeaderboardEntry> {
        return try {
            val snapshot = firestore.collection(ROOT)
                .document(seasonKey)
                .collection(CHALLENGES)
                .document(challengeId.toString())
                .collection(ENTRIES)
                .orderBy(FIELD_TOTAL_SCORE, Query.Direction.DESCENDING)
                .orderBy(FIELD_UPDATED_AT, Query.Direction.ASCENDING) // tie-breaker
                .limit(limit)
                .get()
                .await()
            snapshot.documents.mapNotNull { it.toEntryOrNull() }
        } catch (e: Exception) {
            Log.w(TAG, "readTopEntries failed for $seasonKey/$challengeId", e)
            emptyList()
        }
    }

    /** Single-entry read for the current user, separate path from the list. */
    suspend fun readEntry(
        challengeId: Int,
        seasonKey: String,
        userId: String
    ): LeaderboardEntry? {
        return try {
            firestore.collection(ROOT)
                .document(seasonKey)
                .collection(CHALLENGES)
                .document(challengeId.toString())
                .collection(ENTRIES)
                .document(userId)
                .get()
                .await()
                .toEntryOrNull()
        } catch (e: Exception) {
            Log.w(TAG, "readEntry failed for $seasonKey/$challengeId/$userId", e)
            null
        }
    }

    /**
     * Upsert one entry + update the meta doc in a transaction so the
     * meta's participantCount / topScore can't drift relative to the
     * entries collection. Transaction is best-effort — on failure we
     * log and return false so the use case can retry on the next
     * check-in.
     */
    suspend fun upsertEntry(
        challengeId: Int,
        seasonKey: String,
        userId: String,
        entry: LeaderboardEntry
    ): Boolean {
        return try {
            val entryRef = firestore.collection(ROOT)
                .document(seasonKey)
                .collection(CHALLENGES)
                .document(challengeId.toString())
                .collection(ENTRIES)
                .document(userId)
            val metaRef = firestore.collection(ROOT)
                .document(seasonKey)
                .collection(CHALLENGES)
                .document(challengeId.toString())

            firestore.runTransaction { tx ->
                val existingEntry = tx.get(entryRef)
                val existingMeta = tx.get(metaRef)

                val isNewEntry = !existingEntry.exists()
                val prevTopScore = (existingMeta.getLong(FIELD_TOP_SCORE) ?: 0L).toInt()
                val prevParticipants = (existingMeta.getLong(FIELD_PARTICIPANT_COUNT) ?: 0L).toInt()

                tx.set(entryRef, entry.toFirestoreMap(userId))
                val newTopScore = maxOf(prevTopScore, entry.totalScore)
                val newParticipantCount = if (isNewEntry) prevParticipants + 1 else prevParticipants
                tx.set(
                    metaRef,
                    mapOf(
                        FIELD_PARTICIPANT_COUNT to newParticipantCount,
                        FIELD_TOP_SCORE to newTopScore,
                        FIELD_UPDATED_AT to System.currentTimeMillis()
                    )
                )
                null
            }.await()
            true
        } catch (e: Exception) {
            Log.w(TAG, "upsertEntry failed for $seasonKey/$challengeId/$userId", e)
            false
        }
    }

    /** Pure meta read — fast, no entries scan. */
    suspend fun readMeta(challengeId: Int, seasonKey: String): ChallengeMeta? {
        return try {
            val doc = firestore.collection(ROOT)
                .document(seasonKey)
                .collection(CHALLENGES)
                .document(challengeId.toString())
                .get()
                .await()
            if (!doc.exists()) {
                ChallengeMeta(challengeId, seasonKey, 0, 0, 0L)
            } else {
                ChallengeMeta(
                    challengeId = challengeId,
                    seasonKey = seasonKey,
                    participantCount = (doc.getLong(FIELD_PARTICIPANT_COUNT) ?: 0L).toInt(),
                    topScore = (doc.getLong(FIELD_TOP_SCORE) ?: 0L).toInt(),
                    updatedAt = doc.getLong(FIELD_UPDATED_AT) ?: 0L
                )
            }
        } catch (e: Exception) {
            Log.w(TAG, "readMeta failed for $seasonKey/$challengeId", e)
            null
        }
    }

    // ============================================================
    // Mapping
    // ============================================================

    private fun com.google.firebase.firestore.DocumentSnapshot.toEntryOrNull(): LeaderboardEntry? {
        if (!exists()) return null
        val userId = id
        val displayName = getString(FIELD_DISPLAY_NAME) ?: return null
        return LeaderboardEntry(
            userId = userId,
            displayName = displayName,
            avatarUrl = getString(FIELD_AVATAR_URL),
            completedTasks = (getLong(FIELD_COMPLETED_TASKS) ?: 0L).toInt(),
            currentStreak = (getLong(FIELD_CURRENT_STREAK) ?: 0L).toInt(),
            earnedCoins = (getLong(FIELD_EARNED_COINS) ?: 0L).toInt(),
            totalScore = (getLong(FIELD_TOTAL_SCORE) ?: 0L).toInt(),
            updatedAt = getLong(FIELD_UPDATED_AT) ?: 0L
        )
    }

    private fun LeaderboardEntry.toFirestoreMap(uid: String): Map<String, Any?> = mapOf(
        "userId" to uid,
        FIELD_DISPLAY_NAME to displayName,
        FIELD_AVATAR_URL to avatarUrl,
        FIELD_COMPLETED_TASKS to completedTasks,
        FIELD_CURRENT_STREAK to currentStreak,
        FIELD_EARNED_COINS to earnedCoins,
        FIELD_TOTAL_SCORE to totalScore,
        FIELD_UPDATED_AT to System.currentTimeMillis()
    )

    private companion object {
        const val TAG = "ChallengeLB"

        const val ROOT = "leaderboards"
        const val CHALLENGES = "challenges"
        const val ENTRIES = "entries"

        const val FIELD_DISPLAY_NAME = "displayName"
        const val FIELD_AVATAR_URL = "avatarUrl"
        const val FIELD_COMPLETED_TASKS = "completedTasks"
        const val FIELD_CURRENT_STREAK = "currentStreak"
        const val FIELD_EARNED_COINS = "earnedCoins"
        const val FIELD_TOTAL_SCORE = "totalScore"
        const val FIELD_UPDATED_AT = "updatedAt"
        const val FIELD_PARTICIPANT_COUNT = "participantCount"
        const val FIELD_TOP_SCORE = "topScore"
    }
}
