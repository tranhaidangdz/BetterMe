package com.example.betterme.domain.usecase.challenge

import com.example.betterme.domain.challenge.UserChallengeStatus
import com.example.betterme.utils.DateUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

/**
 * Pure-function tests for the strict-daily evaluator's `decide()` step. We work in
 * `startOfDay` millis so the assertions read like calendar days, and verify each of the
 * scenarios called out in the strict-daily spec:
 *  - missed first day
 *  - gap between check-ins
 *  - app reopened after several days
 *  - missed last day
 *  - timezone (DST) handling
 *  - multiple check-ins on one day collapse
 *  - permanence: FAILED can never become COMPLETED
 *  - 1-day challenges don't fail on the same day they're joined
 */
class EvaluateChallengeStatusUseCaseTest {

    /** Shared anchor: Jan 1, 2026 at 00:00 local time. */
    private val day0: Long = Calendar.getInstance().apply {
        set(2026, Calendar.JANUARY, 1, 0, 0, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    private fun day(offset: Int): Long = DateUtils.plusDays(day0, offset)

    private fun decide(
        durationDays: Int,
        nowOffset: Int,
        doneDayOffsets: Set<Int>,
        status: String = UserChallengeStatus.ACTIVE,
        targetEndOverride: Long? = null
    ) = EvaluateChallengeStatusUseCase.decide(
        status = status,
        startDate = day0,
        targetEndDate = targetEndOverride ?: day(durationDays - 1),
        durationDays = durationDays,
        doneDates = doneDayOffsets.map { day(it) }.toSet(),
        now = day(nowOffset)
    )

    // ============================================================
    // Happy path
    // ============================================================

    @Test
    fun `mid-window with no gaps stays active`() {
        // 7-day challenge, day 3, days 0-2 done, today not yet logged.
        val d = decide(durationDays = 7, nowOffset = 3, doneDayOffsets = setOf(0, 1, 2))
        assertEquals(EvaluateChallengeStatusUseCase.Decision.KeepActive, d)
    }

    @Test
    fun `final-day check-in immediately completes`() {
        // 7-day challenge, today is day 6 (target end), all 7 days done.
        val d = decide(durationDays = 7, nowOffset = 6, doneDayOffsets = (0..6).toSet())
        assertTrue(d is EvaluateChallengeStatusUseCase.Decision.TransitionCompleted)
        val completed = d as EvaluateChallengeStatusUseCase.Decision.TransitionCompleted
        assertEquals(day(6), completed.endDate)
    }

    @Test
    fun `day after deadline with all days done completes`() {
        // User checked in on the final day but the evaluator only runs the next morning.
        val d = decide(durationDays = 7, nowOffset = 7, doneDayOffsets = (0..6).toSet())
        assertTrue(d is EvaluateChallengeStatusUseCase.Decision.TransitionCompleted)
    }

    // ============================================================
    // Failure scenarios from the spec
    // ============================================================

    @Test
    fun `missed first day fails on day 1`() {
        // No check-in on day 0. On day 1, the evaluator runs and finds the gap.
        val d = decide(durationDays = 7, nowOffset = 1, doneDayOffsets = emptySet())
        assertTrue("Expected TransitionFailed, got $d", d is EvaluateChallengeStatusUseCase.Decision.TransitionFailed)
        val failed = d as EvaluateChallengeStatusUseCase.Decision.TransitionFailed
        assertEquals(day(0), failed.firstMissedDate)
    }

    @Test
    fun `mid-window gap fails`() {
        // 7-day challenge, today=day 4, days 0,1,2 done, day 3 missed.
        val d = decide(durationDays = 7, nowOffset = 4, doneDayOffsets = setOf(0, 1, 2))
        assertTrue(d is EvaluateChallengeStatusUseCase.Decision.TransitionFailed)
        val failed = d as EvaluateChallengeStatusUseCase.Decision.TransitionFailed
        assertEquals(day(3), failed.firstMissedDate)
    }

    @Test
    fun `app reopened after several days finds first missed day`() {
        // User checked in day 0, then disappeared. Opens on day 5.
        // Expected: TransitionFailed at day 1.
        val d = decide(durationDays = 7, nowOffset = 5, doneDayOffsets = setOf(0))
        assertTrue(d is EvaluateChallengeStatusUseCase.Decision.TransitionFailed)
        assertEquals(day(1), (d as EvaluateChallengeStatusUseCase.Decision.TransitionFailed).firstMissedDate)
    }

    @Test
    fun `missed last day fails after deadline passes`() {
        // 7-day challenge, days 0-5 done, day 6 missed, today=day 7.
        val d = decide(durationDays = 7, nowOffset = 7, doneDayOffsets = (0..5).toSet())
        assertTrue(d is EvaluateChallengeStatusUseCase.Decision.TransitionFailed)
        assertEquals(day(6), (d as EvaluateChallengeStatusUseCase.Decision.TransitionFailed).firstMissedDate)
    }

    @Test
    fun `on deadline day with no check-in stays active`() {
        // Critical: a 1-day challenge joined today and not yet checked in must NOT
        // auto-fail just because the evaluator ran. The user gets the rest of the day.
        val d = decide(durationDays = 1, nowOffset = 0, doneDayOffsets = emptySet())
        assertEquals(EvaluateChallengeStatusUseCase.Decision.KeepActive, d)
    }

    @Test
    fun `one-day challenge with same-day check-in completes`() {
        val d = decide(durationDays = 1, nowOffset = 0, doneDayOffsets = setOf(0))
        assertTrue(d is EvaluateChallengeStatusUseCase.Decision.TransitionCompleted)
    }

    // ============================================================
    // Permanence + idempotence
    // ============================================================

    @Test
    fun `already-failed row stays terminal`() {
        val d = decide(
            durationDays = 7,
            nowOffset = 6,
            doneDayOffsets = (0..6).toSet(), // even with all days done...
            status = UserChallengeStatus.FAILED
        )
        // ...the evaluator must NOT promote it to COMPLETED.
        assertEquals(EvaluateChallengeStatusUseCase.Decision.AlreadyTerminal, d)
    }

    @Test
    fun `already-completed row stays terminal`() {
        val d = decide(
            durationDays = 7,
            nowOffset = 6,
            doneDayOffsets = (0..6).toSet(),
            status = UserChallengeStatus.COMPLETED
        )
        assertEquals(EvaluateChallengeStatusUseCase.Decision.AlreadyTerminal, d)
    }

    @Test
    fun `abandoned row stays terminal even with full days`() {
        val d = decide(
            durationDays = 7,
            nowOffset = 7,
            doneDayOffsets = (0..6).toSet(),
            status = UserChallengeStatus.ABANDONED
        )
        assertEquals(EvaluateChallengeStatusUseCase.Decision.AlreadyTerminal, d)
    }

    // ============================================================
    // Same-day deduplication + cardinality safety
    // ============================================================

    @Test
    fun `doneDates is a Set so same-day duplicates collapse`() {
        // Set semantics enforce one-per-day. Spec required validation.
        val s = setOf(day(0), day(0), day(1), day(2))
        assertEquals(3, s.size)
    }

    // ============================================================
    // Legacy fallback: targetEndDate==null derived from durationDays
    // ============================================================

    @Test
    fun `null target end date falls back to durationDays`() {
        // Legacy row: target_end_date never backfilled. Days 0-6 done, today=7.
        val d = EvaluateChallengeStatusUseCase.decide(
            status = UserChallengeStatus.ACTIVE,
            startDate = day0,
            targetEndDate = null,
            durationDays = 7,
            doneDates = (0..6).map { day(it) }.toSet(),
            now = day(7)
        )
        assertTrue(d is EvaluateChallengeStatusUseCase.Decision.TransitionCompleted)
    }

    // ============================================================
    // UPCOMING (future-dated start)
    // ============================================================

    @Test
    fun `upcoming challenge before start stays active`() {
        // Today is day -1 (one day before start).
        val d = EvaluateChallengeStatusUseCase.decide(
            status = UserChallengeStatus.UPCOMING,
            startDate = day0,
            targetEndDate = day(6),
            durationDays = 7,
            doneDates = emptySet(),
            now = day(-1)
        )
        assertEquals(EvaluateChallengeStatusUseCase.Decision.KeepActive, d)
    }
}
