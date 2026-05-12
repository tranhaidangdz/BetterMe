package com.example.betterme.presentation.categorydetail

import androidx.lifecycle.viewModelScope
import com.example.betterme.base.BaseMviViewModel
import com.example.betterme.data.local.datastore.DataStoreManager
import com.example.betterme.data.local.room.entities.HabitEntity
import com.example.betterme.domain.ai.AiCoachPersonality
import com.example.betterme.domain.ai.AiHabitInsightRepository
import com.example.betterme.domain.ai.SuggestedHabit
import com.example.betterme.domain.repository.HabitLogRepository
import com.example.betterme.domain.repository.HabitRepository
import com.example.betterme.domain.usecase.ai.GenerateHabitGroupReviewUseCase
import com.example.betterme.domain.usecase.ai.SuggestHabitsForCategoryUseCase
import com.example.betterme.domain.usecase.habit.ScheduleHabitReminderUseCase
import com.example.betterme.utils.DateUtils
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

class CategoryDetailViewModel(
    private val dataStoreManager: DataStoreManager,
    private val habitRepository: HabitRepository,
    private val habitLogRepository: HabitLogRepository,
    private val generateHabitGroupReview: GenerateHabitGroupReviewUseCase,
    private val suggestHabitsForCategory: SuggestHabitsForCategoryUseCase,
    private val scheduleHabitReminder: ScheduleHabitReminderUseCase,
) : BaseMviViewModel<CategoryDetailIntent, CategoryDetailState, CategoryDetailEvent>() {

    override fun initState(): CategoryDetailState = CategoryDetailState()

    override fun processIntent(intent: CategoryDetailIntent) {
        when (intent) {
            is CategoryDetailIntent.LoadData -> loadData(
                intent.categoryId,
                intent.categoryName,
                intent.categoryIcon
            )
            is CategoryDetailIntent.GenerateAiReview -> generateAiReview(intent.forceRefresh)
            CategoryDetailIntent.DismissAiReview -> updateState {
                copy(aiReview = AiReviewState.Idle)
            }
            is CategoryDetailIntent.GenerateAiSuggestions ->
                generateAiSuggestions(intent.forceRefresh)
            CategoryDetailIntent.DismissAiSuggestions -> updateState {
                copy(aiSuggestions = AiSuggestionsState.Idle)
            }
            is CategoryDetailIntent.AddAiSuggestion -> addAiSuggestion(intent.suggestion)
        }
    }

    /**
     * Fire-and-forget AI suggestion request. Re-entrancy guard prevents tap-storms
     * from spawning parallel coroutines; viewModelScope cancels in-flight calls
     * automatically when the screen is left (via DisposableEffect in the host).
     */
    private fun generateAiSuggestions(forceRefresh: Boolean) {
        if (currentState.aiSuggestions is AiSuggestionsState.Loading) return
        val categoryId = currentState.categoryId
        val categoryName = currentState.categoryName
        if (categoryId <= 0 || categoryName.isBlank()) return

        viewModelScope.launch {
            updateState { copy(aiSuggestions = AiSuggestionsState.Loading) }
            val result = suggestHabitsForCategory(
                categoryId = categoryId,
                categoryName = categoryName,
                personality = AiCoachPersonality.Default,
                forceRefresh = forceRefresh
            )
            updateState {
                copy(
                    aiSuggestions = when (result) {
                        is AiHabitInsightRepository.AiSuggestResult.Success ->
                            AiSuggestionsState.Success(result.suggestions)
                        is AiHabitInsightRepository.AiSuggestResult.Failure ->
                            AiSuggestionsState.Error(result.message)
                    }
                )
            }
        }
    }

    /**
     * Materializes an AI suggestion as a real habit row. Defaults: 30-day journey
     * starting today, description from the suggestion, no reminder time (the user
     * can tap into the habit to configure one). The new row appears automatically
     * via the reactive Flow that backs [loadData].
     *
     * Fires a single-shot [CategoryDetailEvent.HabitAddedFromSuggestion] so the host
     * screen can render a success toast/snackbar without reading state.
     */
    private fun addAiSuggestion(suggestion: SuggestedHabit) {
        viewModelScope.launch {
            val userId = dataStoreManager.getCurrentUserId().first().orEmpty()
            if (userId.isBlank()) return@launch

            val now = System.currentTimeMillis()
            val todayStart = DateUtils.startOfDay(now)
            val thirtyDays = TimeUnit.DAYS.toMillis(30)
            // Day-aligned start/end so the new habit shows in today's active list
            // immediately, with no time-of-day comparison off-by-hours bug.
            val entity = HabitEntity(
                user_id = userId,
                category_id = currentState.categoryId.takeIf { it > 0 },
                title = suggestion.title,
                description = suggestion.description.ifBlank { null },
                start_date = todayStart,
                end_date = todayStart + thirtyDays,
                reminder_time = null,
                created_at = now
            )
            val newId = habitRepository.addHabit(entity).toInt()
            scheduleHabitReminder(
                habitId = newId,
                habitTitle = suggestion.title,
                reminderTime = null
            )
            sendEvent(CategoryDetailEvent.HabitAddedFromSuggestion(suggestion.title))
        }
    }

    /**
     * Fire-and-forget AI review request. State transitions Idle → Loading → Success /
     * Error. Re-entrancy guard: if a request is already in flight, additional taps
     * are ignored so a tap-storm doesn't spawn parallel coroutines.
     */
    private fun generateAiReview(forceRefresh: Boolean) {
        if (currentState.aiReview is AiReviewState.Loading) return
        val categoryId = currentState.categoryId
        val categoryName = currentState.categoryName
        if (categoryId <= 0 || categoryName.isBlank()) return

        viewModelScope.launch {
            updateState { copy(aiReview = AiReviewState.Loading) }
            val result = generateHabitGroupReview(
                categoryId = categoryId,
                categoryName = categoryName,
                personality = AiCoachPersonality.Default,
                forceRefresh = forceRefresh
            )
            updateState {
                copy(
                    aiReview = when (result) {
                        is AiHabitInsightRepository.AiResult.Success ->
                            AiReviewState.Success(result.text)
                        is AiHabitInsightRepository.AiResult.Failure ->
                            AiReviewState.Error(result.message)
                    }
                )
            }
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
