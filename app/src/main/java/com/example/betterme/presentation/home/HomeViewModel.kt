package com.example.betterme.presentation.home

import androidx.lifecycle.viewModelScope
import com.example.betterme.base.BaseMviViewModel
import com.example.betterme.data.local.datastore.DataStoreManager
import com.example.betterme.data.local.room.entities.UserEntity
import com.example.betterme.data.provider.GoogleAuthClient
import com.example.betterme.domain.repository.CategoryRepository
import com.example.betterme.domain.repository.HabitLogRepository
import com.example.betterme.domain.repository.HabitRepository
import com.example.betterme.domain.repository.NotificationRepository
import com.example.betterme.domain.repository.UserCategoryRepository
import com.example.betterme.domain.repository.UserRepository
import com.example.betterme.presentation.home.model.CantMiss
import com.example.betterme.presentation.home.model.HomeProgress
import com.example.betterme.presentation.theme.BetterMeColors
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.Calendar
import kotlin.random.Random

class HomeViewModel(
    private val dataStoreManager: DataStoreManager,
    private val categoryRepository: CategoryRepository,
    private val userCategoryRepository: UserCategoryRepository,
    private val habitRepository: HabitRepository,
    private val habitLogRepository: HabitLogRepository,
    private val googleAuthClient: GoogleAuthClient,
    private val userRepository: UserRepository,
    private val notificationRepository: NotificationRepository
) : BaseMviViewModel<HomeIntent, HomeState, HomeEvent>() {

    private companion object {
        const val DAY_MS: Long = 24L * 60L * 60L * 1000L
    }

    override fun initState(): HomeState = HomeState()

    init {
        processIntent(HomeIntent.LoadData)
        observeNotifications()
    }

    override fun processIntent(intent: HomeIntent) {
        when (intent) {
            HomeIntent.LoadData -> loadData()
            HomeIntent.ShowEditProfile -> updateState { copy(showEditProfileDialog = true) }
            HomeIntent.DismissEditProfile -> updateState { copy(showEditProfileDialog = false) }
            is HomeIntent.UpdateUserName -> updateUserName(intent.name)
            is HomeIntent.UpdateUserPhoto -> updateUserPhoto(intent.photoUri)
            HomeIntent.Logout -> logout()
            HomeIntent.OpenNotificationCenter -> updateState { copy(showNotificationCenter = true) }
            HomeIntent.DismissNotificationCenter -> updateState { copy(showNotificationCenter = false) }
            is HomeIntent.MarkNotificationRead -> viewModelScope.launch {
                notificationRepository.markAsRead(intent.id)
            }
            HomeIntent.MarkAllNotificationsRead -> viewModelScope.launch {
                val userId = dataStoreManager.getCurrentUserId().first().orEmpty()
                if (userId.isNotBlank()) notificationRepository.markAllReadForUser(userId)
            }
        }
    }

    private fun observeNotifications() {
        viewModelScope.launch {
            val userId = dataStoreManager.getCurrentUserId().first().orEmpty()
            if (userId.isBlank()) return@launch
            kotlinx.coroutines.flow.combine(
                notificationRepository.observeForUser(userId),
                notificationRepository.observeUnreadCount(userId)
            ) { list, unread -> list to unread }.collect { (list, unread) ->
                updateState {
                    copy(
                        notifications = list,
                        unreadNotificationCount = unread
                    )
                }
            }
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

            // 2. Lấy selected categories từ DB — scoped chặt theo userId hiện tại,
            //    KHÔNG dùng global flag (đã từng leak giữa các tài khoản trên cùng device).
            val selectedCategories = if (userId.isNotBlank()) {
                userCategoryRepository.getSelectedCategories(userId)
            } else emptyList()
            val allCategories = categoryRepository.getAll().first()
            val colors = BetterMeColors.ListColors.list
            val randomizedColors = colors.shuffled()

            // 3. Build category groups với habit count thực — đếm theo userId.
            val categoryGroups = selectedCategories.mapIndexed { index, category ->
                val habitCount = if (userId.isNotBlank()) {
                    habitRepository.getHabitCountByCategoryForUser(category.id, userId)
                } else 0
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
            val today = getStartOfDay()
            val completedHabitIds = habitLogRepository.getCompletedHabitIdsByDate(today)

            // 5. Build "Đang thực hiện" — show every habit whose journey is still in
            //    progress, regardless of whether the user already checked in today.
            //    A habit's journey is COMPLETE when total DONE days >= its planned duration.
            //    Open-ended habits (no end_date) never finish automatically.
            val cantMissList = habits.mapNotNull { habit ->
                val durationDays = if (habit.end_date != null) {
                    (((habit.end_date - habit.start_date) / DAY_MS) + 1).toInt().coerceAtLeast(1)
                } else {
                    Int.MAX_VALUE
                }
                val doneCount = habitLogRepository.countCompleted(habit.id)
                if (doneCount >= durationDays) {
                    // Journey finished — drop from Home.
                    return@mapNotNull null
                }

                val category = allCategories.find { it.id == habit.category_id }
                val habitProgress = if (durationDays == Int.MAX_VALUE) {
                    // Open-ended habit: show "elapsed" days instead of journey %.
                    val elapsed = (((today - habit.start_date) / DAY_MS) + 1).toInt().coerceAtLeast(1)
                    ((doneCount.toFloat() / elapsed) * 100).toInt().coerceIn(0, 100)
                } else {
                    ((doneCount.toFloat() / durationDays) * 100).toInt().coerceIn(0, 100)
                }

                CantMiss(
                    habitId = habit.id,
                    categoryId = category?.id ?: -1,
                    categoryName = category?.name ?: "Khác",
                    categoryIcon = category?.icon ?: "📝",
                    habitTitle = habit.title,
                    progress = habitProgress
                )
            }

            // 6. Tính progress tổng cho card trên cùng
            val totalHabits = habits.size
            val completedHabits = habits.count { it.id in completedHabitIds }
            val percentage = if (totalHabits > 0) {
                ((completedHabits.toFloat() / totalHabits) * 100).toInt()
            } else 0

            val progress = HomeProgress(
                totalHabits = totalHabits,
                completedHabits = completedHabits,
                percentage = percentage
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
            persistProfileToLocalUserTable(updatedName = name)
            updateState { copy(userName = name, showEditProfileDialog = false) }
        }
    }

    private fun updateUserPhoto(photoUri: String) {
        viewModelScope.launch {
            dataStoreManager.updateUserPhotoUrl(photoUri)
            persistProfileToLocalUserTable(updatedPhotoUrl = photoUri)
            updateState { copy(userPhotoUrl = photoUri) }
        }
    }

    private suspend fun persistProfileToLocalUserTable(
        updatedName: String? = null,
        updatedPhotoUrl: String? = null
    ) {
        val userInfo = dataStoreManager.getUserInfo().first()
        val userId = userInfo?.id ?: dataStoreManager.getCurrentUserId().first().orEmpty()
        if (userId.isBlank()) return

        val existingUser = userRepository.getUserById(userId)
        val mergedName = updatedName ?: userInfo?.name.orEmpty().ifBlank { existingUser?.name ?: "User" }
        val mergedPhoto = updatedPhotoUrl ?: userInfo?.photoUrl.orEmpty().ifBlank { existingUser?.photoUrl.orEmpty() }
        val mergedEmail = userInfo?.email.orEmpty().ifBlank { existingUser?.email.orEmpty() }

        val entity = UserEntity(
            id = userId,
            name = mergedName,
            email = mergedEmail,
            photoUrl = mergedPhoto,
            created_at = existingUser?.created_at ?: System.currentTimeMillis()
        )

        userRepository.insertUser(entity)
    }

    private fun logout() {
        viewModelScope.launch {
            updateState { copy(isLoading = true) }
            try {
                googleAuthClient.signOut()
                dataStoreManager.clearUserInfo()
                sendEvent(HomeEvent.NavigateToSignIn)
            } catch (e: Exception) {
                sendEvent(
                    HomeEvent.ShowError(
                        e.message ?: "Đăng xuất thất bại, vui lòng thử lại"
                    )
                )
            } finally {
                updateState { copy(isLoading = false) }
            }
        }
    }

    private fun getStartOfDay(): Long {
        return Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }
}
