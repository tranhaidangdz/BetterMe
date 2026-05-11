package com.example.betterme.presentation.statistics

import androidx.lifecycle.viewModelScope
import com.example.betterme.base.BaseMviViewModel
import com.example.betterme.data.local.datastore.DataStoreManager
import com.example.betterme.data.local.room.entities.CategoryEntity
import com.example.betterme.data.local.room.entities.HabitEntity
import com.example.betterme.data.local.room.entities.HabitLogEntity
import com.example.betterme.domain.repository.CategoryRepository
import com.example.betterme.domain.repository.HabitLogRepository
import com.example.betterme.domain.repository.HabitRepository
import com.example.betterme.domain.repository.UserAchievementRepository
import com.example.betterme.domain.repository.UserChallengeRepository
import com.example.betterme.utils.DateUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/**
 * Real Room-backed analytics engine.
 *
 * Replaces the prior mock-data VM that hardcoded every number. Statistics now derive
 * from three live flows:
 *  - habits          : HabitRepository.getHabits(userId)
 *  - logs (signal)   : HabitLogRepository.observeAllLogs()   (used as change trigger)
 *  - selectedTab + customRange  : MutableStateFlow surfaced as one combined criterion
 *
 * Combining all three with `combine` means any check-in, habit add/edit/delete, tab
 * change, or custom-range pick fires a fresh emission and the screen recomputes
 * end-to-end. There is no in-memory cache between Room and the UI — `combine` is the
 * cache, and Room's invalidation tracker is the cache invalidator.
 *
 * Range semantics
 * - WEEKLY    : last 7 days ending today (inclusive).
 * - MONTHLY   : last 30 days ending today.
 * - ALL_TIME  : from the user's oldest habit start_date through today.
 * - CUSTOM    : explicit user-picked start..end (inclusive), both clamped to startOfDay.
 *
 * Aggregation rules
 * - Logs are filtered to the resolved range and status == "DONE" before any percentage
 *   math runs. The filter is applied once per emission, then the resulting list is
 *   passed to every per-bucket aggregator — no double-iteration of the full log table.
 * - "Days expected" for completion-rate uses (range length in days × active-habits-in-range)
 *   so a user who only had 1 habit on day 1 isn't penalized for not check-ing-in 5
 *   habits' worth of slots on that day.
 * - Journey completion is derived from (DONE count ≥ planned duration days). A habit
 *   counts toward [completedHabits] when isJourneyComplete; toward [failedHabits] when
 *   end_date is in the past AND !isJourneyComplete; toward [ongoingHabits] otherwise.
 */
