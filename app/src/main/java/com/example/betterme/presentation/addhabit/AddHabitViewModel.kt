package com.example.betterme.presentation.addhabit

import androidx.lifecycle.viewModelScope
import com.example.betterme.base.BaseMviViewModel
import com.example.betterme.data.local.datastore.DataStoreManager
import com.example.betterme.data.local.room.entities.HabitEntity
import com.example.betterme.data.local.room.entities.ReminderEntity
import com.example.betterme.domain.repository.CategoryRepository
import com.example.betterme.domain.repository.HabitRepository
import com.example.betterme.domain.repository.ReminderRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class AddHabitViewModel(
    private val dataStoreManager: DataStoreManager,
    private val categoryRepository: CategoryRepository,
    private val habitRepository: HabitRepository,
    private val reminderRepository: ReminderRepository
) : BaseMviViewModel<AddHabitIntent, AddHabitState, AddHabitEvent>() {

    val repeatOptions = listOf("Hàng ngày", "Các ngày trong tuần", "Cuối tuần", "Tùy chỉnh")

    override fun initState(): AddHabitState = AddHabitState()

    init {
        processIntent(AddHabitIntent.LoadData)
    }

    override fun processIntent(intent: AddHabitIntent) {
        when (intent) {
            AddHabitIntent.LoadData -> loadData()
            is AddHabitIntent.SelectCategory -> selectCategory(intent.id)
            is AddHabitIntent.ChangeTitle -> updateState { copy(title = intent.value) }
            is AddHabitIntent.ChangeDescription -> updateState { copy(description = intent.value) }
            is AddHabitIntent.SetStartDate -> updateState { copy(startDateMillis = intent.millis) }
            is AddHabitIntent.SetEndDate -> updateState { copy(endDateMillis = intent.millis) }
            is AddHabitIntent.SetReminderTime -> updateState {
                copy(reminderHour = intent.hour, reminderMinute = intent.minute)
            }
            is AddHabitIntent.SetRepeatPattern -> updateState { copy(repeatPattern = intent.value) }
            AddHabitIntent.Submit -> submitHabit()
        }
    }

    private fun loadData() {
        viewModelScope.launch {
            val categories = categoryRepository.getAll().first().map {
                AddHabitCategoryUiModel(id = it.id, name = it.name, icon = it.icon)
            }
            val selected = categories.firstOrNull()
            updateState {
                copy(
                    categories = categories,
                    selectedCategoryId = selected?.id,
                    selectedCategoryName = selected?.name.orEmpty()
                )
            }
        }
    }

    private fun selectCategory(id: Int) {
        val selected = currentState.categories.firstOrNull { it.id == id } ?: return
        updateState {
            copy(selectedCategoryId = selected.id, selectedCategoryName = selected.name)
        }
    }

    private fun submitHabit() {
        if (currentState.title.isBlank()) {
            sendEvent(AddHabitEvent.ShowError("Vui lòng nhập tên thói quen"))
            return
        }
        if (currentState.selectedCategoryId == null) {
            sendEvent(AddHabitEvent.ShowError("Vui lòng chọn nhóm thói quen"))
            return
        }
        if (currentState.endDateMillis < currentState.startDateMillis) {
            sendEvent(AddHabitEvent.ShowError("Ngày kết thúc phải lớn hơn hoặc bằng ngày bắt đầu"))
            return
        }

        viewModelScope.launch {
            updateState { copy(isLoading = true) }
            try {
                val userId = dataStoreManager.getCurrentUserId().first()
                if (userId.isNullOrBlank()) {
                    sendEvent(AddHabitEvent.ShowError("Không tìm thấy thông tin người dùng"))
                    return@launch
                }

                val now = System.currentTimeMillis()
                val habitEntity = HabitEntity(
                    user_id = userId,
                    category_id = currentState.selectedCategoryId,
                    title = currentState.title.trim(),
                    description = currentState.description.trim().ifBlank { null },
                    start_date = currentState.startDateMillis,
                    end_date = currentState.endDateMillis,
                    reminder_time = currentState.reminderTimeFormatted,
                    reminder_repeat = currentState.repeatPattern,
                    created_at = now
                )
                val habitId = habitRepository.addHabit(habitEntity).toInt()
                reminderRepository.addReminder(
                    ReminderEntity(
                        habit_id = habitId,
                        time = currentState.reminderTimeFormatted,
                        repeat_pattern = currentState.repeatPattern,
                        is_active = true
                    )
                )
                sendEvent(AddHabitEvent.ShowSuccess("Đã thêm thói quen"))
                updateState {
                    copy(
                        title = "",
                        description = "",
                        startDateMillis = now,
                        endDateMillis = now,
                        reminderHour = 7,
                        reminderMinute = 0,
                        repeatPattern = "Hàng ngày"
                    )
                }
            } catch (_: Exception) {
                sendEvent(AddHabitEvent.ShowError("Có lỗi xảy ra, vui lòng thử lại"))
            } finally {
                updateState { copy(isLoading = false) }
            }
        }
    }
}
