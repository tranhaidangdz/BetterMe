package com.example.betterme.presentation.home

import androidx.lifecycle.viewModelScope
import com.example.betterme.base.BaseMviViewModel
import com.example.betterme.data.local.datastore.DataStoreManager
import com.example.betterme.data.local.room.entities.UserEntity
import com.example.betterme.data.provider.GoogleAuthClient
import com.example.betterme.domain.repository.CategoryRepository
import com.example.betterme.domain.repository.HabitLogRepository
import com.example.betterme.domain.repository.HabitRepository
import com.example.betterme.domain.repository.ChallengeLogRepository
import com.example.betterme.domain.repository.ImageUploadRepository
import com.example.betterme.domain.repository.NotificationRepository
import com.example.betterme.domain.repository.UserCategoryRepository
import com.example.betterme.domain.repository.UserChallengeRepository
import com.example.betterme.domain.repository.UserRepository
import com.example.betterme.domain.habit.HabitActivityRules
import com.example.betterme.presentation.home.model.CantMiss
import com.example.betterme.presentation.home.model.HomeProgress
import com.example.betterme.presentation.theme.BetterMeColors
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
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
    private val notificationRepository: NotificationRepository,
    private val userChallengeRepository: UserChallengeRepository,
    private val challengeLogRepository: ChallengeLogRepository,
    private val imageUploadRepository: ImageUploadRepository
) : BaseMviViewModel<HomeIntent, HomeState, HomeEvent>() {

    private companion object {
        const val DAY_MS: Long = 24L * 60L * 60L * 1000L

        /**
         * Poll interval for the day-boundary ticker. 60 s is fine-grained enough
         * that a habit expires within a minute of local midnight while the app
         * is open, yet `distinctUntilChanged` means the heavy recompute only runs
         * when the calendar day actually changes — not every minute.
         */
        const val DAY_TICK_MS: Long = 60_000L
    }

    /**
     * Emits the current start-of-day epoch every [DAY_TICK_MS], deduplicated so a
     * new value only propagates when the calendar day rolls over. Combined into
     * the Home flow so expired habits drop off automatically at midnight without
     * any manual refresh — and without an always-on per-minute recompute.
     */
    private fun dayTickerFlow(): Flow<Long> = flow {
        while (true) {
            emit(getStartOfDay())
            delay(DAY_TICK_MS)
        }
    }.distinctUntilChanged()

    /**
     * Long-running job for the habits/logs/challenges Flow.combine. Tracked here so
     * a re-entrant [HomeIntent.LoadData] (fired when [HomeState.homeRefreshVersion]
     * bumps after a habit-add) cancels the previous observer before starting a new
     * one — otherwise each reload would leak another infinite collector into
     * viewModelScope.
     */
    private var observeJob: Job? = null

    /** Carries the 4-way combine result with destructuring support. */
    private data class HomeCombine(
        val habits: List<com.example.betterme.data.local.room.entities.HabitEntity>,
        val activeChallenges: List<com.example.betterme.data.local.room.entities.UserChallengeEntity>,
        val today: Long
    )

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
        // Cancel previous observer so re-entrant LoadData calls don't pile up
        // infinite collectors on viewModelScope.
        observeJob?.cancel()
        observeJob = viewModelScope.launch {
            updateState { copy(isLoading = true) }

            // 1. Lấy user info từ DataStore (one-shot — không đổi trong session)
            val user = dataStoreManager.getUserInfo().first()
            val userName = user?.name ?: "Guest"
            val userPhotoUrl = user?.photoUrl ?: ""
            val userId = user?.id ?: dataStoreManager.getCurrentUserId().first() ?: ""

            // 2. Selected categories + master category list — scoped theo userId.
            val selectedCategories = if (userId.isNotBlank()) {
                userCategoryRepository.getSelectedCategories(userId)
            } else emptyList()
            val allCategories = categoryRepository.getAll().first()
            val colors = BetterMeColors.ListColors.list
            val randomizedColors = colors.shuffled()

            // 3. Push user info + initial loading=false ngay để UI render avatar
            //    trong khi vòng lặp reactive bên dưới rebuild cantMissList / progress.
            updateState {
                copy(
                    isLoading = false,
                    userName = userName,
                    userPhotoUrl = userPhotoUrl
                )
            }

            if (userId.isBlank()) return@launch

            // 4. Reactive observation. Khi user thêm habit mới hoặc check-in xong, ba
            //    Flow sau emit → combine block chạy → cantMissList + categoryGroups +
            //    progress được rebuild → state cập nhật ngay lập tức. Không cần
            //    phụ thuộc vào homeRefreshVersion + LaunchedEffect để biết có habit
            //    mới — một habit added từ AddHabit screen hoặc từ AI suggestion sẽ
            //    xuất hiện trên Home trong cùng một frame.
            kotlinx.coroutines.flow.combine(
                habitRepository.getHabits(userId),
                habitLogRepository.observeAllLogs(),
                userChallengeRepository.observeByStatus(userId, "ACTIVE"),
                dayTickerFlow()
            ) { habits, _, activeChallenges, todayTick ->
                // todayTick is the deduplicated start-of-day — carries the day
                // boundary into the combine so expiry re-evaluates at midnight.
                HomeCombine(habits, activeChallenges, todayTick)
            }.collect { (habits, activeChallenges, today) ->
                val completedHabitIds = habitLogRepository.getCompletedHabitIdsByDate(today)

                // 4a. Build "Đang thực hiện" — only habits that are genuinely in
                //     progress survive [HabitActivityRules.isActive]:
                //     not deleted, started, not expired (end date past), not complete.
                //     The DAO already drops soft-deleted rows, but isActive double-
                //     checks so the rule set has one authoritative home.
                val cantMissList = habits.mapNotNull { habit ->
                    val doneCount = habitLogRepository.countCompleted(habit.id)
                    val active = HabitActivityRules.isActive(
                        startDate = habit.start_date,
                        endDate = habit.end_date,
                        doneCount = doneCount,
                        today = today,
                        isDeleted = habit.is_deleted
                    )
                    if (!active) return@mapNotNull null

                    val category = allCategories.find { it.id == habit.category_id }
                    val habitProgress = HabitActivityRules.progressPercent(
                        startDate = habit.start_date,
                        endDate = habit.end_date,
                        doneCount = doneCount,
                        today = today
                    )

                    CantMiss(
                        habitId = habit.id,
                        categoryId = category?.id ?: -1,
                        categoryName = category?.name ?: "Khác",
                        categoryIcon = category?.icon ?: "📝",
                        habitTitle = habit.title,
                        progress = habitProgress
                    )
                }

                // 4b. Category groups — habitCount per category cần fresh theo từng emit.
                val categoryGroups = selectedCategories.mapIndexed { index, category ->
                    val habitCount = habits.count { it.category_id == category.id }
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

                // 4c. Progress tổng + challenge check-in tracker.
                val totalHabits = habits.size
                val completedHabits = habits.count { it.id in completedHabitIds }
                val percentage = if (totalHabits > 0) {
                    ((completedHabits.toFloat() / totalHabits) * 100).toInt()
                } else 0
                val checkedInToday = activeChallenges.count { uc ->
                    val log = challengeLogRepository.getLogByDate(uc.id, today)
                    log?.status == "DONE"
                }
                val progress = HomeProgress(
                    totalHabits = totalHabits,
                    completedHabits = completedHabits,
                    percentage = percentage,
                    totalChallenges = activeChallenges.size,
                    checkedInChallenges = checkedInToday
                )

                updateState {
                    copy(
                        progress = progress,
                        cantMissList = cantMissList,
                        categoryGroups = categoryGroups
                    )
                }
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
            // Upload to Cloudinary so the avatar survives reinstalls and is reachable from
            // any device the user signs into. If the value is already an HTTPS URL (e.g.,
            // returned from Google Sign-In), the uploader transparently passes it through.
            // If upload fails (returns null), keep the existing avatar — never persist
            // a transient local URI that won't survive cache rotation.
            val resolved: String = if (photoUri.startsWith("http://") || photoUri.startsWith("https://")) {
                photoUri
            } else {
                imageUploadRepository.upload(
                    localUri = android.net.Uri.parse(photoUri),
                    folder = ImageUploadRepository.Folder.Profile
                ) ?: run {
                    sendEvent(HomeEvent.ShowError("Tải ảnh đại diện thất bại. Vui lòng thử lại."))
                    return@launch
                }
            }
            dataStoreManager.updateUserPhotoUrl(resolved)
            persistProfileToLocalUserTable(updatedPhotoUrl = resolved)
            updateState { copy(userPhotoUrl = resolved) }
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
