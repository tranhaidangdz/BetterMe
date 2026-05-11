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
/** Lifecycle of the AI review request shown inside the Category Detail screen. */
sealed class AiReviewState {
    data object Idle : AiReviewState()
    data object Loading : AiReviewState()
    data class Success(val text: String) : AiReviewState()
    data class Error(val message: String) : AiReviewState()
}

data class CategoryDetailState(
    val isLoading: Boolean = false,
    val categoryId: Int = -1,
    val categoryName: String = "",
    val categoryIcon: String = "",
    val habits: List<HabitDetailUiModel> = emptyList(),
    /** AI coaching review — Idle until the user taps "AI nhận xét". */
    val aiReview: AiReviewState = AiReviewState.Idle,
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
    /** Generate an AI coaching review for the loaded category. */
    data object GenerateAiReview : CategoryDetailIntent()
    /** Clear the AI review back to Idle (e.g. after the user dismisses it). */
    data object DismissAiReview : CategoryDetailIntent()
}

// ============================================================
// EVENT
// ============================================================
sealed class CategoryDetailEvent : MviSingleEvent {
    data object NavigateToAiChat : CategoryDetailEvent()
    data object NavigateToAddHabit : CategoryDetailEvent()
}
