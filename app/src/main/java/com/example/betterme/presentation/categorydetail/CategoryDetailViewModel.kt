package com.example.betterme.presentation.categorydetail

import androidx.lifecycle.viewModelScope
import com.example.betterme.base.BaseMviViewModel
import com.example.betterme.data.local.datastore.DataStoreManager
import com.example.betterme.domain.repository.HabitLogRepository
import com.example.betterme.domain.repository.HabitRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

class CategoryDetailViewModel(
    private val dataStoreManager: DataStoreManager,
    private val habitRepository: HabitRepository,
    private val habitLogRepository: HabitLogRepository,
) : BaseMviViewModel<CategoryDetailIntent, CategoryDetailState, CategoryDetailEvent>() {

    override fun initState(): CategoryDetailState = CategoryDetailState()

    override fun processIntent(intent: CategoryDetailIntent) {
        when (intent) {
            is CategoryDetailIntent.LoadData -> loadData(
                intent.categoryId,
                intent.categoryName,
                intent.categoryIcon
            )
        }
    }

    // ============================================================
    // LOAD — Lấy habits của user trong category này
    // Dùng reactive Flow để tự cập nhật khi có thay đổi
    // ============================================================
    private fun loadData(categoryId: Int, categoryName: String, categoryIcon: String) {
        viewModelScope.launch {
            updateState {
                copy(
                    isLoading = true,
                    categoryId = categoryId,
                    categoryName = categoryName,
                    categoryIcon = categoryIcon
                )
            }

            val userId = dataStoreManager.getCurrentUserId().first() ?: ""

            // Lấy tất cả habits của user rồi lọc theo categoryId
            habitRepository.getHabits(userId).collect { allHabits ->
                val categoryHabits = allHabits.filter { it.category_id == categoryId }

                val habitUiModels = categoryHabits.map { habit ->
                    val totalDays = calculateTotalDays(habit.start_date, habit.end_date)

                    // Lấy số ngày đã hoàn thành từ HabitLog
                    val completedDays = try {
                        habitLogRepository.countCompleted(habit.id)
                    } catch (_: Exception) {
                        0
                    }

                    val completionPercent = if (totalDays > 0)
                        ((completedDays.toFloat() / totalDays) * 100).toInt().coerceIn(0, 100)
                    else 0

                    HabitDetailUiModel(
                        id = habit.id,
                        title = habit.title,
                        description = habit.description,
                        reminderTime = habit.reminder_time,
                        repeatPattern = null,
                        startDateMillis = habit.start_date,
                        endDateMillis = habit.end_date,
                        completedDays = completedDays,
                        totalDays = totalDays,
                        completionPercent = completionPercent
                    )
                }

                updateState {
                    copy(
                        isLoading = false,
                        habits = habitUiModels
                    )
                }
            }
        }
    }

    private fun calculateTotalDays(startDateMillis: Long, endDateMillis: Long?): Int {
        val dayMillis = TimeUnit.DAYS.toMillis(1)
        val effectiveEnd = endDateMillis ?: System.currentTimeMillis()
        val diff = (effectiveEnd - startDateMillis).coerceAtLeast(0L)
        // +1 để tính luôn ngày bắt đầu
        return (diff / dayMillis).toInt() + 1
    }

    // ============================================================
    // ACTIONS — Điều hướng
    // ============================================================
    fun onAiChatClick() {
        viewModelScope.launch {
            sendEvent(CategoryDetailEvent.NavigateToAiChat)
        }
    }

    fun onAddHabitClick() {
        viewModelScope.launch {
            sendEvent(CategoryDetailEvent.NavigateToAddHabit)
        }
    }
}
