package com.example.betterme.data.share

import android.util.Log
import com.example.betterme.domain.repository.ShareRepository
import com.example.betterme.domain.share.ShareLink
import com.example.betterme.domain.share.VerificationStatus
import com.example.betterme.domain.share.VerifiedCheckIn
import com.example.betterme.domain.share.VerifiedProfile
import com.example.betterme.domain.share.VerifiedShare
import com.example.betterme.domain.share.VerifiedSummary
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

/**
 * Firestore-direct implementation of the simple share system.
 *
 * Firestore layout:
 *
 *   /shared_progress/{userId}
 *     {
 *       userId, displayName, avatarUrl, publishedAt,
 *       totalCheckIns, currentStreakDays, longestStreakDays,
 *       completedChallenges, legendaryChallenges,
 *       checkIns: [
 *         { itemId, name, kind, date },
 *         ...
 *       ]
 *     }
 *
 * One document per user. Each publish overwrites the previous
 * snapshot atomically. One Firestore read per viewer open — cheap
 * regardless of how many check-ins are in the array (1 MB doc limit
 * comfortably handles ~5000 entries).
 *
 * No HMAC, no signature, no per-row proof hash. "Verified" means the
 * data lives in Firestore.
 *
 * ### Recommended Firestore Security Rules
 *
 *     match /shared_progress/{userId} {
 *       // Anyone can read — the URL itself is the access token.
 *       allow read: if true;
 *       // Only the owner can publish / overwrite their own snapshot.
 *       allow write: if request.auth != null
 *                  && request.auth.uid == userId;
 *     }
 */
