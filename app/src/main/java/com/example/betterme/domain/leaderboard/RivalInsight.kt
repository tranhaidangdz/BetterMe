package com.example.betterme.domain.leaderboard

/**
 * Narrative card surfaced inside the Friends tab — one line about a
 * specific rival's relationship to the current user.
 *
 * Three kinds drive the available palette:
 *
 *  - [Kind.PASSED_YOU]   — rival was below or tied last snapshot and
 *                          is now above the user.
 *  - [Kind.WITHIN_REACH] — rival is at most [pointsGap] points ahead
 *                          (used for "Only 6 points behind Emma").
 *  - [Kind.DEFEATED]     — user moved past the rival since last snapshot.
 *
 * Pure data — produced deterministically by the repository on every
 * read; not persisted.
 */
data class RivalInsight(
    val kind: Kind,
    val rivalDisplayName: String,
    val rivalAvatarUrl: String?,
    val pointsGap: Int,
    val message: String
) {
    enum class Kind { PASSED_YOU, WITHIN_REACH, DEFEATED }
}
