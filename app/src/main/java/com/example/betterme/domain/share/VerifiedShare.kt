package com.example.betterme.domain.share

/**
 * A user's published progress snapshot, loaded directly from
 * Firestore via the viewer screen.
 *
 * "Verified" here is the simple student-level interpretation:
 *   - The data lives in Firestore at `/shared_progress/{userId}`.
 *   - The viewer reads it directly from there, never from the
 *     viewing device's local Room database.
 *   - If the doc exists, the snapshot is treated as authentic.
 *
 * No HMAC, no signature, no per-row proof hash. The single point of
 * authority is "Firestore says so." That is enough for the share-
 * progress use case without the operational complexity of a Cloud
 * Functions backend.
 */
data class VerifiedShare(
    val userId: String,
    val profile: VerifiedProfile,
    val summary: VerifiedSummary,
    val checkIns: List<VerifiedCheckIn>,
    val publishedAt: Long
)

data class VerifiedProfile(
    val displayName: String,
    val avatarUrl: String?
)

/**
 * Aggregate counts the publisher computed at share time. Stored
 * inline on the snapshot doc so the viewer doesn't need to re-derive
 * anything on read — one Firestore round trip and the screen has
 * everything it needs.
 */
data class VerifiedSummary(
    val totalCheckIns: Int,
    val currentStreakDays: Int,
    val longestStreakDays: Int,
    val completedChallenges: Int,
    val legendaryChallenges: Int
)

/**
 * One row in the shared timeline. [kind] is HABIT or CHALLENGE so the
 * viewer can render the right emoji + grouping. No proof hash, no
 * signature — the entire snapshot's authority is "exists in Firestore".
 */
data class VerifiedCheckIn(
    val itemId: String,
    val name: String,
    val kind: Kind,
    val date: Long
) {
    enum class Kind { HABIT, CHALLENGE }
}

/**
 * Status the viewer surfaces.
 *
 *  - [VALID]     — Firestore returned a snapshot doc for this userId.
 *  - [NOT_FOUND] — userId exists but has never published progress, or
 *                  the doc was deleted.
 *  - [NETWORK]   — couldn't reach Firestore (offline, etc.).
 */
enum class VerificationStatus { VALID, NOT_FOUND, NETWORK }
