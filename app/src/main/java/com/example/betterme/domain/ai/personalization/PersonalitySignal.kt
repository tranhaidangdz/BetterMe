package com.example.betterme.domain.ai.personalization

import com.example.betterme.data.local.room.entities.HabitEntity
import com.example.betterme.data.local.room.entities.HabitLogEntity

/**
 * Lightweight behavioral signals derived from existing habit + log data.
 * No schema migration needed — every signal is computable from what's
 * already in Room.
 *
 * Used to diversify AI outputs (both online prompts and canned templates)
 * so two different users with very different rhythms don't see the same
 * coaching text just because their group rollup happens to land at 60%
 * completion. The repo passes the signal names verbatim into the user
 * prompt; the canned template pool indexes by signal too so offline
 * users still get differentiated copy.
 *
 * Signals are intentionally coarse (boolean: present / absent) rather
 * than scored. The AI doesn't need a precise score; "the user is a night
 * owl" is enough to anchor the coaching tone. Keep the heuristics simple
 * — adding subtle thresholds invites drift between online + canned tone.
 */
enum class PersonalitySignal(
    /** Vietnamese label used in prompts + canned coaching copy. */
    val vietnameseLabel: String,
    /** Short English tag emitted into the user prompt for the model. */
    val tag: String
) {
    /** ≥ 50% of reminders are at 21:00 or later. */
    NIGHT_OWL("Cú đêm", "night_owl"),
    /** ≥ 50% of reminders are before 09:00. */
    EARLY_STARTER("Người dậy sớm", "early_starter"),
    /** > 8 active habits. */
    OVERLOADED("Đang quá tải", "overloaded"),
    /** Overall completion ≥ 85% AND ≥ 3 active habits. */
    HIGHLY_DISCIPLINED("Rất kỷ luật", "highly_disciplined"),
    /** > 30% of habits have <7 day-since-start AND no DONE log. */
    INCONSISTENT_STARTER("Bắt đầu chưa đều", "inconsistent_starter"),
    /** Overall completion < 40% OR ≥ 2 habits with miss streak ≥ 3. */
    RECOVERY_NEEDING("Cần phục hồi", "recovery_needing"),
    /** 7d completion ≥ 14d completion + 10. Trending up. */
    STEADY_IMPROVER("Đang tiến đều", "steady_improver");

    companion object {

        /**
         * Cheap derivation across the user's full habit set. Pure function
         * (no I/O) — callers pre-fetch habits + logs and pass them in. Pass
         * `todayMs = DateUtils.startOfDay()` so the window math lines up
         * with the rest of the analytics pipeline.
         *
         * Signals are not mutually exclusive — a user can be both a NIGHT_OWL
         * and OVERLOADED. Two signals max is the typical case in practice.
         */
        fun derive(
            habits: List<HabitEntity>,
            logsByHabitId: Map<Int, List<HabitLogEntity>>,
            todayMs: Long
        ): Set<PersonalitySignal> {
            if (habits.isEmpty()) return emptySet()

            val active = habits.filter { it.end_date == null || it.end_date >= todayMs }
            val out = mutableSetOf<PersonalitySignal>()

            // ── Time-of-day signals ─────────────────────────────────
            val withReminder = active.mapNotNull { habit ->
                habit.reminder_time?.split(":")?.firstOrNull()?.toIntOrNull()
            }
            if (withReminder.isNotEmpty()) {
                val nightShare = withReminder.count { it >= 21 }.toFloat() / withReminder.size
                val morningShare = withReminder.count { it < 9 }.toFloat() / withReminder.size
                if (nightShare >= 0.5f) out += NIGHT_OWL
                if (morningShare >= 0.5f) out += EARLY_STARTER
            }

            // ── Workload signal ─────────────────────────────────────
            if (active.size > 8) out += OVERLOADED

            // ── Completion-based signals ────────────────────────────
            var totalPlanned = 0
            var totalDone = 0
            var freshAndQuiet = 0
            var stalledCount = 0
            var rate7Sum = 0
            var rate14Sum = 0
            var habitsWithEnoughHistory = 0
            for (habit in active) {
                val daysSinceStart = (((todayMs - habit.start_date) / DAY_MS) + 1)
                    .toInt().coerceAtLeast(1)
                val logs = logsByHabitId[habit.id].orEmpty()
                val doneDays = logs.filter { it.status == "DONE" }.map { it.date }.toSet()

                totalPlanned += daysSinceStart.coerceAtMost(30) // last 30 days view
                val recentDone = logs.count {
                    it.status == "DONE" && it.date >= todayMs - 30 * DAY_MS
                }
                totalDone += recentDone

                if (daysSinceStart < 7 && doneDays.isEmpty()) freshAndQuiet++
                val missStreak = computeMissStreak(doneDays, todayMs)
                if (missStreak >= 3) stalledCount++

                if (daysSinceStart >= 14) {
                    rate7Sum += completionRate(doneDays, todayMs, 7, daysSinceStart, 0)
                    rate14Sum += completionRate(doneDays, todayMs, 14, daysSinceStart, 0)
                    habitsWithEnoughHistory++
                }
            }
            val overallRate = if (totalPlanned > 0) (totalDone * 100) / totalPlanned else 0
            val freshShare = if (active.isNotEmpty()) freshAndQuiet.toFloat() / active.size else 0f
            if (freshShare > 0.3f) out += INCONSISTENT_STARTER
            if (overallRate < 40 || stalledCount >= 2) out += RECOVERY_NEEDING
            if (overallRate >= 85 && active.size >= 3) out += HIGHLY_DISCIPLINED

            // ── Trend signal ────────────────────────────────────────
            if (habitsWithEnoughHistory > 0) {
                val avg7 = rate7Sum / habitsWithEnoughHistory
                val avg14 = rate14Sum / habitsWithEnoughHistory
                if (avg7 >= avg14 + 10) out += STEADY_IMPROVER
            }

            return out
        }

        private const val DAY_MS: Long = 24L * 60L * 60L * 1000L

        private fun computeMissStreak(doneDays: Set<Long>, todayMs: Long): Int {
            if (todayMs in doneDays) return 0
            var streak = 0
            var cursor = todayMs
            for (i in 1..14) {
                cursor -= DAY_MS
                if (cursor in doneDays) break
                streak++
            }
            return streak
        }

        private fun completionRate(
            doneDays: Set<Long>,
            todayMs: Long,
            windowDays: Int,
            daysSinceStart: Int,
            offsetDays: Int
        ): Int {
            if (daysSinceStart <= offsetDays) return 0
            val windowEnd = todayMs - offsetDays * DAY_MS
            val windowStart = windowEnd - windowDays * DAY_MS
            val done = doneDays.count { it in windowStart..windowEnd }
            val denom = minOf(windowDays, daysSinceStart - offsetDays).coerceAtLeast(1)
            return (done * 100 / denom).coerceIn(0, 100)
        }
    }
}
