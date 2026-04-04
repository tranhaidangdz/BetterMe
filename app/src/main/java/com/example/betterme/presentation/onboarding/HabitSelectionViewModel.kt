package com.example.betterme.presentation.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.betterme.data.local.fake.fakeCategories
import com.example.betterme.domain.repository.CategoryRepository
import com.example.betterme.presentation.onboarding.model.CategoryUiModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class HabitSelectionViewModel(
    private val repository: CategoryRepository
) : ViewModel() {

    companion object {
        private const val MAX_SELECTION = 6
    }

    private val _state = MutableStateFlow(HabitSelectionState())
    val state: StateFlow<HabitSelectionState> = _state.asStateFlow()

    private val _event = MutableSharedFlow<HabitSelectionEvent>()
    val event = _event.asSharedFlow()

    private val selectedIds = mutableSetOf<Int>()

    init {
        observeCategories()
        insertFakeData()
    }

    fun onIntent(intent: HabitSelectionIntent) {
        when (intent) {
            is HabitSelectionIntent.ToggleCategory -> toggleCategory(intent.id)
            is HabitSelectionIntent.Continue -> continueFlow()
        }
    }

    private fun observeCategories() {
        viewModelScope.launch {
            repository.getAll().collect { list ->
                updateState(list)
            }
        }
    }

    private fun updateState(list: List<com.example.betterme.data.local.room.entities.CategoryEntity>) {
        val uiList = list.map {
            CategoryUiModel(
                id = it.id,
                name = it.name,
                description = it.description,
                icon = it.icon,
                isSelected = selectedIds.contains(it.id)
            )
        }

        _state.update {
            it.copy(
                categories = uiList,
                selectedCount = selectedIds.size
            )
        }
    }

    private fun toggleCategory(id: Int) {
        if (selectedIds.contains(id)) {
            selectedIds.remove(id)
        } else {
            if (selectedIds.size >= MAX_SELECTION) {
                emitError("Bạn chỉ chọn tối đa $MAX_SELECTION mục")
                return
            }
            selectedIds.add(id)
        }

        _state.update {
            it.copy(
                categories = it.categories.map { item ->
                    item.copy(isSelected = selectedIds.contains(item.id))
                },
                selectedCount = selectedIds.size
            )
        }
    }

    private fun continueFlow() {
        viewModelScope.launch {
            _event.emit(
                HabitSelectionEvent.NavigateNext(selectedIds.toSet())
            )
        }
    }

    private fun emitError(message: String) {
        viewModelScope.launch {
            _event.emit(HabitSelectionEvent.ShowError(message))
        }
    }

    private fun insertFakeData() {
        viewModelScope.launch {
            val current = repository.getAll().first()
            if (current.isEmpty()) {
                repository.insertAll(fakeCategories())
            }
        }
    }
}