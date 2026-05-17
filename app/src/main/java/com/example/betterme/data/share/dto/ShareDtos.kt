package com.example.betterme.data.share.dto

import kotlinx.serialization.Serializable

/**
 * Wire-format DTOs for the verified-share Cloud Functions endpoints.
 *
 * Field names match `functions/index.js` 1:1 — keep them in lockstep
 * with the server if you ever rename. All optional / nullable for
 * graceful schema evolution.
 */

// ─── createShare body ───────────────────────────────────────────────

@Serializable
data class CreateShareRequest(
    val type: String,
    val profile: ProfileDto,
    val summary: SummaryDto,
    val checkIns: List<CheckInDto>
)

@Serializable
data class ProfileDto(
    val displayName: String,
    val avatarUrl: String? = null
)

@Serializable
data class SummaryDto(
    val currentStreakDays: Int = 0,
    val longestStreakDays: Int = 0,
    val completedChallenges: Int = 0,
    val legendaryChallenges: Int = 0
)

@Serializable
data class CheckInDto(
    val itemId: String,
    val itemTitle: String,
    val timestamp: Long,
    val kind: String,
    val note: String? = null,
    /**
     * Server-assigned on read; clients never populate this on create.
     * Kept optional so the same DTO works for both directions of the
     * wire.
     */
    val proofHash: String? = null
)

// ─── createShare response ───────────────────────────────────────────

@Serializable
data class CreateShareResponse(
    val shareId: String,
    val createdAt: Long,
    val deepLink: String
)

// ─── getShare response ──────────────────────────────────────────────

@Serializable
data class GetShareResponse(
    val status: String,
    val snapshot: SnapshotDto? = null
)

@Serializable
data class SnapshotDto(
    val shareId: String,
    val userId: String,
    val type: String,
    val profile: ProfileDto,
    val summary: ServerSummaryDto,
    val checkIns: List<CheckInDto>,
    val createdAt: Long,
    val expiresAt: Long
)

/**
 * Server-derived summary that includes the totals the function
 * computed during signing — these have authority over the
 * client-submitted [SummaryDto].
 */
@Serializable
data class ServerSummaryDto(
    val totalCheckIns: Int = 0,
    val uniqueItems: Int = 0,
    val earliestTimestamp: Long = 0,
    val latestTimestamp: Long = 0,
    val currentStreakDays: Int = 0,
    val longestStreakDays: Int = 0,
    val completedChallenges: Int = 0,
    val legendaryChallenges: Int = 0
)

// ─── verifyShare response ───────────────────────────────────────────

@Serializable
data class VerifyShareResponse(val status: String)
