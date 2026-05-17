package com.example.betterme.domain.ai

import com.example.betterme.utils.DateUtils
import java.util.concurrent.ConcurrentHashMap

/**
 * Process-scoped throttle for the Home AI surfaces (Recovery, Progression,
 * Lifestyle insight). Stops the cards from re-analyzing every time the
 * user navigates back to Home — burning battery, redoing Room reads, and
 * flashing the loading skeleton.
 *
 * The 24h repo-level [AiCacheRepository] already short-circuits the actual
 * OpenRouter call when the input fingerprint hasn't changed. This memory
 * sits one layer *above* that — it stops the VM from even firing
 * `Analyze()` on routine navigation, so the user sees their last result
 * instantly without a Loading flash.
 *
 * ### Throttle rules
 *  - **Min re-analysis interval** ([MIN_REANALYZE_INTERVAL_MS] — 4h by
 *    default). On Home entry, if the VM analyzed less than 4h ago, the
 *    HomeScreen's auto-launch is skipped. The user's manual "Phân tích
 *    lại" pill always forces a refresh and bypasses the throttle.
 *  - **Dismissal silences** ([DISMISS_SILENCE_MS] — 24h). When the user
 *    taps "Ẩn" / "Để sau" on a card, that surface goes silent until next
 *    day. The user has explicitly told us "not now" — respect it.
 *  - **Day boundary refresh**. The throttle resets at the local-midnight
 *    boundary by comparing `DateUtils.startOfDay(lastAt)` to `today` —
 *    the next morning's first Home entry always re-analyzes even if it's
 *    only been ~5 hours.
 *
 * ### Thread safety
 * Two ConcurrentHashMaps — one per surface. The VM reads/writes from the
 * IO-bound coroutine that analyzes; HomeScreen reads from the Main thread
 * before dispatching. Atomic put/get on either map is enough; no compound
 * operations need locking.
 *
 * ### Lifecycle
 * Process-scoped singleton (Koin `single`). State is intentionally lost on
 * process death — when the OS kills BetterMe and the user re-enters, a
 * fresh analysis is the correct behavior; we don't try to persist this.
 */
class AiHomeSessionMemory {

    enum class Surface {
        RECOVERY,
        PROGRESSION,
        LIFESTYLE_INSIGHT
    }

    private val lastAnalyzedAtMs = ConcurrentHashMap<Surface, Long>()
    private val dismissedAtMs = ConcurrentHashMap<Surface, Long>()

    /**
     * Should the surface auto-analyze on Home entry?
     *
     *  - **false** when:
     *    1. The user dismissed this surface in the last 24h (and we're still
     *       on the same calendar day they dismissed it).
     *    2. The surface was analyzed within the last 4h on the current day.
     *  - **true** otherwise — including the first call ever, or any call
     *    after a calendar-day boundary.
     *
     * The user's manual "Phân tích lại" pill always bypasses this gate by
     * passing `forceRefresh = true` to the VM's Analyze intent.
     */
    fun shouldAutoAnalyze(surface: Surface, now: Long = System.currentTimeMillis()): Boolean {
        val today = DateUtils.startOfDay(now)

        val dismissedAt = dismissedAtMs[surface]
        if (dismissedAt != null) {
            val dismissedDay = DateUtils.startOfDay(dismissedAt)
            if (dismissedDay == today && (now - dismissedAt) < DISMISS_SILENCE_MS) {
                return false
            }
        }

        val lastAt = lastAnalyzedAtMs[surface] ?: return true
        val lastDay = DateUtils.startOfDay(lastAt)
        if (lastDay != today) return true // new day — always refresh
        return (now - lastAt) >= MIN_REANALYZE_INTERVAL_MS
    }

    /** Called by the VM when an Analyze completes (success or hidden). */
    fun markAnalyzed(surface: Surface, now: Long = System.currentTimeMillis()) {
        lastAnalyzedAtMs[surface] = now
    }

    /** Called by the VM when the user dismisses the card. */
    fun markDismissed(surface: Surface, now: Long = System.currentTimeMillis()) {
        dismissedAtMs[surface] = now
    }

    /** Clears throttle state — used by tests and by sign-out flows. */
    fun reset() {
        lastAnalyzedAtMs.clear()
        dismissedAtMs.clear()
    }

    private companion object {
        /** 4 hours — see [shouldAutoAnalyze] docs. */
        const val MIN_REANALYZE_INTERVAL_MS: Long = 4L * 60 * 60 * 1000

        /** 24 hours — paired with same-day check for graceful next-morning refresh. */
        const val DISMISS_SILENCE_MS: Long = 24L * 60 * 60 * 1000
    }
}
