package com.example.betterme.domain.leaderboard

/**
 * Per-entry rank movement from the previously persisted snapshot.
 *
 * Produced by the repository when reading a fresh leaderboard — the
 * previous rank lives in [com.example.betterme.data.leaderboard.RankSnapshotStore],
 * keyed by `(challengeId, seasonKey, userId)`.
 *
 * UI rules:
 *  - [Up] / [Down] render an arrow chip with `±N`.
 *  - [New] renders a "NEW" pill — fires when the user wasn't in the
 *    previous snapshot at all (first-ever appearance OR previous read
 *    didn't reach them).
 *  - [Unchanged] renders a small `•` dot to keep visual rhythm without
 *    drawing attention.
 *  - [Hidden] suppresses the chip entirely — used for rows we
 *    intentionally don't compute deltas for (e.g. seeded rivals on
 *    first session before any snapshot exists).
 */
sealed class RankDelta {
    data class Up(val by: Int) : RankDelta()
    data class Down(val by: Int) : RankDelta()
    data object New : RankDelta()
    data object Unchanged : RankDelta()
    data object Hidden : RankDelta()

    /** True when a non-zero direction should be animated by the UI. */
    val isDirectional: Boolean get() = this is Up || this is Down

    companion object {
        /**
         * Pure helper. `current` and `previous` are 1-indexed ranks;
         * `previous = null` means the entry didn't appear in the prior
         * snapshot.
         */
        fun compute(current: Int, previous: Int?): RankDelta = when {
            previous == null -> New
            previous == current -> Unchanged
            previous > current -> Up(by = previous - current)
            else -> Down(by = current - previous)
        }
    }
}
