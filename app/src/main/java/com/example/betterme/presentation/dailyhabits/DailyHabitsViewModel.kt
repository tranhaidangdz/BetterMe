package com.example.betterme.presentation.dailyhabits

import androidx.lifecycle.viewModelScope
import com.example.betterme.base.BaseMviViewModel
import com.example.betterme.data.local.datastore.DataStoreManager
import com.example.betterme.domain.repository.CategoryRepository
import com.example.betterme.domain.repository.HabitLogRepository
import com.example.betterme.domain.repository.HabitRepository
import com.example.betterme.presentation.dailyhabits.model.HabitUiModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class DailyHabitsViewModel(
    private val dataStoreManager: DataStoreManager,
    private val habitRepository: HabitRepository,
    private val habitLogRepository: HabitLogRepository,
    private val categoryRepository: CategoryRepository,
) : BaseMviViewModel<DailyHabitsIntent, DailyHabitsState, DailyHabitsEvent>() {

    override fun initState(): DailyHabitsState = DailyHabitsState()

    init {
        processIntent(DailyHabitsIntent.LoadData)
    }

    override fun processIntent(intent: DailyHabitsIntent) {
        when (intent) {
            DailyHabitsIntent.LoadData -> loadData()
            is DailyHabitsIntent.SelectDate -> selectDate(intent.index)
            is DailyHabitsIntent.SelectFilter -> selectFilter(intent.filter)
        }
    }

    // ============================================================
    // LOAD — Tạo danh sách 7 ngày (3 trước + hôm nay + 3 sau)
    //        + load habits thực từ Room
    // ============================================================
    private fun loadData() {
        viewModelScope.launch {
            updateState { copy(isLoading = true) }

            try {
                // 1. Tạo danh sách ngày thực tế (7 ngày)
                val today = Calendar.getInstance()
                val todayIndex = 3 // Hôm nay nằm ở vị trí thứ 4 (index 3)
                val dates = (-3..3).map { offset ->
                    val cal = Calendar.getInstance().apply {
                        add(Calendar.DAY_OF_YEAR, offset)
                    }
                    DateUiModel(
                        month = SimpleDateFormat("MMM", Locale("vi")).format(cal.time),
                        day = SimpleDateFormat("dd", Locale.getDefault()).format(cal.time),
                        weekDay = SimpleDateFormat("EEE", Locale("vi")).format(cal.time),
                        dateMillis = getStartOfDay(cal),
                        isToday = offset == 0
                    )
                }

                // 2. Load habits + check-in status cho ngày được chọn (mặc định = hôm nay)
                val selectedDate = dates[todayIndex]
                val habits = loadHabitsForDate(selectedDate.dateMillis)

                updateState {
                    copy(
                        isLoading = false,
                        dates = dates,
                        todayIndex = todayIndex,
                        selectedDateIndex = todayIndex,
                        selectedFilter = DailyHabitFilter.ALL,
                        allHabits = habits,
                        visibleHabits = habits
                    )
                }
            } catch (e: Exception) {
                updateState { copy(isLoading = false) }
            }
        }
    }

    // ============================================================
    // SELECT DATE — Load lại habits cho ngày mới
    // ============================================================
    private fun selectDate(index: Int) {
        if (index !in currentState.dates.indices) return
        viewModelScope.launch {
            updateState { copy(isLoading = true, selectedDateIndex = index) }

            try {
                val selectedDate = currentState.dates[index]
                val habits = loadHabitsForDate(selectedDate.dateMillis)

                updateState {
                    copy(
                        isLoading = false,
                        allHabits = habits,
                        visibleHabits = applyFilter(currentState.selectedFilter, habits)
                    )
                }
            } catch (e: Exception) {
                updateState { copy(isLoading = false) }
            }
        }
    }

    // ============================================================
    // FILTER — Lọc theo trạng thái
    // ============================================================
    private fun selectFilter(filter: DailyHabitFilter) {
        updateState {
            copy(
                selectedFilter = filter,
                visibleHabits = applyFilter(filter, allHabits)
            )
        }
    }

    // ============================================================
    // HELPER — Load habits kèm trạng thái check-in cho 1 ngày
    // ============================================================
    private suspend fun loadHabitsForDate(dateMillis: Long): List<HabitUiModel> {
        val userId = dataStoreManager.getCurrentUserId().first() ?: return emptyList()

        // Lấy tất cả habits của user (đang active trong ngày được chọn)
        val allHabits = habitRepository.getHabits(userId).first()
            .filter { habit ->
                // Chỉ hiện habit mà ngày được chọn nằm trong khoảng [start_date, end_date]
                habit.start_date <= dateMillis &&
                        (habit.end_date == null || habit.end_date >= dateMillis)
            }

        // Lấy danh sách habitId đã DONE trong ngày
        val completedIds = habitLogRepository.getCompletedHabitIdsByDate(dateMillis).toSet()

        // Lấy tất cả categories để map tên + icon
        val categories = try {
            categoryRepository.getAll().first().associateBy { it.id }
        } catch (_: Exception) {
            emptyMap()
        }

        return allHabits.map { habit ->
            val category = habit.category_id?.let { categories[it] }
            val isCompleted = habit.id in completedIds

            HabitUiModel(
                id = habit.id,
                category = category?.name ?: "Không phân loại",
                title = habit.title,
                time = habit.reminder_time ?: "",
                statusLabel = if (isCompleted) "Đã hoàn thành" else "Đang thực hiện",
                isCompleted = isCompleted,
                icon = category?.icon ?: "📌"
            )
        }
    }

    // ============================================================
    // HELPER — Áp dụng filter lên danh sách habits
    // ============================================================
    private fun applyFilter(filter: DailyHabitFilter, habits: List<HabitUiModel>): List<HabitUiModel> {
        return when (filter) {
            DailyHabitFilter.ALL -> habits
            DailyHabitFilter.IN_PROGRESS -> habits.filter { !it.isCompleted }
            DailyHabitFilter.DONE -> habits.filter { it.isCompleted }
        }
    }

    // ============================================================
    // HELPER — Lấy start of day (00:00:00) millis
    // ============================================================
    private fun getStartOfDay(calendar: Calendar): Long {
        return calendar.apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }
}
