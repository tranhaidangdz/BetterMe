package com.example.betterme.presentation.home

import androidx.lifecycle.viewModelScope
import com.example.betterme.base.BaseMviViewModel
import com.example.betterme.data.local.datastore.DataStoreManager
import com.example.betterme.data.provider.GoogleAuthClient
import com.example.betterme.domain.repository.CategoryRepository
import com.example.betterme.domain.repository.HabitRepository
import com.example.betterme.presentation.home.model.CantMiss
import com.example.betterme.presentation.home.model.HomeProgress
import com.example.betterme.presentation.theme.BetterMeColors
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlin.random.Random

class HomeViewModel(
    private val dataStoreManager: DataStoreManager,
    private val googleAuthClient: GoogleAuthClient,
    private val categoryRepository: CategoryRepository,
    private val habitRepository: HabitRepository
) : BaseMviViewModel<HomeIntent, HomeState, HomeEvent>() {

    override fun initState(): HomeState = HomeState()

    init {
        processIntent(HomeIntent.LoadData)
    }

    override fun processIntent(intent: HomeIntent) {
        when (intent) {
            HomeIntent.LoadData -> loadData()
            HomeIntent.ShowEditProfile -> updateState { copy(showEditProfileDialog = true) }
            HomeIntent.DismissEditProfile -> updateState { copy(showEditProfileDialog = false) }
            is HomeIntent.UpdateUserName -> updateUserName(intent.name)
            is HomeIntent.UpdateUserPhoto -> updateUserPhoto(intent.photoUri)
            HomeIntent.Logout -> logout()
        }
    }

    private fun loadData() {
        viewModelScope.launch {
            updateState { copy(isLoading = true) }

            // 1. Lấy user info từ DataStore
            val user = dataStoreManager.getUserInfo().first()
            val userName = user?.name ?: "Guest"
            val userPhotoUrl = user?.photoUrl ?: ""
            val userId = user?.id ?: dataStoreManager.getCurrentUserId().first() ?: ""

            // 2. Lấy selected categories từ DB
            val selectedCategories = categoryRepository.getSelectedCategories().first()
            val allCategories = categoryRepository.getAll().first()
            val colors = BetterMeColors.ListColors.list
            val randomizedColors = colors.shuffled()

            // 3. Build category groups với habit count thực
            val categoryGroups = selectedCategories.mapIndexed { index, category ->
                val habitCount = habitRepository.getHabitCountByCategory(category.id)
                HomeCategoryGroup(
                    categoryId = category.id,
                    categoryName = category.name.uppercase(),
                    categoryIcon = category.icon,
                    habitCount = habitCount,
                    color = randomizedColors.getOrElse(index) {
                        colors[Random.nextInt(colors.size)]
                    }
                )
            }

            // 4. Build "Đang thực hiện" list từ habits thực
            val habits = if (userId.isNotBlank()) {
                habitRepository.getHabits(userId).first()
            } else {
                emptyList()
            }

            val cantMissList = habits.mapNotNull { habit ->
                val category = allCategories.find { it.id == habit.category_id }
                CantMiss(
                    categoryId = category?.id ?: -1,
                    categoryName = category?.name ?: "Khác",
                    categoryIcon = category?.icon ?: "📝",
                    habitTitle = habit.title,
                    progress = 0 // Sẽ tính sau khi có HabitLog tracking
                )
            }

            // 5. Tính progress
            val totalHabits = habits.size
            val progress = HomeProgress(
                totalHabits = totalHabits,
                completedHabits = 0, // Sẽ tính sau khi có HabitLog tracking
                percentage = 0
            )

            updateState {
                copy(
                    isLoading = false,
                    userName = userName,
                    userPhotoUrl = userPhotoUrl,
                    progress = progress,
                    cantMissList = cantMissList,
                    categoryGroups = categoryGroups
                )
            }
        }
    }

    private fun updateUserName(name: String) {
        viewModelScope.launch {
            dataStoreManager.updateUserName(name)
            updateState { copy(userName = name, showEditProfileDialog = false) }
        }
    }

    private fun updateUserPhoto(photoUri: String) {
        viewModelScope.launch {
            dataStoreManager.updateUserPhotoUrl(photoUri)
            updateState { copy(userPhotoUrl = photoUri) }
        }
    }

    private fun logout() {
        viewModelScope.launch {
            try {
                googleAuthClient.signOut()
                dataStoreManager.clearUserInfo()
                sendEvent(HomeEvent.LoggedOut)
            } catch (_: Exception) {
                sendEvent(HomeEvent.ShowError("Không thể đăng xuất, vui lòng thử lại"))
            }
        }
    }
}
