package com.example.betterme.presentation.categorydetail

import com.example.betterme.base.MviIntent
import com.example.betterme.base.MviSingleEvent
import com.example.betterme.base.MviViewState

// ============================================================
// UI MODEL — dữ liệu từng thói quen hiển thị trong detail
// ============================================================
data class HabitDetailUiModel(
    val id: Int,
    val title: String,
    val description: String?,
    val reminderTime: String?,
    val repeatPattern: String?,
    val startDateMillis: Long,
    val endDateMillis: Long?,
    // Completion từ HabitLog (TODO: connect khi có HabitLog tracking)
    val completedDays: Int = 0,
    val totalDays: Int = 0,
    val completionPercent: Int = 0
)

// ============================================================
// STATE
// ============================================================
data class CategoryDetailState(
    val isLoading: Boolean = false,
    val categoryId: Int = -1,
    val categoryName: String = "",
    val categoryIcon: String = "",
    val habits: List<HabitDetailUiModel> = emptyList(),
) : MviViewState {
    /** Tổng số thói quen đã hoàn thành (completionPercent = 100) */
    val completedHabitCount: Int
        get() = habits.count { it.completionPercent == 100 }

    /** % hoàn thành toàn nhóm = tổng completedDays / tổng totalDays */
    val groupCompletionPercent: Int
        get() {
            val totalCompletedDays = habits.sumOf { it.completedDays }
            val totalDays = habits.sumOf { it.totalDays }
            if (totalDays <= 0) return 0
            return ((totalCompletedDays.toFloat() / totalDays) * 100).toInt().coerceIn(0, 100)
        }
}

// ============================================================
// INTENT
// ============================================================
sealed class CategoryDetailIntent : MviIntent {
    data class LoadData(
        val categoryId: Int,
        val categoryName: String,
        val categoryIcon: String
    ) : CategoryDetailIntent()
}

// ============================================================
// EVENT
// ============================================================
sealed class CategoryDetailEvent : MviSingleEvent {
    data object NavigateToAiChat : CategoryDetailEvent()
    data object NavigateToAddHabit : CategoryDetailEvent()
}
