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

    private val _selectedIds = MutableStateFlow<Set<Int>>(emptySet())

    companion object {
        private const val MAX_SELECTION = 6
    }

    val categories: StateFlow<List<CategoryUiModel>> =
        repository.getAll()
            .combine(_selectedIds) { list, selected ->
                list.map {
                    CategoryUiModel(
                        id = it.id,
                        name = it.name,
                        description = it.description,
                        icon = it.icon,
                        isSelected = selected.contains(it.id)
                    )
                }
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val selectedCount: StateFlow<Int> =
        _selectedIds.map { it.size }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), 0)

    init {
        insertFakeData()
    }

    private fun insertFakeData() {
        viewModelScope.launch {
            val current = repository.getAll().first()
            if (current.isEmpty()) {
                repository.insertAll(fakeCategories())
            }
        }
    }

    fun toggleCategory(id: Int) {
        val current = _selectedIds.value.toMutableSet()

        if (current.contains(id)) {
            current.remove(id)
        } else {
            if (current.size >= MAX_SELECTION) return // giới hạn 3
            current.add(id)
        }

        _selectedIds.value = current
    }

    fun getSelectedIds(): Set<Int> = _selectedIds.value
}