class ShareRepositoryImpl(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth
) : ShareRepository {

    override suspend fun publishMyProgress(
        displayName: String,
        avatarUrl: String?,
        currentStreakDays: Int,
        longestStreakDays: Int,
        completedChallenges: Int,
        legendaryChallenges: Int,
        checkIns: List<ShareRepository.CheckInInput>
    ): ShareLink {
        val userId = auth.currentUser?.uid
            ?: throw IllegalStateException("Vui lòng đăng nhập để chia sẻ tiến độ.")
        if (checkIns.isEmpty()) {
            throw IllegalStateException("Chưa có check-in nào để chia sẻ.")
        }

        val publishedAt = System.currentTimeMillis()
        // Recency-sorted + capped — keeps doc under the 1 MB limit
        // even for users with thousands of check-ins, and surfaces the
        // most relevant rows first when the viewer renders the timeline.
        val safeCheckIns = checkIns
            .sortedByDescending { it.date }
            .take(MAX_CHECK_INS_PER_DOC)

        val doc = mapOf(
            FIELD_USER_ID to userId,
            FIELD_DISPLAY_NAME to displayName,
            FIELD_AVATAR_URL to avatarUrl,
            FIELD_PUBLISHED_AT to publishedAt,
            FIELD_TOTAL_CHECK_INS to safeCheckIns.size,
            FIELD_CURRENT_STREAK to currentStreakDays,
            FIELD_LONGEST_STREAK to longestStreakDays,
            FIELD_COMPLETED_CHALLENGES to completedChallenges,
            FIELD_LEGENDARY_CHALLENGES to legendaryChallenges,
            FIELD_CHECK_INS to safeCheckIns.map { it.toMap() }
        )

        firestore.collection(COLLECTION).document(userId).set(doc).await()

        val deepLink = "betterme://share/$userId"
        val richMessage = buildRichMessage(
            displayName = displayName,
            totalCheckIns = safeCheckIns.size,
            streakDays = currentStreakDays,
            legendaryChallenges = legendaryChallenges,
            link = deepLink
        )
        return ShareLink(
            userId = userId,
            deepLink = deepLink,
            richMessage = richMessage,
            publishedAt = publishedAt
        )
    }

    override suspend fun loadByUserId(userId: String): ShareRepository.LoadResult {
        if (userId.isBlank()) {
            return ShareRepository.LoadResult(VerificationStatus.NOT_FOUND, null)
        }
        return try {
            val snap = firestore.collection(COLLECTION).document(userId).get().await()
            if (!snap.exists()) {
                ShareRepository.LoadResult(VerificationStatus.NOT_FOUND, null)
            } else {
                val parsed = snap.data?.toDomain()
                if (parsed == null) {
                    ShareRepository.LoadResult(VerificationStatus.NOT_FOUND, null)
                } else {
                    ShareRepository.LoadResult(VerificationStatus.VALID, parsed)
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "loadByUserId failed for $userId", e)
            ShareRepository.LoadResult(VerificationStatus.NETWORK, null)
        }
    }

    // ─── Mapping helpers ───────────────────────────────────────────

    private fun ShareRepository.CheckInInput.toMap(): Map<String, Any?> = mapOf(
        FIELD_CHECK_IN_ITEM_ID to itemId,
        FIELD_CHECK_IN_NAME to name,
        FIELD_CHECK_IN_KIND to kind.name,
        FIELD_CHECK_IN_DATE to date
    )

    private fun Map<String, Any?>.toDomain(): VerifiedShare? {
        val userId = (this[FIELD_USER_ID] as? String) ?: return null
        val displayName = (this[FIELD_DISPLAY_NAME] as? String) ?: "BetterMe User"
        val avatarUrl = this[FIELD_AVATAR_URL] as? String
        val publishedAt = (this[FIELD_PUBLISHED_AT] as? Number)?.toLong() ?: 0L
        val checkInsRaw = this[FIELD_CHECK_INS] as? List<*> ?: emptyList<Any?>()
        val checkIns = checkInsRaw.mapNotNull { rawRow ->
            val row = rawRow as? Map<*, *> ?: return@mapNotNull null
            val itemId = row[FIELD_CHECK_IN_ITEM_ID] as? String ?: return@mapNotNull null
            val name = row[FIELD_CHECK_IN_NAME] as? String ?: return@mapNotNull null
            val date = (row[FIELD_CHECK_IN_DATE] as? Number)?.toLong() ?: return@mapNotNull null
            val kindStr = (row[FIELD_CHECK_IN_KIND] as? String)?.uppercase()
            val kind = when (kindStr) {
                "CHALLENGE" -> VerifiedCheckIn.Kind.CHALLENGE
                else -> VerifiedCheckIn.Kind.HABIT
            }
            VerifiedCheckIn(itemId = itemId, name = name, kind = kind, date = date)
        }
        return VerifiedShare(
            userId = userId,
            profile = VerifiedProfile(displayName = displayName, avatarUrl = avatarUrl),
            summary = VerifiedSummary(
                totalCheckIns = (this[FIELD_TOTAL_CHECK_INS] as? Number)?.toInt() ?: checkIns.size,
                currentStreakDays = (this[FIELD_CURRENT_STREAK] as? Number)?.toInt() ?: 0,
                longestStreakDays = (this[FIELD_LONGEST_STREAK] as? Number)?.toInt() ?: 0,
                completedChallenges = (this[FIELD_COMPLETED_CHALLENGES] as? Number)?.toInt() ?: 0,
                legendaryChallenges = (this[FIELD_LEGENDARY_CHALLENGES] as? Number)?.toInt() ?: 0
            ),
            checkIns = checkIns,
            publishedAt = publishedAt
        )
    }

    /** Vietnamese rich-text the user posts to Messenger / Zalo / FB. */
    private fun buildRichMessage(
        displayName: String,
        totalCheckIns: Int,
        streakDays: Int,
        legendaryChallenges: Int,
        link: String
    ): String = buildString {
        append("🔥 ").append(displayName).append(" đã hoàn thành ")
            .append(totalCheckIns).append(" check-in trên BetterMe!\n")
        if (streakDays > 0) {
            append("💪 Đang giữ chuỗi ").append(streakDays).append(" ngày liên tiếp\n")
        }
        if (legendaryChallenges > 0) {
            append("🏆 ").append(legendaryChallenges)
                .append(" thử thách huyền thoại đã hoàn thành\n")
        }
        append("\n👉 Xem chi tiết: ").append(link)
    }

    private companion object {
        const val TAG = "ShareRepo"
        const val COLLECTION = "shared_progress"
        const val MAX_CHECK_INS_PER_DOC = 500

        // Field names — kept as constants so the publish + load paths
        // can't drift on a typo.
        const val FIELD_USER_ID = "userId"
        const val FIELD_DISPLAY_NAME = "displayName"
        const val FIELD_AVATAR_URL = "avatarUrl"
        const val FIELD_PUBLISHED_AT = "publishedAt"
        const val FIELD_TOTAL_CHECK_INS = "totalCheckIns"
        const val FIELD_CURRENT_STREAK = "currentStreakDays"
        const val FIELD_LONGEST_STREAK = "longestStreakDays"
        const val FIELD_COMPLETED_CHALLENGES = "completedChallenges"
        const val FIELD_LEGENDARY_CHALLENGES = "legendaryChallenges"
        const val FIELD_CHECK_INS = "checkIns"
        const val FIELD_CHECK_IN_ITEM_ID = "itemId"
        const val FIELD_CHECK_IN_NAME = "name"
        const val FIELD_CHECK_IN_KIND = "kind"
        const val FIELD_CHECK_IN_DATE = "date"
    }
}
