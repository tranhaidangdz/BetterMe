package com.example.betterme.domain.repository

import com.example.betterme.domain.share.ShareLink
import com.example.betterme.domain.share.ShareType
import com.example.betterme.domain.share.VerificationStatus
import com.example.betterme.domain.share.VerifiedCheckIn
import com.example.betterme.domain.share.VerifiedShare

/**
 * Domain contract for the Verified Shareable Check-in History feature.
 *
 *  - [createShare] — POST `/createShare`. Caller has already gathered
 *    the snapshot inputs locally; the server signs + stores them and
 *    returns the share URLs. No local persistence — the server is the
 *    single source of truth.
 *
 *  - [loadShare] — GET `/getShare`. Server re-validates HMAC before
 *    returning. Failures surface as [VerificationStatus] without ever
 *    exposing a partial payload to the UI.
 *
 *  - [verifyShare] — GET `/verifyShare`. Light status-only probe.
 *    Used by the viewer's "re-check" button.
 */
interface ShareRepository {

    suspend fun createShare(
        type: ShareType,
        displayName: String,
        avatarUrl: String?,
        currentStreakDays: Int,
        longestStreakDays: Int,
        completedChallenges: Int,
        legendaryChallenges: Int,
        checkIns: List<CheckInInput>
    ): ShareLink

    suspend fun loadShare(shareId: String): LoadResult

    suspend fun verifyShare(shareId: String): VerificationStatus

    /**
     * Input row the client provides to [createShare] — the server adds
     * a `proofHash` field server-side, so the client never produces
     * one (it can't, without the server secret).
     */
    data class CheckInInput(
        val itemId: String,
        val itemTitle: String,
        val timestamp: Long,
        val kind: VerifiedCheckIn.Kind,
        val note: String? = null
    )

    /**
     * Wraps the verification outcome + the snapshot when valid. The
     * UI never sees a non-null [share] when the status isn't VALID,
     * so it can render a single conditional on [status].
     */
    data class LoadResult(
        val status: VerificationStatus,
        val share: VerifiedShare?
    )
}
