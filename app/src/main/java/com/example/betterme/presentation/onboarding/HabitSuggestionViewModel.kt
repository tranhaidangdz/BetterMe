package com.example.betterme.presentation.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.betterme.data.local.fake.fakeHabitGroups
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class HabitSuggestionViewModel(
    private val selectedCategoryIds: List<Int>
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
            is HabitSuggestionIntent.SetReminderTime -> setReminderTime(intent.hour, intent.minute)
            is HabitSuggestionIntent.SetRepeat -> setRepeat(intent.label)
            HabitSuggestionIntent.ShowReminderPicker -> showReminderPicker(true)
            HabitSuggestionIntent.DismissReminderPicker -> showReminderPicker(false)
            HabitSuggestionIntent.ShowRepeatPicker -> showRepeatPicker(true)
            HabitSuggestionIntent.DismissRepeatPicker -> showRepeatPicker(false)
            HabitSuggestionIntent.StartJourney -> startJourney()
        }
    }

    private fun loadSuggestedHabits() {
        val allGroups = fakeHabitGroups()
        val filteredGroups = allGroups.filter { it.categoryId in selectedCategoryIds }

        var globalId = 1
        val categoryWithHabits = filteredGroups.map { group ->
            CategoryWithHabits(
                categoryName = group.categoryName,
                categoryIcon = group.categoryIcon,
                habits = group.habits.map { title ->
                    SuggestedHabitUiModel(
                        id = globalId++,
                        title = title,
                        isChecked = true
                    )
                }
            )
        }

        _state.update { it.copy(categoryHabits = categoryWithHabits) }
    }

    private fun toggleHabit(id: Int) {
        _state.update { current ->
            current.copy(
                categoryHabits = current.categoryHabits.map { category ->
                    category.copy(
                        habits = category.habits.map { habit ->
                            if (habit.id == id) habit.copy(isChecked = !habit.isChecked)
                            else habit
                        }
                    )
                }
            )
        }
    }

    private fun setReminderTime(hour: Int, minute: Int) {
        _state.update { it.copy(reminderHour = hour, reminderMinute = minute, showReminderPicker = false) }
    }

    private fun setRepeat(label: String) {
        _state.update { it.copy(repeatLabel = label, showRepeatPicker = false) }
    }

    private fun showReminderPicker(show: Boolean) {
        _state.update { it.copy(showReminderPicker = show) }
    }

    private fun showRepeatPicker(show: Boolean) {
        _state.update { it.copy(showRepeatPicker = show) }
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
            _event.emit(HabitSuggestionEvent.NavigateToSignIn)
        }
    }
}
