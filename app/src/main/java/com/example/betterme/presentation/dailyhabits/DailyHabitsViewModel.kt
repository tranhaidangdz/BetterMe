package com.example.betterme.presentation.dailyhabits

import com.example.betterme.base.BaseMviViewModel
import com.example.betterme.presentation.dailyhabits.model.Habit
import com.example.betterme.presentation.dailyhabits.model.HabitUiModel
import java.util.concurrent.TimeUnit

class DailyHabitsViewModel : BaseMviViewModel<DailyHabitsIntent, DailyHabitsState, DailyHabitsEvent>() {

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

    private fun loadData() {
        val dates = listOf(
            DateUiModel("Jan", "20", "Mon"),
            DateUiModel("Jan", "21", "Tue"),
            DateUiModel("Jan", "22", "Wed"),
            DateUiModel("Jan", "23", "Thu"),
            DateUiModel("Jan", "24", "Fri")
        )

        val habits = fakeHabits().map(::toUiModel)

        updateState {
            copy(
                dates = dates,
                selectedDateIndex = 2,
                selectedFilter = DailyHabitFilter.ALL,
                allHabits = habits,
                visibleHabits = habits
            )
        }
    }

    private fun selectDate(index: Int) {
        if (index !in currentState.dates.indices) return
        updateState {
            copy(selectedDateIndex = index)
        }
    }

    private fun selectFilter(filter: DailyHabitFilter) {
        val filtered = when (filter) {
            DailyHabitFilter.ALL -> currentState.allHabits
            DailyHabitFilter.IN_PROGRESS -> currentState.allHabits.filter { !it.isCompleted }
            DailyHabitFilter.DONE -> currentState.allHabits.filter { it.isCompleted }
        }

        updateState {
            copy(
                selectedFilter = filter,
                visibleHabits = filtered
            )
        }
    }

    private fun toUiModel(habit: Habit): HabitUiModel {
        val daysRemaining = habit.endDateMillis?.let { end ->
            TimeUnit.MILLISECONDS.toDays(end - System.currentTimeMillis()).toInt().coerceAtLeast(0)
        }
        return HabitUiModel(
            id = habit.id,
            category = habit.category,
            title = habit.title,
            time = habit.time,
            statusLabel = if (habit.isCompleted) "Đã hoàn thành" else "Đang thực hiện",
            isCompleted = habit.isCompleted,
            icon = habit.icon,
            startDateMillis = habit.startDateMillis,
            endDateMillis = habit.endDateMillis,
            daysRemaining = daysRemaining
        )
    }

    private fun fakeHabits(): List<Habit> {
        val now = System.currentTimeMillis()
        val day = TimeUnit.DAYS.toMillis(1)
        return listOf(
            Habit(1, "Vận động & thể chất", "Đi bộ 10000 bước mỗi ngày", "06:40 AM", now, now + day * 21, false, "🏃"),
            Habit(2, "Vận động & thể chất", "Tập Gym 30 phút mỗi ngày", "09:40 AM", now, now + day * 10, false, "🏃"),
            Habit(3, "Dinh dưỡng & ăn uống lành mạnh", "Ăn 500g rau mỗi ngày", "11:00 AM", now, now + day * 14, false, "🥗"),
            Habit(4, "Tinh thần & sức khỏe tâm lý", "Đọc một cuốn sách mỗi ngày", "15:00 PM", now, now + day * 7, true, "🧠")
        )
    }
}
