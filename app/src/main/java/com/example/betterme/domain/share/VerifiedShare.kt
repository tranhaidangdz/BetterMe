package com.example.betterme.domain.share

/**
 * Server-verified share snapshot. This is the ONLY shape the Android
 * viewer ever renders — never local Room data, never screenshots,
 * never AI-detected content. The server's HMAC over the snapshot's
 * canonical JSON is the integrity primitive; if that check fails, the
 * repository returns [VerificationStatus.INVALID] and the viewer
 * surfaces a red "Không hợp lệ" state instead.
 *
 * Field shape mirrors what `getShare` returns. Keep the field names
 * matching the wire DTO so the Retrofit mapper stays a trivial
 * one-liner per field.
 */
data class VerifiedShare(
    val shareId: String,
    val userId: String,
    val type: ShareType,
    val profile: VerifiedProfile,
    val summary: VerifiedSummary,
    val checkIns: List<VerifiedCheckIn>,
    val createdAt: Long,
    val expiresAt: Long
)

data class VerifiedProfile(
    val displayName: String,
    val avatarUrl: String?
)

/**
 * Aggregate stats the backend computed at share-creation time. These
 * are SERVER-DERIVED — the client-submitted payload's totals are
 * recomputed by the function to prevent the user from inflating the
 * numbers in the share message they post to Messenger.
 */
data class VerifiedSummary(
    val totalCheckIns: Int,
    val uniqueItems: Int,
    val earliestTimestamp: Long,
    val latestTimestamp: Long,
    val currentStreakDays: Int,
    val longestStreakDays: Int,
    val completedChallenges: Int,
    val legendaryChallenges: Int
)

/**
 * One check-in row. [proofHash] is one-way derived from
 * `SHA256(userId + itemId + timestamp + serverSecret)` so the viewer
 * can render a per-row "✔ Verified" badge without re-contacting the
 * server. A client cannot forge a row because it doesn't have the
 * secret.
 *
 * The hash itself is informational on the client side; the actual
 * trust boundary is the top-level HMAC validated by the server before
 * the snapshot is returned at all.
 */
data class VerifiedCheckIn(
    val itemId: String,
    val itemTitle: String,
    val timestamp: Long,
    val kind: Kind,
    val note: String?,
    val proofHash: String
) {
    enum class Kind { HABIT, CHALLENGE }
}

/**
 * Status returned by `getShare` / `verifyShare`.
 *
 *  - [VALID]     — HMAC matched, snapshot is fresh, payload safe to render.
 *  - [INVALID]   — Firestore doc was tampered with externally; refuse.
 *  - [NOT_FOUND] — never existed or expired.
 *  - [NETWORK]   — couldn't reach the function (no payload to render).
 */
enum class VerificationStatus { VALID, INVALID, NOT_FOUND, NETWORK }
