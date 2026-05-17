package com.example.betterme.presentation.dailyhabits

import androidx.lifecycle.viewModelScope
import com.example.betterme.base.BaseMviViewModel
import com.example.betterme.data.local.datastore.DataStoreManager
import com.example.betterme.data.local.room.entities.HabitEntity
import com.example.betterme.domain.repository.CategoryRepository
import com.example.betterme.domain.repository.HabitLogRepository
import com.example.betterme.domain.repository.HabitRepository
import com.example.betterme.presentation.dailyhabits.model.HabitUiModel
import com.example.betterme.utils.DateUtils
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/**
 * Backs the Tasks bottom-nav tab.
 *
 * The visible list is computed reactively from three sources:
 * 1. The user's habits ([HabitRepository.getHabits]) — Flow, emits on add/edit/delete.
 * 2. Every mutation of the habit-logs table ([HabitLogRepository.observeAllLogs])
 *    — used purely as a change signal so a check-in landing anywhere in the app pushes
 *    a fresh emission through this VM and the Tasks list updates without a manual
 *    reload. The list itself isn't consumed; only its existence as a Flow.
 * 3. The currently selected date ([selectedDate]) — drives "isCheckedInToday" lookups.
 *
 * Combining these three with [combine] means the visible list is always derived from
 * fresh DB state. Journey-completion is recomputed on every emission via
 * `countCompleted(habit.id)`, so a habit that just hit its target appears under
 * "Đã hoàn thành" the moment the check-in commits.
 */
private const val DAY_MS: Long = 24L * 60L * 60L * 1000L

