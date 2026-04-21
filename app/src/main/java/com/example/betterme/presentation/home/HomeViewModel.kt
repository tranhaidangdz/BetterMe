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
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import java.util.Random

class HomeViewModel(
    private val dataStoreManager: DataStoreManager,
    private val googleAuthClient: GoogleAuthClient,
    private val categoryRepository: CategoryRepository,
    private val habitRepository: HabitRepository
) : BaseMviViewModel<HomeIntent, HomeState, HomeEvent>() {

    override fun initState(): HomeState = HomeState()

    // Job để cancel collector cũ khi loadData được gọi lại
    private var dataJob: Job? = null

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

    // ============================================================
    // LOAD DATA — Reactive, per-user
    // Dùng combine() để tự động cập nhật khi habit thay đổi
    // (ví dụ: sau khi AddHabitScreen thêm habit mới)
    // ============================================================
    private fun loadData() {
        dataJob?.cancel() // Hủy collector cũ nếu có
        dataJob = viewModelScope.launch {
            updateState { copy(isLoading = true) }

            // 1. Lấy user info (one-shot, ổn định)
            val user = dataStoreManager.getUserInfo().first()
            val userName = user?.name ?: "Guest"
            val userPhotoUrl = user?.photoUrl ?: ""
            val userId = user?.id ?: dataStoreManager.getCurrentUserId().first() ?: ""

            if (userId.isBlank()) {
                updateState {
                    copy(isLoading = false, userName = userName, userPhotoUrl = userPhotoUrl)
                }
                return@launch
            }

            // 2. Dùng màu nhất quán theo userId (không random mỗi lần reload)
            val colorSeed = userId.hashCode().toLong()
            val colors = BetterMeColors.ListColors.list
            val userColors = colors.shuffled(Random(colorSeed))

            // 3. Combine 2 reactive flows → tự động recompose khi DB thay đổi
            combine(
                habitRepository.getHabits(userId),    // Flow: thay đổi khi add/delete habit
                categoryRepository.getAll()            // Flow: thay đổi khi add/delete category
            ) { habits, allCategories ->

                // ===== Category Groups =====
                // Lấy các category ID duy nhất từ HABITS của user này
                // → Automatically per-user, không phụ thuộc vào isSelected global
                val userCategoryIds = habits
                    .mapNotNull { it.category_id }
                    .distinct()

                val categoryGroups = userCategoryIds.mapIndexedNotNull { index, categoryId ->
                    val category = allCategories.find { it.id == categoryId }
                        ?: return@mapIndexedNotNull null
                    val habitCount = habits.count { it.category_id == categoryId }
                    HomeCategoryGroup(
                        categoryId = category.id,
                        categoryName = category.name.uppercase(),
                        categoryIcon = category.icon,
                        habitCount = habitCount,
                        color = userColors.getOrElse(index) { colors[index % colors.size] }
                    )
                }

                // ===== Cant Miss List (Đang thực hiện) =====
                val cantMissList = habits.mapNotNull { habit ->
                    val category = allCategories.find { it.id == habit.category_id }
                    CantMiss(
                        categoryId = category?.id ?: -1,
                        categoryName = category?.name ?: "Khác",
                        categoryIcon = category?.icon ?: "📝",
                        habitTitle = habit.title,
                        progress = 0 // TODO: tính từ HabitLog
                    )
                }

                // ===== Progress =====
                val progress = HomeProgress(
                    totalHabits = habits.size,
                    completedHabits = 0, // TODO: tính từ HabitLog
                    percentage = 0
                )

                Triple(categoryGroups, cantMissList, progress)
            }.collect { (categoryGroups, cantMissList, progress) ->
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
    }

    // ============================================================
    // UPDATE PROFILE
    // ============================================================
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

    // ============================================================
    // LOGOUT
    // ============================================================
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
