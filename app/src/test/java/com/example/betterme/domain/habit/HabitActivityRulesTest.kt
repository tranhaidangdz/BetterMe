package com.example.betterme.domain.habit

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Contract for [HabitActivityRules] — the single source of truth for whether a
 * habit belongs in the Home "Đang thực hiện" section.
 *
 * All dates are day-aligned epoch-millis (start-of-day), matching how the app
 * normalizes start_date / end_date at write time and how the Home ticker emits
 * `today`.
 */
class HabitActivityRulesTest {

    private val day = HabitActivityRules.DAY_MS
    // Fixed reference "today" = an arbitrary start-of-day.
    private val today = 1_900_000L * 1000L - (1_900_000L * 1000L % day)

    private fun daysFromToday(n: Int): Long = today + n * day

    // ---------------------------------------------------------------
    // durationDays
    // ---------------------------------------------------------------
    @Test
    fun `duration is inclusive of both endpoints`() {
        // start == end → 1-day journey.
        assertEquals(1, HabitActivityRules.durationDays(today, today))
        // 30-day journey: end = start + 29 days.
        assertEquals(30, HabitActivityRules.durationDays(today, daysFromToday(29)))
    }

    @Test
    fun `open-ended duration is the sentinel`() {
        assertEquals(
            HabitActivityRules.OPEN_ENDED_DURATION,
            HabitActivityRules.durationDays(today, null)
        )
    }

    // ---------------------------------------------------------------
    // Active visibility
    // ---------------------------------------------------------------
    @Test
    fun `fresh habit starting today with no completions is active`() {
        val active = HabitActivityRules.isActive(
            startDate = today,
            endDate = daysFromToday(29),
            doneCount = 0,
            today = today,
            isDeleted = false
        )
        assertTrue("a brand-new 30-day habit must show on Home", active)
    }

    @Test
    fun `open-ended habit is always active when started and not deleted`() {
        val active = HabitActivityRules.isActive(
            startDate = daysFromToday(-100),
            endDate = null,
            doneCount = 80,
            today = today,
            isDeleted = false
        )
        assertTrue("open-ended habits never auto-expire / auto-complete", active)
    }

    // ---------------------------------------------------------------
    // Completed removal
    // ---------------------------------------------------------------
    @Test
    fun `completed journey is removed`() {
        // 30-day journey, 30 DONE days → complete.
        val active = HabitActivityRules.isActive(
            startDate = daysFromToday(-29),
            endDate = today,
            doneCount = 30,
            today = today,
            isDeleted = false
        )
        assertFalse("a fully-completed journey must drop off Home", active)
        assertTrue(HabitActivityRules.isComplete(30, daysFromToday(-29), today))
    }

    @Test
    fun `over-completed journey is also removed`() {
        val active = HabitActivityRules.isActive(
            startDate = daysFromToday(-29),
            endDate = today,
            doneCount = 35,
            today = today,
            isDeleted = false
        )
        assertFalse(active)
    }

    // ---------------------------------------------------------------
    // Expired / failed removal
    // ---------------------------------------------------------------
    @Test
    fun `expired window with incomplete journey is removed (failed)`() {
        // 30-day journey that ended yesterday; user only did 5 days → failed.
        val endedYesterday = daysFromToday(-1)
        val active = HabitActivityRules.isActive(
            startDate = daysFromToday(-30),
            endDate = endedYesterday,
            doneCount = 5,
            today = today,
            isDeleted = false
        )
        assertFalse("an expired-but-incomplete habit (failed) must drop off Home", active)
        assertTrue(HabitActivityRules.isExpired(endedYesterday, today))
    }

    @Test
    fun `habit is still active on its final day`() {
        // end_date == today → the user still has all of today to finish. Active.
        val active = HabitActivityRules.isActive(
            startDate = daysFromToday(-29),
            endDate = today,
            doneCount = 10,
            today = today,
            isDeleted = false
        )
        assertTrue("habit must remain active through the whole of its end day", active)
        assertFalse(HabitActivityRules.isExpired(today, today))
    }

    @Test
    fun `habit expires the day after its end date — boundary`() {
        val endDate = daysFromToday(-1) // ended yesterday
        assertTrue("today is strictly past end → expired", HabitActivityRules.isExpired(endDate, today))
        // And exactly on the boundary (end == today) it is NOT expired.
        assertFalse(HabitActivityRules.isExpired(today, today))
    }

    // ---------------------------------------------------------------
    // Abandoned removal
    // ---------------------------------------------------------------
    @Test
    fun `soft-deleted habit is removed even if otherwise active`() {
        val active = HabitActivityRules.isActive(
            startDate = today,
            endDate = daysFromToday(29),
            doneCount = 0,
            today = today,
            isDeleted = true
        )
        assertFalse("an abandoned (soft-deleted) habit must never show", active)
    }

    // ---------------------------------------------------------------
    // Not-yet-started removal
    // ---------------------------------------------------------------
    @Test
    fun `future-dated habit is not active yet`() {
        val active = HabitActivityRules.isActive(
            startDate = daysFromToday(3),
            endDate = daysFromToday(33),
            doneCount = 0,
            today = today,
            isDeleted = false
        )
        assertFalse("a habit scheduled to start in 3 days isn't in progress yet", active)
    }

    // ---------------------------------------------------------------
    // progressPercent
    // ---------------------------------------------------------------
    @Test
    fun `progress percent for fixed-duration habit`() {
        // 30-day journey, 15 done → 50%.
        val pct = HabitActivityRules.progressPercent(
            startDate = daysFromToday(-14),
            endDate = daysFromToday(15),
            doneCount = 15,
            today = today
        )
        assertEquals(50, pct)
    }

    @Test
    fun `progress percent clamps to 100`() {
        val pct = HabitActivityRules.progressPercent(
            startDate = daysFromToday(-29),
            endDate = today,
            doneCount = 40,
            today = today
        )
        assertEquals(100, pct)
    }

    @Test
    fun `progress percent for open-ended uses elapsed days`() {
        // Started 9 days ago (10 elapsed inclusive), 5 done → 50%.
        val pct = HabitActivityRules.progressPercent(
            startDate = daysFromToday(-9),
            endDate = null,
            doneCount = 5,
            today = today
        )
        assertEquals(50, pct)
    }
}
