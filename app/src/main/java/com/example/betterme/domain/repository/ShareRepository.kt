package com.example.betterme.domain.repository

import com.example.betterme.domain.share.ShareLink
import com.example.betterme.domain.share.VerificationStatus
import com.example.betterme.domain.share.VerifiedCheckIn
import com.example.betterme.domain.share.VerifiedShare

/**
 * Domain contract for the simple Verified Share feature.
 *
 * The implementation reads + writes Firestore directly at
 * `/shared_progress/{userId}` — there is no signing layer, no Cloud
 * Functions backend, no canonical-JSON HMAC. "Verified" means the
 * data lives in Firestore (the server source of truth) and the
 * viewer never falls back to local Room.
 *
 *  - [publishMyProgress] — write a fresh snapshot of the current
 *    user's habits + challenge check-ins to Firestore. Overwrites the
 *    previous snapshot for that user atomically. Returns the deep
 *    link + rich-share message the caller hands to ACTION_SEND.
 *
 *  - [loadByUserId] — fetch the snapshot doc for [userId]. Returns
 *    [VerificationStatus.NOT_FOUND] when the user has never published.
 */
interface ShareRepository {

    suspend fun publishMyProgress(
        displayName: String,
        avatarUrl: String?,
        currentStreakDays: Int,
        longestStreakDays: Int,
        completedChallenges: Int,
        legendaryChallenges: Int,
        checkIns: List<CheckInInput>
    ): ShareLink

    suspend fun loadByUserId(userId: String): LoadResult

    /** Input row for [publishMyProgress]. */
    data class CheckInInput(
        val itemId: String,
        val name: String,
        val kind: VerifiedCheckIn.Kind,
        val date: Long
    )

    /**
     * Wraps the load outcome. [share] is non-null only when [status]
     * is [VerificationStatus.VALID], so the UI can render a single
     * conditional.
     */
    data class LoadResult(
        val status: VerificationStatus,
        val share: VerifiedShare?
    )
}
