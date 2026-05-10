package com.example.betterme.presentation.dailyhabits

import androidx.lifecycle.viewModelScope
import com.example.betterme.base.BaseMviViewModel
import com.example.betterme.data.local.datastore.DataStoreManager
import com.example.betterme.data.local.room.entities.HabitEntity
import com.example.betterme.domain.repository.CategoryRepository
import com.example.betterme.domain.repository.HabitLogRepository
import com.example.betterme.domain.repository.HabitRepository
import com.example.betterme.presentation.dailyhabits.model.HabitUiModel
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
        }
    }

    /**
     * One-time setup of the date strip + the long-lived combine that keeps the visible
     * list in sync with Room. Subsequent re-emissions don't re-run this — they flow
     * through the [combine] below.
     */
    private fun bootstrap() {
        viewModelScope.launch {
            updateState { copy(isLoading = true) }
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
            updateState {
                copy(
                    dates = dates,
                    todayIndex = todayIndex,
                    selectedDateIndex = todayIndex
                )
            }
            selectedDateMillis.value = dates[todayIndex].dateMillis

            val userId = dataStoreManager.getCurrentUserId().first().orEmpty()
            if (userId.isBlank()) {
                updateState { copy(isLoading = false) }
                return@launch
            }

            val categoryMap = runCatching {
                categoryRepository.getAll().first().associateBy { it.id }
            }.getOrDefault(emptyMap())

            // The third flow is a change-signal Flow. We don't care about its payload;
            // we map to Unit so the combine is invalidated whenever it emits.
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
        val activeOnDate = habits.filter { habit ->
            habit.start_date <= dateMs &&
                (habit.end_date == null || habit.end_date >= dateMs)
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
     * "Đang thực hiện" filters out habits whose journey has finished — they belong in
     * "Đã hoàn thành". A habit cannot appear in both buckets at the same time.
     */
    private fun applyFilter(filter: DailyHabitFilter, habits: List<HabitUiModel>): List<HabitUiModel> {
        return when (filter) {
            DailyHabitFilter.ALL -> habits
            DailyHabitFilter.IN_PROGRESS -> habits.filter { !it.isJourneyComplete }
            DailyHabitFilter.DONE -> habits.filter { it.isJourneyComplete }
        }
    }

    private fun startOfDay(calendar: Calendar): Long = calendar.apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis
}
