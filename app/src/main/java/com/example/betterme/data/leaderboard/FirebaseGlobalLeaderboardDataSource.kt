package com.example.betterme.data.leaderboard

import android.util.Log
import com.example.betterme.domain.leaderboard.GlobalLeaderboardEntry
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await

/**
 * Firestore I/O for the cross-challenge GLOBAL leaderboard. Mirrors
 * the shape of [FirebaseChallengeLeaderboardDataSource] but on a
 * different sub-collection so writes can't cross-contaminate:
 *
 *   /leaderboards/{seasonKey}/global/{userId}
 *
 * One field-named-order pinpoints the leaderboard query
 * (`totalScore DESC, updatedAt ASC`). Single-collection single-field
 * order requires no composite index — Firestore handles it
 * automatically.
 */
class FirebaseGlobalLeaderboardDataSource(
    private val firestore: FirebaseFirestore
) {

    suspend fun readTopEntries(
        seasonKey: String,
        limit: Long
    ): List<GlobalLeaderboardEntry> = try {
        firestore.collection(ROOT)
            .document(seasonKey)
            .collection(GLOBAL)
            .orderBy(FIELD_TOTAL_SCORE, Query.Direction.DESCENDING)
            .orderBy(FIELD_UPDATED_AT, Query.Direction.ASCENDING)
            .limit(limit)
            .get()
            .await()
            .documents
            .mapNotNull { it.toEntryOrNull() }
    } catch (e: Exception) {
        Log.w(TAG, "readTopEntries failed for $seasonKey", e)
        emptyList()
    }

    suspend fun readEntry(seasonKey: String, userId: String): GlobalLeaderboardEntry? = try {
        firestore.collection(ROOT)
            .document(seasonKey)
            .collection(GLOBAL)
            .document(userId)
            .get()
            .await()
            .toEntryOrNull()
    } catch (e: Exception) {
        Log.w(TAG, "readEntry failed for $seasonKey/$userId", e)
        null
    }

    suspend fun upsertEntry(
        seasonKey: String,
        userId: String,
        entry: GlobalLeaderboardEntry
    ): Boolean = try {
        firestore.collection(ROOT)
            .document(seasonKey)
            .collection(GLOBAL)
            .document(userId)
            .set(entry.toMap(userId))
            .await()
        true
    } catch (e: Exception) {
        Log.w(TAG, "upsertEntry failed for $seasonKey/$userId", e)
        false
    }

    suspend fun readTopForSeason(seasonKey: String, limit: Long = 3): List<GlobalLeaderboardEntry> =
        readTopEntries(seasonKey, limit)

    private fun DocumentSnapshot.toEntryOrNull(): GlobalLeaderboardEntry? {
        if (!exists()) return null
        val name = getString(FIELD_DISPLAY_NAME) ?: return null
        return GlobalLeaderboardEntry(
            userId = id,
            displayName = name,
            avatarUrl = getString(FIELD_AVATAR_URL),
            totalCompletedHabits = (getLong(FIELD_TOTAL_HABITS) ?: 0L).toInt(),
            longestStreak = (getLong(FIELD_LONGEST_STREAK) ?: 0L).toInt(),
            earnedCoins = (getLong(FIELD_COINS) ?: 0L).toInt(),
            completedChallenges = (getLong(FIELD_COMPLETED_CHALLENGES) ?: 0L).toInt(),
            monthlyConsistencyBonus = (getLong(FIELD_CONSISTENCY) ?: 0L).toInt(),
            totalScore = (getLong(FIELD_TOTAL_SCORE) ?: 0L).toInt(),
            updatedAt = getLong(FIELD_UPDATED_AT) ?: 0L
        )
    }

    private fun GlobalLeaderboardEntry.toMap(uid: String): Map<String, Any?> = mapOf(
        "userId" to uid,
        FIELD_DISPLAY_NAME to displayName,
        FIELD_AVATAR_URL to avatarUrl,
        FIELD_TOTAL_HABITS to totalCompletedHabits,
        FIELD_LONGEST_STREAK to longestStreak,
        FIELD_COINS to earnedCoins,
        FIELD_COMPLETED_CHALLENGES to completedChallenges,
        FIELD_CONSISTENCY to monthlyConsistencyBonus,
        FIELD_TOTAL_SCORE to totalScore,
        FIELD_UPDATED_AT to System.currentTimeMillis()
    )

    private companion object {
        const val TAG = "GlobalLB"

        const val ROOT = "leaderboards"
        const val GLOBAL = "global"

        const val FIELD_DISPLAY_NAME = "displayName"
        const val FIELD_AVATAR_URL = "avatarUrl"
        const val FIELD_TOTAL_HABITS = "totalCompletedHabits"
        const val FIELD_LONGEST_STREAK = "longestStreak"
        const val FIELD_COINS = "earnedCoins"
        const val FIELD_COMPLETED_CHALLENGES = "completedChallenges"
        const val FIELD_CONSISTENCY = "monthlyConsistencyBonus"
        const val FIELD_TOTAL_SCORE = "totalScore"
        const val FIELD_UPDATED_AT = "updatedAt"
    }
}
