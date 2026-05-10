package com.example.betterme.presentation.onboarding.habitselection

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.betterme.data.local.datastore.DataStoreManager
import com.example.betterme.data.local.fake.fakeCategories
import com.example.betterme.data.local.room.entities.CategoryEntity
import com.example.betterme.domain.repository.CategoryRepository
import com.example.betterme.domain.repository.UserCategoryRepository
import com.example.betterme.presentation.onboarding.model.CategoryUiModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class HabitSelectionViewModel(
    private val categoryRepository: CategoryRepository,
    private val userCategoryRepository: UserCategoryRepository,
    private val dataStoreManager: DataStoreManager
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
        seedCategoriesIfEmpty()
        observeCategories()
    }

    fun onIntent(intent: HabitSelectionIntent) {
        when (intent) {
            is HabitSelectionIntent.ToggleCategory -> toggleCategory(intent.id)
            is HabitSelectionIntent.Continue -> continueFlow()
        }
    }

    private fun observeCategories() {
        viewModelScope.launch {
            // Pre-load this user's existing selection set (if any) so re-entering the screen
            // shows previously-selected items pre-checked. Critically, never includes
            // selections from other users on the same device.
            val userId = dataStoreManager.getCurrentUserId().first().orEmpty()
            if (userId.isNotBlank()) {
                selectedIds.clear()
                selectedIds.addAll(userCategoryRepository.getSelectedIds(userId))
            }

            categoryRepository.getAll().collect { list ->
                updateState(list)
            }
        }
    }

    private fun updateState(list: List<CategoryEntity>) {
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
        if (selectedIds.isEmpty()) {
            emitError("Vui lòng chọn ít nhất 1 nhóm thói quen")
            return
        }
        viewModelScope.launch {
            val userId = dataStoreManager.getCurrentUserId().first().orEmpty()
            if (userId.isBlank()) {
                emitError("Không tìm thấy thông tin người dùng")
                return@launch
            }
            // Persist selection scoped to THIS user only.
            userCategoryRepository.saveSelections(userId, selectedIds)
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

    private fun seedCategoriesIfEmpty() {
        viewModelScope.launch {
            val current = categoryRepository.getAll().first()
            if (current.isEmpty()) {
                categoryRepository.insertAll(fakeCategories())
            }
        }
    }
}