class DailyHabitsViewModel(
    private val dataStoreManager: DataStoreManager,
    private val habitRepository: HabitRepository,
    private val habitLogRepository: HabitLogRepository,
    private val categoryRepository: CategoryRepository,
) : BaseMviViewModel<DailyHabitsIntent, DailyHabitsState, DailyHabitsEvent>() {

    private val selectedDateMillis = MutableStateFlow(startOfDay(Calendar.getInstance()))

    /**
     * Tracks the live bootstrap coroutine so a re-entrant [DailyHabitsIntent.LoadData]
     * or a midnight rollover doesn't leak a second infinite `combine` collector on
     * top of the first. The cancel-then-relaunch pattern keeps the VM safe even if
     * the screen gains a manual refresh trigger in the future.
     */
    private var bootstrapJob: Job? = null

    override fun initState(): DailyHabitsState = DailyHabitsState()

    init {
        processIntent(DailyHabitsIntent.LoadData)
    }

    override fun processIntent(intent: DailyHabitsIntent) {
        when (intent) {
            DailyHabitsIntent.LoadData -> bootstrap()
            is DailyHabitsIntent.SelectDate -> selectDate(intent.index)
            is DailyHabitsIntent.SelectFilter -> selectFilter(intent.filter)
        }
    }

    /**
     * One-time setup of the date strip + the long-lived combine that keeps the
     * visible list in sync with Room. Internally hosts a midnight-tick coroutine
     * that re-runs the strip rebuild every time the local calendar day rolls
     * over — so a user who keeps the app open across 00:00 sees the date strip
     * advance, and check-ins land under the new day.
     */
    private fun bootstrap() {
        bootstrapJob?.cancel()
        bootstrapJob = viewModelScope.launch {
            updateState { copy(isLoading = true) }
            rebuildDateStripForCurrentDay(initial = true)

            val userId = dataStoreManager.getCurrentUserId().first().orEmpty()
            if (userId.isBlank()) {
                updateState { copy(isLoading = false) }
                return@launch
            }

            val categoryMap = runCatching {
                categoryRepository.getAll().first().associateBy { it.id }
            }.getOrDefault(emptyMap())

            // Midnight rollover ticker. Sleeps until the next local 00:00, then
            // rebuilds the date strip in place. The user's selected day is preserved
            // when possible (we find the same absolute date in the new strip);
            // otherwise we snap to the new today. Lives as a child of bootstrapJob
            // so it dies with the VM and re-spawns on a fresh bootstrap.
            launch { runMidnightTicker() }

            // collectLatest cancels in-flight buildUiHabits when a newer (habits,
            // date, logs) tuple arrives, so a rapid sequence of check-ins or
            // tab-switches only commits the freshest list to state. With plain
            // collect, every emission queues — under burst load that would surface
            // briefly as stale rows before the latest list lands.
            combine(
                habitRepository.getHabits(userId),
                selectedDateMillis,
                habitLogRepository.observeAllLogs()
            ) { habits, dateMs, _ ->
                buildUiHabits(habits, dateMs, categoryMap)
            }.collectLatest { uiHabits ->
                updateState {
                    copy(
                        isLoading = false,
                        allHabits = uiHabits,
                        visibleHabits = applyFilter(currentState.selectedFilter, uiHabits)
                    )
                }
            }
        }
    }

    /**
     * Builds the 7-day strip centered on today and writes it into state. When
     * [initial] is true we always snap selection to today (fresh load). When
     * called from the midnight ticker, we preserve the user's absolute selected
     * date if it's still in the new window, otherwise we snap to the new today.
     */
    private fun rebuildDateStripForCurrentDay(initial: Boolean) {
        val todayIndex = 3
        val dates = (-3..3).map { offset ->
            val cal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, offset) }
            DateUiModel(
                month = SimpleDateFormat("MMM", Locale("vi")).format(cal.time),
                day = SimpleDateFormat("dd", Locale.getDefault()).format(cal.time),
                weekDay = SimpleDateFormat("EEE", Locale("vi")).format(cal.time),
                dateMillis = startOfDay(cal),
                isToday = offset == 0
            )
        }
        val previousSelectedMillis = selectedDateMillis.value
        val preservedIndex = dates.indexOfFirst { it.dateMillis == previousSelectedMillis }
        val newSelectedIndex = when {
            initial -> todayIndex
            preservedIndex >= 0 -> preservedIndex
            else -> todayIndex
        }
        updateState {
            copy(
                dates = dates,
                todayIndex = todayIndex,
                selectedDateIndex = newSelectedIndex
            )
        }
        selectedDateMillis.value = dates[newSelectedIndex].dateMillis
    }

    /**
     * Sleeps until the next local 00:00 (plus a 1s safety buffer to land *after*
     * the boundary), rebuilds the strip, then loops. The delay is computed against
     * wall-clock time so a backgrounded VM whose process survived across midnight
     * still fires exactly once when the user resumes.
     *
     * `while (true)` is safe: [delay] is a cancellation point, so when
     * [bootstrapJob] is cancelled (VM clear or re-bootstrap) the loop exits
     * cleanly via CancellationException without leaking the coroutine.
     */
    private suspend fun runMidnightTicker() {
        while (true) {
            val now = System.currentTimeMillis()
            val nextMidnight = Calendar.getInstance().apply {
                add(Calendar.DAY_OF_YEAR, 1)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis
            // Minimum 1-minute sleep prevents an accidental hot-loop if the system
            // clock is set exactly at midnight when we enter this coroutine.
            delay((nextMidnight - now + 1_000L).coerceAtLeast(60_000L))
            rebuildDateStripForCurrentDay(initial = false)
        }
    }

    private fun selectDate(index: Int) {
        if (index !in currentState.dates.indices) return
        updateState { copy(selectedDateIndex = index) }
        selectedDateMillis.value = currentState.dates[index].dateMillis
    }

    private fun selectFilter(filter: DailyHabitFilter) {
        updateState {
            copy(
                selectedFilter = filter,
                visibleHabits = applyFilter(filter, allHabits)
            )
        }
    }

    /**
     * Builds the row list for [dateMs]. For every active-on-that-date habit:
     * - looks up DONE-count vs the planned duration to decide journey completion
     * - looks up the selected day's check-in status
     * Open-ended habits (no `end_date`) never auto-complete the journey.
     */
    private suspend fun buildUiHabits(
        habits: List<HabitEntity>,
        dateMs: Long,
        categoryMap: Map<Int, com.example.betterme.data.local.room.entities.CategoryEntity>
    ): List<HabitUiModel> {
        // Day-precise inclusion: a habit is active on `dateMs` if its calendar
        // start day is on or before `dateMs` AND its end day (if any) is on or
        // after `dateMs`. Normalizing both sides via [DateUtils.startOfDay]
        // fixes the prior bug where a habit created today at 10:30 AM (so
        // start_date = today_10:30) was excluded from today (dateMs = today_00:00)
        // because raw `10:30 <= 00:00` is false. After normalization both
        // sides land on today_00:00 and the habit shows up on its first day.
        val activeOnDate = habits.filter { habit ->
            val habitStart = DateUtils.startOfDay(habit.start_date)
            val habitEnd = habit.end_date?.let { DateUtils.startOfDay(it) }
            habitStart <= dateMs && (habitEnd == null || habitEnd >= dateMs)
        }
        val completedOnDate = habitLogRepository.getCompletedHabitIdsByDate(dateMs).toSet()

        return activeOnDate.map { habit ->
            val durationDays = if (habit.end_date != null) {
                (((habit.end_date - habit.start_date) / DAY_MS) + 1).toInt().coerceAtLeast(1)
            } else {
                Int.MAX_VALUE
            }
            val doneCount = habitLogRepository.countCompleted(habit.id)
            val isJourneyComplete = doneCount >= durationDays

            val category = habit.category_id?.let { categoryMap[it] }
            val checkedInToday = habit.id in completedOnDate

            val statusLabel = when {
                isJourneyComplete -> "Đã hoàn thành"
                checkedInToday -> "Đã check-in hôm nay"
                else -> "Đang thực hiện"
            }

            HabitUiModel(
                id = habit.id,
                category = category?.name ?: "Không phân loại",
                title = habit.title,
                time = habit.reminder_time ?: "",
                statusLabel = statusLabel,
                isCheckedInToday = checkedInToday,
                isJourneyComplete = isJourneyComplete,
                icon = category?.icon ?: "📌"
            )
        }
    }

    /**
     * Tab semantics are anchored to the *selected day's* check-in status, not the
     * habit's overall journey:
     *
     * - IN_PROGRESS ("Đang thực hiện"): the user still has something to do on this
     *   day — habit hasn't been checked in for this date yet.
     * - DONE ("Đã hoàn thành"): the user already completed this habit on this date.
     *
     * The instant a check-in lands (HabitLog row inserted → observeAllLogs emits →
     * combine() in bootstrap() re-runs buildUiHabits → isCheckedInToday flips to
     * true), the habit moves from IN_PROGRESS to DONE without any manual refresh.
     * That moves the user's mental model from "is the journey done?" to "did I do
     * it today?" — which matches how people actually use a daily-tasks screen.
     *
     * Habits whose overall journey is fully complete have their end_date in the
     * past, so the active-on-date filter in buildUiHabits already excludes them
     * from future selected dates. They only appear under DONE on the actual day
     * their check-in landed.
     */
    private fun applyFilter(filter: DailyHabitFilter, habits: List<HabitUiModel>): List<HabitUiModel> {
        return when (filter) {
            DailyHabitFilter.ALL -> habits
            DailyHabitFilter.IN_PROGRESS -> habits.filter { !it.isCheckedInToday }
            DailyHabitFilter.DONE -> habits.filter { it.isCheckedInToday }
        }
    }

    private fun startOfDay(calendar: Calendar): Long = calendar.apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis
}
