package com.example.betterme.presentation.addhabit

import androidx.lifecycle.viewModelScope
import com.example.betterme.base.BaseMviViewModel
import com.example.betterme.data.local.datastore.DataStoreManager
import com.example.betterme.data.local.room.entities.HabitEntity
import com.example.betterme.domain.repository.CategoryRepository
import com.example.betterme.domain.repository.HabitRepository
import com.example.betterme.domain.repository.UserCategoryRepository
import com.example.betterme.domain.usecase.habit.ScheduleHabitReminderUseCase
import com.example.betterme.utils.DateUtils
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class AddHabitViewModel(
    private val dataStoreManager: DataStoreManager,
    private val habitRepository: HabitRepository,
    private val categoryRepository: CategoryRepository,
    private val userCategoryRepository: UserCategoryRepository,
    private val scheduleHabitReminder: ScheduleHabitReminderUseCase,
) : BaseMviViewModel<AddHabitIntent, AddHabitState, AddHabitEvent>() {

    override fun initState(): AddHabitState = AddHabitState()

    init {
        processIntent(AddHabitIntent.LoadCategories)
    }

    override fun processIntent(intent: AddHabitIntent) {
        when (intent) {
            AddHabitIntent.LoadCategories -> loadCategories()
            is AddHabitIntent.InputTitle -> updateState {
                copy(title = intent.title, titleError = null)
            }
            is AddHabitIntent.InputDescription -> updateState {
                copy(description = intent.description)
            }
            is AddHabitIntent.SelectCategory -> updateState {
                copy(
                    selectedCategoryId = intent.categoryId,
                    selectedCategoryName = intent.categoryName,
                    showCategorySelector = false
                )
            }
            is AddHabitIntent.InputReminderTime -> updateState {
                copy(reminderTime = intent.time)
            }
            is AddHabitIntent.InputStartDate -> updateState {
                copy(startDate = intent.dateMillis)
            }
            is AddHabitIntent.InputEndDate -> updateState {
                copy(endDate = intent.dateMillis)
            }
            AddHabitIntent.ToggleCategorySelector -> updateState {
                copy(showCategorySelector = !showCategorySelector)
            }
            AddHabitIntent.Submit -> submitHabit()
            is AddHabitIntent.ApplyAiSuggestions -> applyAiSuggestions(intent.suggestions)
        }
    }

    /**
     * Patch form state from a list of AI suggestions. Multiple suggestions
     * are merged in order — later ones can override earlier picks for the
     * same field. Only the three fields that have backing HabitEntity
     * columns mutate (title, reminderTime, categoryId); the rest are
     * silently ignored so we never lie to the user about what an apply
     * actually did.
     *
     * Category is matched by name against the already-loaded categories
     * list — exact match first, then case-insensitive fallback. Unknown
     * category names are dropped silently rather than crashing the form.
     *
     * Emits [AddHabitEvent.AppliedSuggestions] with a human-readable
     * summary so the screen can flash a snackbar listing the mutations.
     */
    private fun applyAiSuggestions(suggestions: List<com.example.betterme.domain.ai.habitcreation.HabitCreationSuggestion>) {
        if (suggestions.isEmpty()) return
        val applicable = suggestions.filter { it.hasApplicableMutation }
        if (applicable.isEmpty()) return

        val state = currentState
        var newTitle = state.title
        var newReminder = state.reminderTime
        var newCategoryId = state.selectedCategoryId
        var newCategoryName = state.selectedCategoryName
        val changes = mutableListOf<String>()

        applicable.forEach { s ->
            s.suggestedTitle?.takeIf { it.isNotBlank() && it != newTitle }?.let {
                newTitle = it
                changes += "tên"
            }
            s.suggestedReminderTime?.takeIf { it.isNotBlank() && it != newReminder }?.let {
                newReminder = it
                changes += "giờ nhắc → $it"
            }
            s.suggestedCategory?.takeIf { it.isNotBlank() }?.let { suggestedName ->
                val match = state.categories.firstOrNull { it.name == suggestedName }
                    ?: state.categories.firstOrNull { it.name.equals(suggestedName, ignoreCase = true) }
                if (match != null && match.id != newCategoryId) {
                    newCategoryId = match.id
                    newCategoryName = match.name
                    changes += "nhóm → ${match.name}"
                }
            }
        }

        if (changes.isEmpty()) return

        updateState {
            copy(
                title = newTitle,
                titleError = null,
                reminderTime = newReminder,
                selectedCategoryId = newCategoryId,
                selectedCategoryName = newCategoryName
            )
        }
        sendEvent(AddHabitEvent.AppliedSuggestions("Đã áp dụng: " + changes.joinToString(", ")))
    }

    // ============================================================
    // LOAD — Lấy danh sách categories đã chọn
    // ============================================================
    private fun loadCategories() {
        viewModelScope.launch {
            try {
                val userId = dataStoreManager.getCurrentUserId().first().orEmpty()
                val categories = if (userId.isNotBlank()) {
                    userCategoryRepository.getSelectedCategories(userId)
                } else emptyList()
                // Fall back to the full catalog if the user has no selections yet so they can
                // still pick a category when adding a habit before completing onboarding.
                val resolved = categories.ifEmpty {
                    categoryRepository.getAll().first()
                }
                updateState { copy(categories = resolved) }
            } catch (e: Exception) {
                sendEvent(AddHabitEvent.ShowError("Không thể tải danh mục: ${e.message}"))
            }
        }
    }

    // ============================================================
    // SUBMIT — Validate + lưu habit vào DB
    // ============================================================
    private fun submitHabit() {
        val state = currentState

        // Re-entrancy guard. The submit button isn't disabled at the UI layer
        // during the in-flight insert, so a fast double-tap would queue a second
        // viewModelScope coroutine and write the habit twice (and arm two alarms).
        // isLoading is flipped to true inside the coroutine below, so checking it
        // here costs nothing on the cold path and short-circuits the duplicate
        // path before any IO work happens.
        if (state.isLoading) return

        // Validation
        if (state.title.isBlank()) {
            updateState { copy(titleError = "Vui lòng nhập tên thói quen") }
            return
        }

        viewModelScope.launch {
            updateState { copy(isLoading = true) }

            try {
                val userId = dataStoreManager.getCurrentUserId().first() ?: ""

                // Normalize start_date and end_date to start-of-day at write time so
                // every read site sees a clean day-aligned value. Without this, the
                // default state.startDate = System.currentTimeMillis() (e.g. today
                // 10:30 AM) would land mid-day in the DB, and comparing it against
                // dateMs (= today 00:00) using `start_date <= dateMs` would be
                // FALSE — silently excluding the habit from its own first day.
                // Doing the floor here makes the rest of the app idempotent on
                // time-of-day; the read-side normalization in DailyHabitsViewModel
                // and StatisticsViewModel stays as defense in depth.
                val habitEntity = HabitEntity(
                    user_id = userId,
                    category_id = state.selectedCategoryId,
                    title = state.title.trim(),
                    description = state.description.trim().ifBlank { null },
                    start_date = DateUtils.startOfDay(state.startDate),
                    end_date = state.endDate?.let { DateUtils.startOfDay(it) },
                    reminder_time = state.reminderTime.ifBlank { null },
                    created_at = System.currentTimeMillis()
                )

                val insertedHabitId = habitRepository.addHabit(habitEntity).toInt()

                // Arm the per-habit AlarmManager exact alarm immediately. If the user
                // didn't set a reminder time, the use case is a no-op.
                scheduleHabitReminder(
                    habitId = insertedHabitId,
                    habitTitle = habitEntity.title,
                    reminderTime = habitEntity.reminder_time
                )

                updateState { copy(isLoading = false) }
                sendEvent(AddHabitEvent.SaveSuccess)

                // Reset form
                updateState {
                    copy(
                        title = "",
                        description = "",
                        selectedCategoryId = null,
                        selectedCategoryName = "",
                        reminderTime = "",
                        startDate = System.currentTimeMillis(),
                        endDate = null,
                        titleError = null
                    )
                }
            } catch (e: Exception) {
                updateState { copy(isLoading = false) }
                sendEvent(AddHabitEvent.ShowError("Lỗi: ${e.message}"))
            }
        }
    }
}
