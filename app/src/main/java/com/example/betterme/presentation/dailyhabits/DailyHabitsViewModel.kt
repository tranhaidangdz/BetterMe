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
import kotlinx.coroutines.flow.MutableStateFlow
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

    override fun initState(): DailyHabitsState = DailyHabitsState()

    init {
        processIntent(DailyHabitsIntent.LoadData)
    }

    override fun processIntent(intent: DailyHabitsIntent) {
        when (intent) {
            DailyHabitsIntent.LoadData -> bootstrap()
            is DailyHabitsIntent.SelectDate -> selectDate(intent.index)
            is DailyHabitsIntent.SelectFilter -> selectFilter(intent.filter)
            DailyHabitsIntent.RefreshIfDayChanged -> refreshIfDayChanged()
        }
    }

    /**
     * One-time setup of the date strip + the long-lived combine that keeps the
     * visible list in sync with Room. The cross-midnight case is handled
     * event-style via [refreshIfDayChanged] — the screen fires that intent on
     * every (re-)entry, so a user returning to Tasks after a day rollover
     * triggers a date-strip rebuild without any background ticker.
     */
    private fun bootstrap() {
        viewModelScope.launch {
            updateState { copy(isLoading = true) }
            buildDateStrip(snapToToday = true)

            val userId = dataStoreManager.getCurrentUserId().first().orEmpty()
            if (userId.isBlank()) {
                updateState { copy(isLoading = false) }
                return@launch
            }

            val categoryMap = runCatching {
                categoryRepository.getAll().first().associateBy { it.id }
            }.getOrDefault(emptyMap())

            // Plain collect (not collectLatest): buildUiHabits is ~5–20 ms and
            // user-driven emissions (add habit, check-in, date pick) don't burst
            // faster than that. Sequential processing keeps state updates strictly
            // ordered with the emissions that triggered them, which is the more
            // predictable choice for a lightweight pipeline.
            combine(
                habitRepository.getHabits(userId),
                selectedDateMillis,
                habitLogRepository.observeAllLogs()
            ) { habits, dateMs, _ ->
                buildUiHabits(habits, dateMs, categoryMap)
            }.collect { uiHabits ->
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
     * Cheap drift check. If the day in state still matches the current local
     * calendar day, this is a no-op. Otherwise the strip is rebuilt; the user's
     * previously-selected absolute date is preserved when it still fits in the
     * new 7-day window, else we snap to the new today.
     */
    private fun refreshIfDayChanged() {
        val currentTodayMs = startOfDay(Calendar.getInstance())
        val cachedTodayMs = currentState.dates.getOrNull(currentState.todayIndex)?.dateMillis
        if (cachedTodayMs == currentTodayMs) return
        buildDateStrip(snapToToday = false)
    }

    /**
     * Generates the 7-day strip centered on today and commits it to state.
     * [snapToToday] = true on a fresh bootstrap (user lands on today). On a
     * day-rollover refresh, we keep the user's absolute selection if it's still
     * inside the new window.
     */
    private fun buildDateStrip(snapToToday: Boolean) {
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
            snapToToday -> todayIndex
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
