package com.example.betterme.presentation.onboarding.habitsuggestion

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.betterme.data.local.datastore.DataStoreManager
import com.example.betterme.data.local.fake.fakeHabitGroups
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
            is HabitSuggestionIntent.ConfirmHabitSettings -> confirmHabitSettings(intent.habitId)
            is HabitSuggestionIntent.DismissHabitSettings -> dismissHabitSettings(intent.habitId)
            HabitSuggestionIntent.StartJourney -> startJourney()
        }
    }

    private fun loadSuggestedHabits() {
        viewModelScope.launch {
            val selectedCategories = categoryRepository.getAll().first()
                .filter { it.id in selectedCategoryIds }
            val selectedCategoryNameToId = selectedCategories.associate { it.name to it.id }

            val allGroups = fakeHabitGroups()
            val filteredGroups = allGroups.filter { group ->
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

    private fun toggleHabit(id: Int) {
        val habit = _state.value.categoryHabits.flatMap { it.habits }.find { it.id == id } ?: return

        if (!habit.isChecked) {
            // Chưa tích → tích + hiện dialog chỉnh sửa reminder/repeat
            _state.update { current ->
                current.copy(
                    categoryHabits = current.categoryHabits.map { category ->
                        category.copy(
                            habits = category.habits.map { h ->
                                if (h.id == id) h.copy(isChecked = true)
                                else h
                            }
                        )
                    },
                    editingHabitId = id
                )
            }
        } else {
            // Đã tích → bỏ tích
            _state.update { current ->
                current.copy(
                    categoryHabits = current.categoryHabits.map { category ->
                        category.copy(
                            habits = category.habits.map { h ->
                                if (h.id == id) h.copy(isChecked = false)
                                else h
                            }
                        )
                    },
                    editingHabitId = null
                )
            }
        }
    }

    private fun setReminderTime(habitId: Int, hour: Int, minute: Int) {
        _state.update { current ->
            current.copy(
                categoryHabits = current.categoryHabits.map { category ->
                    category.copy(
                        habits = category.habits.map { h ->
                            if (h.id == habitId) h.copy(reminderHour = hour, reminderMinute = minute)
                            else h
                        }
                    )
                },
                showReminderPicker = false
            )
        }
    }

    private fun setRepeat(habitId: Int, label: String) {
        _state.update { current ->
            current.copy(
                categoryHabits = current.categoryHabits.map { category ->
                    category.copy(
                        habits = category.habits.map { h ->
                            if (h.id == habitId) h.copy(repeatLabel = label)
                            else h
                        }
                    )
                },
                showRepeatPicker = false
            )
        }
    }

    private fun showReminderPicker(habitId: Int) {
        _state.update { it.copy(showReminderPicker = true, editingHabitId = habitId) }
    }

    private fun dismissReminderPicker() {
        _state.update { it.copy(showReminderPicker = false) }
    }

    private fun showRepeatPicker(habitId: Int) {
        _state.update { it.copy(showRepeatPicker = true, editingHabitId = habitId) }
    }

    private fun dismissRepeatPicker() {
        _state.update { it.copy(showRepeatPicker = false) }
    }

    private fun confirmHabitSettings(habitId: Int) {
        // Đóng dialog settings, giữ habit đã checked với settings hiện tại
        _state.update { it.copy(editingHabitId = null) }
    }

    private fun dismissHabitSettings(habitId: Int) {
        // Bỏ tích habit nếu user huỷ dialog settings
        _state.update { current ->
            current.copy(
                categoryHabits = current.categoryHabits.map { category ->
                    category.copy(
                        habits = category.habits.map { h ->
                            if (h.id == habitId) h.copy(isChecked = false)
                            else h
                        }
                    )
                },
                editingHabitId = null
            )
        }
    }

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
                            // Lưu habit vào DB
                            val habitEntity = HabitEntity(
                                user_id = userId,
                                category_id = category.categoryId,
                                title = habit.title,
                                description = null,
                                start_date = now,
                                end_date = null,
                                reminder_time = habit.reminderTimeFormatted,
                                reminder_repeat = habit.repeatLabel,
                                created_at = now
                            )
                            val habitId = habitRepository.addHabit(habitEntity)

                            // Lưu reminder vào DB
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
}
