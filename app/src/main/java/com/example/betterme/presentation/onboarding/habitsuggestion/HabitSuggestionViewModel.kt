package com.example.betterme.presentation.onboarding.habitsuggestion

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.betterme.data.local.datastore.DataStoreManager
import com.example.betterme.data.local.fake.getPersonalizedHabitGroups
import com.example.betterme.data.local.room.entities.HabitEntity
import com.example.betterme.data.local.room.entities.ReminderEntity
import com.example.betterme.domain.repository.CategoryRepository
import com.example.betterme.domain.repository.HabitRepository
import com.example.betterme.domain.repository.ReminderRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class HabitSuggestionViewModel(
    private val selectedCategoryIds: List<Int>,
    private val dataStoreManager: DataStoreManager,
    private val categoryRepository: CategoryRepository,
    private val habitRepository: HabitRepository,
    private val reminderRepository: ReminderRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(HabitSuggestionState())
    val state: StateFlow<HabitSuggestionState> = _state.asStateFlow()

    private val _event = MutableSharedFlow<HabitSuggestionEvent>()
    val event = _event.asSharedFlow()

    val repeatOptions = listOf("Hàng ngày", "Các ngày trong tuần", "Cuối tuần", "Tùy chỉnh")

    init {
        loadSuggestedHabits()
    }

    fun onIntent(intent: HabitSuggestionIntent) {
        when (intent) {
            is HabitSuggestionIntent.ToggleHabit -> toggleHabit(intent.id)
            is HabitSuggestionIntent.SetReminderTime -> setReminderTime(intent.habitId, intent.hour, intent.minute)
            is HabitSuggestionIntent.SetRepeat -> setRepeat(intent.habitId, intent.label)
            is HabitSuggestionIntent.ShowReminderPicker -> showReminderPicker(intent.habitId)
            HabitSuggestionIntent.DismissReminderPicker -> dismissReminderPicker()
            is HabitSuggestionIntent.ShowRepeatPicker -> showRepeatPicker(intent.habitId)
            HabitSuggestionIntent.DismissRepeatPicker -> dismissRepeatPicker()
            is HabitSuggestionIntent.ShowStartDatePicker -> showStartDatePicker(intent.habitId)
            is HabitSuggestionIntent.ShowEndDatePicker -> showEndDatePicker(intent.habitId)
            is HabitSuggestionIntent.SetStartDate -> setStartDate(intent.habitId, intent.millis)
            is HabitSuggestionIntent.SetEndDate -> setEndDate(intent.habitId, intent.millis)
            HabitSuggestionIntent.DismissDatePicker -> dismissDatePicker()
            is HabitSuggestionIntent.ConfirmHabitSettings -> confirmHabitSettings(intent.habitId)
            is HabitSuggestionIntent.DismissHabitSettings -> dismissHabitSettings(intent.habitId)
            HabitSuggestionIntent.StartJourney -> startJourney()
        }
    }

    // ============================================================
    // LOAD — Personalized habits dựa vào userId
    // ============================================================
    private fun loadSuggestedHabits() {
        viewModelScope.launch {
            // Lấy userId để làm seed cho personalization
            val userId = dataStoreManager.getCurrentUserId().first() ?: "default_user"

            val selectedCategories = categoryRepository.getAll().first()
                .filter { it.id in selectedCategoryIds }
            val selectedCategoryNameToId = selectedCategories.associate { it.name to it.id }

            // Lấy danh sách thói quen đã được personalize theo userId
            val personalizedGroups = getPersonalizedHabitGroups(userId)

            val filteredGroups = personalizedGroups.filter { group ->
                selectedCategoryNameToId.containsKey(group.categoryName)
            }

            var globalId = 1
            val categoryWithHabits = filteredGroups.mapNotNull { group ->
                val realCategoryId = selectedCategoryNameToId[group.categoryName] ?: return@mapNotNull null
                CategoryWithHabits(
                    categoryName = group.categoryName,
                    categoryIcon = group.categoryIcon,
                    categoryId = realCategoryId,
                    habits = group.habits.map { title ->
                        SuggestedHabitUiModel(
                            id = globalId++,
                            title = title,
                            isChecked = false // Mặc định KHÔNG tích
                        )
                    }
                )
            }

            _state.update { it.copy(categoryHabits = categoryWithHabits) }
        }
    }

    // ============================================================
    // TOGGLE
    // ============================================================
    private fun toggleHabit(id: Int) {
        val habit = _state.value.categoryHabits.flatMap { it.habits }.find { it.id == id } ?: return

        if (!habit.isChecked) {
            // Chưa tích → tích + hiện dialog chỉnh sửa reminder/repeat/date
            _state.update { current ->
                current.copy(
                    categoryHabits = current.updateHabit(id) { it.copy(isChecked = true) },
                    editingHabitId = id
                )
            }
        } else {
            // Đã tích → bỏ tích
            _state.update { current ->
                current.copy(
                    categoryHabits = current.updateHabit(id) { it.copy(isChecked = false) },
                    editingHabitId = null
                )
            }
        }
    }

    // ============================================================
    // REMINDER
    // ============================================================
    private fun setReminderTime(habitId: Int, hour: Int, minute: Int) {
        _state.update { current ->
            current.copy(
                categoryHabits = current.updateHabit(habitId) {
                    it.copy(reminderHour = hour, reminderMinute = minute)
                },
                showReminderPicker = false
            )
        }
    }

    private fun showReminderPicker(habitId: Int) {
        _state.update { it.copy(showReminderPicker = true, editingHabitId = habitId) }
    }

    private fun dismissReminderPicker() {
        _state.update { it.copy(showReminderPicker = false) }
    }

    // ============================================================
    // REPEAT
    // ============================================================
    private fun setRepeat(habitId: Int, label: String) {
        _state.update { current ->
            current.copy(
                categoryHabits = current.updateHabit(habitId) { it.copy(repeatLabel = label) },
                showRepeatPicker = false
            )
        }
    }

    private fun showRepeatPicker(habitId: Int) {
        _state.update { it.copy(showRepeatPicker = true, editingHabitId = habitId) }
    }

    private fun dismissRepeatPicker() {
        _state.update { it.copy(showRepeatPicker = false) }
    }

    // ============================================================
    // DATE PICKER
    // ============================================================
    private fun showStartDatePicker(habitId: Int) {
        _state.update { it.copy(showStartDatePicker = true, editingHabitId = habitId) }
    }

    private fun showEndDatePicker(habitId: Int) {
        _state.update { it.copy(showEndDatePicker = true, editingHabitId = habitId) }
    }

    private fun setStartDate(habitId: Int, millis: Long) {
        _state.update { current ->
            current.copy(
                categoryHabits = current.updateHabit(habitId) { it.copy(startDateMillis = millis) },
                showStartDatePicker = false
            )
        }
    }

    private fun setEndDate(habitId: Int, millis: Long) {
        _state.update { current ->
            current.copy(
                categoryHabits = current.updateHabit(habitId) { it.copy(endDateMillis = millis) },
                showEndDatePicker = false
            )
        }
    }

    private fun dismissDatePicker() {
        _state.update { it.copy(showStartDatePicker = false, showEndDatePicker = false) }
    }

    // ============================================================
    // CONFIRM / DISMISS DIALOG
    // ============================================================
    private fun confirmHabitSettings(habitId: Int) {
        _state.update { it.copy(editingHabitId = null) }
    }

    private fun dismissHabitSettings(habitId: Int) {
        // Bỏ tích habit nếu user huỷ dialog settings
        _state.update { current ->
            current.copy(
                categoryHabits = current.updateHabit(habitId) { it.copy(isChecked = false) },
                editingHabitId = null
            )
        }
    }

    // ============================================================
    // START JOURNEY — Lưu habit thực vào DB
    // ============================================================
    private fun startJourney() {
        val checkedCount = _state.value.selectedHabitCount
        if (checkedCount == 0) {
            viewModelScope.launch {
                _event.emit(HabitSuggestionEvent.ShowError("Vui lòng chọn ít nhất 1 thói quen"))
            }
            return
        }

        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            try {
                val userId = dataStoreManager.getCurrentUserId().first()
                if (userId == null) {
                    _event.emit(HabitSuggestionEvent.ShowError("Không tìm thấy thông tin người dùng"))
                    return@launch
                }

                val now = System.currentTimeMillis()

                _state.value.categoryHabits.forEach { category ->
                    category.habits
                        .filter { it.isChecked }
                        .forEach { habit ->
                            // Lưu habit — dùng startDate/endDate người dùng đã chọn
                            val habitEntity = HabitEntity(
                                user_id = userId,
                                category_id = category.categoryId,
                                title = habit.title,
                                description = null,
                                start_date = habit.startDateMillis,
                                end_date = habit.endDateMillis,
                                reminder_time = habit.reminderTimeFormatted,
                                reminder_repeat = habit.repeatLabel,
                                created_at = now
                            )
                            val habitId = habitRepository.addHabit(habitEntity)

                            // Lưu reminder
                            val reminderEntity = ReminderEntity(
                                habit_id = habitId.toInt(),
                                time = habit.reminderTimeFormatted,
                                repeat_pattern = habit.repeatLabel,
                                is_active = true
                            )
                            reminderRepository.addReminder(reminderEntity)
                        }
                }

                dataStoreManager.setHasSelectedHabits()
                _event.emit(HabitSuggestionEvent.NavigateToMain)
            } catch (e: Exception) {
                e.printStackTrace()
                _event.emit(HabitSuggestionEvent.ShowError("Có lỗi xảy ra, vui lòng thử lại"))
            } finally {
                _state.update { it.copy(isLoading = false) }
            }
        }
    }

    // ============================================================
    // HELPER — Cập nhật habit trong nested list
    // ============================================================
    private fun HabitSuggestionState.updateHabit(
        habitId: Int,
        transform: (SuggestedHabitUiModel) -> SuggestedHabitUiModel
    ): List<CategoryWithHabits> {
        return categoryHabits.map { category ->
            category.copy(
                habits = category.habits.map { h ->
                    if (h.id == habitId) transform(h) else h
                }
            )
        }
    }
}
