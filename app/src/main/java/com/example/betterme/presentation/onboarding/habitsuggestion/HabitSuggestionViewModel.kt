package com.example.betterme.presentation.onboarding.habitsuggestion

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.betterme.data.local.datastore.DataStoreManager
import com.example.betterme.data.local.fake.fakeHabitGroups
import com.example.betterme.data.local.room.entities.HabitEntity
import com.example.betterme.data.local.room.entities.ReminderEntity
import com.example.betterme.data.local.room.entities.UserEntity
import com.example.betterme.domain.repository.CategoryRepository
import com.example.betterme.domain.repository.HabitRepository
import com.example.betterme.domain.repository.ReminderRepository
import com.example.betterme.domain.repository.UserRepository
import com.example.betterme.utils.DateUtils
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.random.Random

class HabitSuggestionViewModel(
    private val selectedCategoryIds: List<Int>,
    private val dataStoreManager: DataStoreManager,
    private val categoryRepository: CategoryRepository,
    private val habitRepository: HabitRepository,
    private val reminderRepository: ReminderRepository,
    private val userRepository: UserRepository,
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
            is HabitSuggestionIntent.SetStartDate -> setStartDate(intent.habitId, intent.dateMillis)
            is HabitSuggestionIntent.SetEndDate -> setEndDate(intent.habitId, intent.dateMillis)
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
            val userId = dataStoreManager.getCurrentUserId().first().orEmpty()
            val selectedCategoriesById = categoryRepository.getAll().first()
                .filter { it.id in selectedCategoryIds }
                .associateBy { it.id }

            val allGroups = fakeHabitGroups()
            val filteredGroups = allGroups.filter { group -> selectedCategoriesById.containsKey(group.categoryId) }

            var globalId = 1
            val categoryWithHabits = filteredGroups.mapNotNull { group ->
                val selectedCategory = selectedCategoriesById[group.categoryId] ?: return@mapNotNull null
                val personalizedHabits = group.habits
                    .personalizedPick(userId = userId, categoryId = group.categoryId, maxItems = 5)
                CategoryWithHabits(
                    categoryName = selectedCategory.name,
                    categoryIcon = selectedCategory.icon,
                    categoryId = selectedCategory.id,
                    habits = personalizedHabits.map { title ->
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

    private fun List<String>.personalizedPick(userId: String, categoryId: Int, maxItems: Int): List<String> {
        if (isEmpty()) return emptyList()
        val seed = "$userId-$categoryId".hashCode()
        return this
            .shuffled(Random(seed))
            .take(maxItems.coerceAtMost(this.size))
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

    private fun setStartDate(habitId: Int, dateMillis: Long) {
        _state.update { current ->
            current.copy(
                categoryHabits = current.categoryHabits.map { category ->
                    category.copy(
                        habits = category.habits.map { h ->
                            if (h.id == habitId) {
                                h.copy(
                                    startDate = dateMillis,
                                    endDate = h.endDate?.takeIf { it >= dateMillis }
                                )
                            } else h
                        }
                    )
                }
            )
        }
    }

    private fun setEndDate(habitId: Int, dateMillis: Long?) {
        val habit = _state.value.categoryHabits.flatMap { it.habits }.find { it.id == habitId } ?: return
        if (dateMillis != null && dateMillis < habit.startDate) {
            viewModelScope.launch {
                _event.emit(HabitSuggestionEvent.ShowError("Ngày kết thúc phải lớn hơn hoặc bằng ngày bắt đầu"))
            }
            return
        }
        _state.update { current ->
            current.copy(
                categoryHabits = current.categoryHabits.map { category ->
                    category.copy(
                        habits = category.habits.map { h ->
                            if (h.id == habitId) h.copy(endDate = dateMillis) else h
                        }
                    )
                }
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
        val habit = _state.value.categoryHabits
            .flatMap { it.habits }
            .find { it.id == habitId } ?: return

        if (habit.endDate == null) {
            viewModelScope.launch {
                _event.emit(HabitSuggestionEvent.ShowError("Bạn chưa chọn ngày kết thúc cho thói quen này"))
            }
            return
        }

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

        val selectedHabitWithoutEndDate = _state.value.categoryHabits
            .flatMap { it.habits }
            .firstOrNull { it.isChecked && it.endDate == null }

        if (selectedHabitWithoutEndDate != null) {
            _state.update { it.copy(editingHabitId = selectedHabitWithoutEndDate.id) }
            viewModelScope.launch {
                _event.emit(
                    HabitSuggestionEvent.ShowError(
                        "Thói quen \"${selectedHabitWithoutEndDate.title}\" chưa có ngày kết thúc"
                    )
                )
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

                // Ensure local user exists to avoid FK error when inserting habits.
                if (userRepository.getUserById(userId) == null) {
                    val userInfo = dataStoreManager.getUserInfo().first()
                    userRepository.insertUser(
                        UserEntity(
                            id = userId,
                            name = userInfo?.name.orEmpty().ifBlank { "User" },
                            email = userInfo?.email.orEmpty(),
                            photoUrl = userInfo?.photoUrl.orEmpty(),
                            created_at = System.currentTimeMillis()
                        )
                    )
                }

                _state.value.categoryHabits.forEach { category ->
                    category.habits
                        .filter { it.isChecked }
                        .forEach { habit ->
                            // Lưu habit vào DB. Normalize cả start_date và end_date
                            // về startOfDay để các filter theo ngày phía read-side
                            // (DailyHabits, Statistics, …) so sánh ngày-chính-xác.
                            val habitEntity = HabitEntity(
                                user_id = userId,
                                category_id = category.categoryId,
                                title = habit.title,
                                description = null,
                                start_date = DateUtils.startOfDay(habit.startDate),
                                end_date = habit.endDate?.let { DateUtils.startOfDay(it) },
                                reminder_time = habit.reminderTimeFormatted,
                                created_at = System.currentTimeMillis()
                            )
                            val habitId = habitRepository.addHabit(habitEntity)

                            // Lưu reminder vào DB
                            val reminderEntity = ReminderEntity(
                                target_type = "HABIT",
                                target_id = habitId.toInt(),
                                time = habit.reminderTimeFormatted,
                                is_active = true
                            )
                            reminderRepository.addReminder(reminderEntity)
                        }
                }

                dataStoreManager.setHasSelectedHabits()
                _event.emit(HabitSuggestionEvent.NavigateToMain)
            } catch (e: Exception) {
                e.printStackTrace()
                _event.emit(
                    HabitSuggestionEvent.ShowError(
                        e.message ?: "Có lỗi xảy ra, vui lòng thử lại"
                    )
                )
            } finally {
                _state.update { it.copy(isLoading = false) }
            }
        }
    }
}