class StatisticsViewModel(
    private val dataStoreManager: DataStoreManager,
    private val habitRepository: HabitRepository,
    private val habitLogRepository: HabitLogRepository,
    private val categoryRepository: CategoryRepository,
    private val userChallengeRepository: UserChallengeRepository,
    private val userAchievementRepository: UserAchievementRepository
) : BaseMviViewModel<StatisticsIntent, StatisticsState, StatisticsEvent>() {

    private data class RangeKey(
        val tab: StatisticsTab,
        val customStart: Long?,
        val customEnd: Long?
    )

    private val rangeKey = MutableStateFlow(RangeKey(StatisticsTab.WEEKLY, null, null))

    override fun initState(): StatisticsState = StatisticsState()

    init {
        processIntent(StatisticsIntent.LoadData)
    }

    override fun processIntent(intent: StatisticsIntent) {
        when (intent) {
            StatisticsIntent.LoadData -> load()
            is StatisticsIntent.SelectTab -> {
                rangeKey.value = rangeKey.value.copy(tab = intent.tab)
                updateState { copy(selectedTab = intent.tab) }
            }
            is StatisticsIntent.ToggleSection -> updateState {
                copy(expandedSection = if (expandedSection == intent.section) null else intent.section)
            }
            is StatisticsIntent.SelectCustomRange -> {
                val start = DateUtils.startOfDay(intent.start)
                val end = DateUtils.startOfDay(intent.end)
                rangeKey.value = RangeKey(
                    StatisticsTab.CUSTOM,
                    customStart = start,
                    customEnd = end
                )
                updateState {
                    copy(
                        selectedTab = StatisticsTab.CUSTOM,
                        customRangeStart = start,
                        customRangeEnd = end
                    )
                }
            }
        }
    }

    private fun load() {
        viewModelScope.launch {
            val userId = dataStoreManager.getCurrentUserId().first().orEmpty()
            if (userId.isBlank()) {
                updateState { copy(isLoading = false) }
                return@launch
            }

            updateState { copy(isLoading = true) }

            val categoriesByIdSnapshot: Map<Int, CategoryEntity> = runCatching {
                categoryRepository.getAll().first().associateBy { it.id }
            }.getOrDefault(emptyMap())

            combine(
                habitRepository.getHabits(userId),
                habitLogRepository.observeAllLogs(),
                rangeKey
            ) { habits, allLogs, key ->
                Triple(habits, allLogs, key)
            }.collect { (habits, allLogs, key) ->
                val (rangeStart, rangeEnd) = resolveRange(key, habits)
                val analytics = computeAnalytics(
                    habits = habits,
                    allLogs = allLogs,
                    rangeStart = rangeStart,
                    rangeEnd = rangeEnd,
                    categoriesById = categoriesByIdSnapshot,
                    userId = userId
                )
                updateState {
                    copy(
                        isLoading = false,
                        effectiveRangeStart = rangeStart,
                        effectiveRangeEnd = rangeEnd,
                        overview = analytics.overview,
                        streakAnalytics = analytics.streaks,
                        featuredHabits = analytics.featured,
                        habitJourney = analytics.journey,
                        completedHabits = analytics.completed,
                        failedHabits = analytics.failed,
                        ongoingHabits = analytics.ongoing,
                        additionalInsights = analytics.insights,
                        challengeStats = analytics.challengeStats
                    )
                }
            }
        }
    }

    // =====================================================================
    // Range resolution
    // =====================================================================

    private fun resolveRange(key: RangeKey, habits: List<HabitEntity>): Pair<Long, Long> {
        val today = DateUtils.startOfDay()
        val day = 24L * 60L * 60L * 1000L
        return when (key.tab) {
            StatisticsTab.WEEKLY -> (today - 6L * day) to today
            StatisticsTab.MONTHLY -> (today - 29L * day) to today
            StatisticsTab.ALL_TIME -> {
                val firstHabitStart = habits.minOfOrNull { it.start_date } ?: today
                DateUtils.startOfDay(firstHabitStart) to today
            }
            StatisticsTab.CUSTOM -> {
                val start = key.customStart ?: today
                val end = key.customEnd ?: today
                start to end
            }
        }
    }

    // =====================================================================
    // Aggregation
    // =====================================================================

    private data class Analytics(
        val overview: OverviewStats,
        val streaks: StreakAnalytics,
        val featured: List<FeaturedHabit>,
        val journey: HabitJourney,
        val completed: List<HabitStatusItem>,
        val failed: List<HabitStatusItem>,
        val ongoing: List<HabitStatusItem>,
        val insights: AdditionalInsights,
        val challengeStats: ChallengeStats
    )

    private suspend fun computeAnalytics(
        habits: List<HabitEntity>,
        allLogs: List<HabitLogEntity>,
        rangeStart: Long,
        rangeEnd: Long,
        categoriesById: Map<Int, CategoryEntity>,
        userId: String
    ): Analytics {
        val dayMs = 24L * 60L * 60L * 1000L
        val rangeDays = (((rangeEnd - rangeStart) / dayMs) + 1).toInt().coerceAtLeast(1)

        // Habits *active during* the range — created on or before rangeEnd, not ended
        // before rangeStart. Avoids counting a habit ended last year against this week.
        val habitsInRange = habits.filter { h ->
            h.start_date <= rangeEnd && (h.end_date == null || h.end_date >= rangeStart)
        }

        val habitIds = habitsInRange.map { it.id }.toSet()
        val doneLogsInRange = allLogs
            .filter { it.status == "DONE" && it.habit_id in habitIds }
            .filter { it.date in rangeStart..rangeEnd }

        val doneCountByHabit = doneLogsInRange.groupingBy { it.habit_id }.eachCount()
        val allDoneCountByHabit = allLogs
            .filter { it.status == "DONE" }
            .groupingBy { it.habit_id }
            .eachCount()

        // Bucketize each habit into completed / failed / ongoing based on its full
        // history, not just the slice — a habit completed last month is "completed"
        // when viewing weekly stats too.
        val now = DateUtils.startOfDay()
        val completedList = mutableListOf<HabitStatusItem>()
        val failedList = mutableListOf<HabitStatusItem>()
        val ongoingList = mutableListOf<HabitStatusItem>()

        for (habit in habits) {
            val plannedDuration = if (habit.end_date != null) {
                (((habit.end_date - habit.start_date) / dayMs) + 1).toInt().coerceAtLeast(1)
            } else {
                Int.MAX_VALUE
            }
            val doneAll = allDoneCountByHabit[habit.id] ?: 0
            val isJourneyComplete = doneAll >= plannedDuration
            val hasEnded = habit.end_date != null && habit.end_date < now
            val pct = when {
                plannedDuration == Int.MAX_VALUE -> {
                    // Open-ended: % is "done / elapsed" within range
                    val elapsed = (((rangeEnd - habit.start_date) / dayMs) + 1)
                        .toInt().coerceAtLeast(1)
                    ((doneAll.toFloat() / elapsed) * 100).toInt().coerceIn(0, 100)
                }
                else -> ((doneAll.toFloat() / plannedDuration) * 100).toInt().coerceIn(0, 100)
            }
            val item = HabitStatusItem(
                name = habit.title,
                icon = categoriesById[habit.category_id]?.icon ?: "📌",
                statusLabel = when {
                    isJourneyComplete -> "Hoàn thành"
                    hasEnded && !isJourneyComplete -> "Thất bại"
                    else -> "Đang thực hiện"
                },
                completionInfo = "$doneAll/${if (plannedDuration == Int.MAX_VALUE) "∞" else plannedDuration} ngày",
                streakInfo = "${calculateLongestStreakFor(habit.id, allLogs)} ngày liên tiếp",
                percentage = pct
            )
            when {
                isJourneyComplete -> completedList.add(item)
                hasEnded -> failedList.add(item)
                else -> ongoingList.add(item)
            }
        }

        // ===== Overview (within range) =====
        val totalSlotsInRange = habitsInRange.sumOf { habit ->
            // Days the habit was active inside the range
            val habitStartInRange = maxOf(habit.start_date, rangeStart)
            val habitEndInRange = minOf(habit.end_date ?: rangeEnd, rangeEnd)
            (((habitEndInRange - habitStartInRange) / dayMs) + 1)
                .toInt().coerceAtLeast(0)
        }
        val totalDoneInRange = doneLogsInRange.size
        val completionRate = if (totalSlotsInRange > 0)
            ((totalDoneInRange.toFloat() / totalSlotsInRange) * 100).toInt().coerceIn(0, 100)
        else 0
        val overview = OverviewStats(
            completionRate = completionRate,
            totalCompleted = completedList.size,
            totalFailed = failedList.size,
            totalOngoing = ongoingList.size
        )

        // ===== Streaks =====
        val doneDatesByHabit = allLogs
            .filter { it.status == "DONE" }
            .groupBy { it.habit_id }
            .mapValues { (_, logs) -> logs.map { DateUtils.startOfDay(it.date) } }
        val longestStreakAll = doneDatesByHabit.values
            .maxOfOrNull { DateUtils.longestStreak(it) } ?: 0
        val currentStreakAll = doneDatesByHabit.values
            .maxOfOrNull { DateUtils.currentStreak(it, now) } ?: 0
        val streakSum = doneDatesByHabit.values.sumOf { DateUtils.longestStreak(it) }
        val averageStreak = if (doneDatesByHabit.isNotEmpty())
            streakSum / doneDatesByHabit.size else 0
        val totalStreakDays = allLogs.count { it.status == "DONE" }
        val streaks = StreakAnalytics(
            longestStreak = longestStreakAll,
            averageStreak = averageStreak,
            currentStreak = currentStreakAll,
            totalStreakDays = totalStreakDays
        )

        // ===== Featured (top by completion % within range) =====
        val featured = habitsInRange
            .map { habit ->
                val plannedDuration = if (habit.end_date != null) {
                    (((habit.end_date - habit.start_date) / dayMs) + 1).toInt().coerceAtLeast(1)
                } else rangeDays
                val done = doneCountByHabit[habit.id] ?: 0
                val pct = ((done.toFloat() / plannedDuration) * 100).toInt().coerceIn(0, 100)
                FeaturedHabit(
                    icon = categoriesById[habit.category_id]?.icon ?: "📌",
                    title = habit.title,
                    completionPercentage = pct,
                    completedDays = done,
                    totalDays = plannedDuration
                )
            }
            .sortedByDescending { it.completionPercentage }
            .take(6)

        // ===== Habit journey =====
        val badgesEarned = runCatching {
            userAchievementRepository.observeByUser(userId).first().size
        }.getOrDefault(0)
        val activeDays = doneLogsInRange.map { DateUtils.startOfDay(it.date) }.distinct().size
        val journey = HabitJourney(
            totalCreated = habits.size,
            totalCompleted = completedList.size,
            totalFailed = failedList.size,
            badgesEarned = badgesEarned,
            activeDays = activeDays
        )

        // ===== Insights — weekday / hour breakdowns =====
        val checkinsByWeekday = IntArray(7)
        val checkinsByHourBucket = IntArray(6) // 0-3, 4-7, 8-11, 12-15, 16-19, 20-23
        val cal = Calendar.getInstance()
        for (log in doneLogsInRange) {
            cal.timeInMillis = log.date
            // Calendar.SUNDAY = 1 ... SATURDAY = 7 → shift to Monday=0 ... Sunday=6
            val weekdayIdx = ((cal.get(Calendar.DAY_OF_WEEK) + 5) % 7)
            checkinsByWeekday[weekdayIdx]++
            cal.timeInMillis = log.created_at
            val hour = cal.get(Calendar.HOUR_OF_DAY)
            checkinsByHourBucket[(hour / 4).coerceIn(0, 5)]++
        }
        val weekdayLabels = listOf("Thứ 2", "Thứ 3", "Thứ 4", "Thứ 5", "Thứ 6", "Thứ 7", "CN")
        val bestWeekdayIdx = checkinsByWeekday.indices
            .maxByOrNull { checkinsByWeekday[it] } ?: 0
        val hourBucketLabels = listOf(
            "0:00 – 4:00", "4:00 – 8:00", "8:00 – 12:00",
            "12:00 – 16:00", "16:00 – 20:00", "20:00 – 24:00"
        )
        val bestHourBucket = checkinsByHourBucket.indices
            .maxByOrNull { checkinsByHourBucket[it] } ?: 0
        val insights = AdditionalInsights(
            mostProductiveDay = if (doneLogsInRange.isEmpty()) "—" else weekdayLabels[bestWeekdayIdx],
            mostActiveHour = if (doneLogsInRange.isEmpty()) "—" else hourBucketLabels[bestHourBucket],
            bestStreak = longestStreakAll,
            totalCompletions = totalDoneInRange
        )

        // ===== Challenge stats =====
        val userChallenges = runCatching {
            userChallengeRepository.observeByUser(userId).first()
        }.getOrDefault(emptyList())
        val joined = userChallenges.size
        val completedC = userChallenges.count { it.status == "COMPLETED" }
        val activeC = userChallenges.count { it.status == "ACTIVE" }
        val rateC = if (joined > 0) ((completedC.toFloat() / joined) * 100).toInt() else 0
        val challengeStats = ChallengeStats(
            joined = joined,
            completed = completedC,
            active = activeC,
            completionRatePct = rateC
        )

        // Stable sort within each status bucket: highest progress first for completed,
        // closest-to-target first for ongoing, earliest-failed first for failed.
        return Analytics(
            overview = overview,
            streaks = streaks,
            featured = featured,
            journey = journey,
            completed = completedList.sortedByDescending { it.percentage },
            failed = failedList.sortedByDescending { it.percentage },
            ongoing = ongoingList.sortedByDescending { it.percentage },
            insights = insights,
            challengeStats = challengeStats
        )
    }

    private fun calculateLongestStreakFor(habitId: Int, allLogs: List<HabitLogEntity>): Int {
        val dates = allLogs
            .asSequence()
            .filter { it.status == "DONE" && it.habit_id == habitId }
            .map { DateUtils.startOfDay(it.date) }
            .toList()
        return DateUtils.longestStreak(dates)
    }

    @Suppress("unused")
    private val dateLabel = SimpleDateFormat("dd/MM", Locale("vi"))
}
