package com.example.betterme.presentation.habitdetail

import android.net.Uri
import androidx.lifecycle.viewModelScope
import com.example.betterme.base.BaseMviViewModel
import com.example.betterme.data.local.datastore.DataStoreManager
import com.example.betterme.data.local.room.entities.HabitLogEntity
import com.example.betterme.domain.repository.CategoryRepository
import com.example.betterme.domain.repository.HabitLogRepository
import com.example.betterme.domain.repository.HabitRepository
import com.example.betterme.domain.repository.ImageUploadRepository
import com.example.betterme.domain.usecase.habit.CancelHabitReminderUseCase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class HabitDetailViewModel(
    private val dataStoreManager: DataStoreManager,
    private val habitRepository: HabitRepository,
    private val habitLogRepository: HabitLogRepository,
    private val categoryRepository: CategoryRepository,
    private val imageUploadRepository: ImageUploadRepository,
    private val cancelHabitReminder: CancelHabitReminderUseCase
) : BaseMviViewModel<HabitDetailIntent, HabitDetailState, HabitDetailEvent>() {

    override fun initState(): HabitDetailState = HabitDetailState()

    override fun processIntent(intent: HabitDetailIntent) {
        when (intent) {
            is HabitDetailIntent.LoadHabit -> loadHabit(intent.habitId)
            is HabitDetailIntent.SelectTab -> selectTab(intent.tab)
            HabitDetailIntent.PreviousMonth -> changeMonth(-1)
            HabitDetailIntent.NextMonth -> changeMonth(1)

            // Check-in camera flow
            HabitDetailIntent.StartCheckIn -> startCheckIn()
            is HabitDetailIntent.PhotoCaptured -> photoCaptured(intent.uri)
            is HabitDetailIntent.UpdateCheckInNote -> updateCheckInNote(intent.note)
            is HabitDetailIntent.SetLocation -> setLocation(intent.lat, intent.lng, intent.name)
            HabitDetailIntent.ConfirmCheckIn -> confirmCheckIn()
            HabitDetailIntent.DismissCheckIn -> dismissCheckIn()
        }
    }

    // ============================================================
    // LOAD — Lấy thông tin habit + streak + calendar + logs
    // ============================================================
    private fun loadHabit(habitId: Int) {
        viewModelScope.launch {
            updateState { copy(isLoading = true) }
            try {
                val habit = habitRepository.getHabitById(habitId) ?: run {
                    updateState { copy(isLoading = false) }
                    return@launch
                }

                // Lấy category name + icon
                val category = habit.category_id?.let { categoryRepository.getById(it) }

                // Lấy tất cả logs
                val allLogs = habitLogRepository.getLogs(habitId).first()
                val doneLogs = allLogs.filter { it.status == "DONE" }

                // Tính streak
                val today = getStartOfDay(Calendar.getInstance())
                val isCompletedToday = habitLogRepository.getLogByDate(habitId, today)?.status == "DONE"
                val currentStreak = calculateCurrentStreak(doneLogs, today)
                val longestStreak = calculateLongestStreak(doneLogs)

                // Weekly progress (tuần này)
                val weeklyProgress = calculateWeeklyProgress(doneLogs)

                // Calendar cho tháng hiện tại
                val now = Calendar.getInstance()
                val calMonth = now.get(Calendar.MONTH)
                val calYear = now.get(Calendar.YEAR)
                val calendarDays = buildCalendarDays(calMonth, calYear, doneLogs)
                val calendarTitle = formatMonthTitle(calMonth, calYear)

                // Check-in logs (gần nhất trước)
                val checkInLogs = allLogs.sortedByDescending { it.date }.map { log ->
                    CheckInLogUiModel(
                        logId = log.id,
                        dateFormatted = formatLogDate(log.date),
                        timeFormatted = formatLogTime(log.created_at),
                        note = log.note,
                        imageUri = log.image,
                        status = log.status
                    )
                }

                // Stats — journey completion is derived from real check-in count vs the
                // habit's planned duration. Open-ended habits (no end_date) never
                // auto-complete; their `totalDays` reflects elapsed days for display only.
                val DAY_MS = 24L * 60L * 60L * 1000L
                val plannedDurationDays = if (habit.end_date != null) {
                    (((habit.end_date - habit.start_date) / DAY_MS) + 1).toInt().coerceAtLeast(1)
                } else {
                    Int.MAX_VALUE
                }
                val isJourneyComplete = doneLogs.size >= plannedDurationDays
                // Failed = the journey window has fully elapsed AND target not reached.
                // Day-precise comparison: compare end_date's day to today's day so a
                // habit whose end_date is "today" stays Active for all of today and only
                // flips to Failed starting tomorrow. Without normalizing, raw
                // `end_date < today` would mark a habit as Failed mid-day when
                // end_date = today_07:00 but today = today_00:00 (off by hours).
                val isFailed = !isJourneyComplete &&
                    habit.end_date != null &&
                    com.example.betterme.utils.DateUtils.startOfDay(habit.end_date) < today
                // Journey just hit 100% (or window elapsed) → cancel the pending alarm
                // so the user never gets a stale "time to check-in" notification for a
                // habit they can no longer check into. Idempotent if already cancelled.
                if (isJourneyComplete || isFailed) cancelHabitReminder(habitId)
                val totalDays = if (habit.end_date != null) plannedDurationDays
                    else ((today - habit.start_date) / DAY_MS).toInt().coerceAtLeast(1)
                val completionRate = if (totalDays > 0)
                    ((doneLogs.size.toFloat() / totalDays) * 100).toInt().coerceIn(0, 100)
                else 0

                updateState {
                    copy(
                        isLoading = false,
                        habitId = habitId,
                        habitTitle = habit.title,
                        habitDescription = habit.description,
                        categoryName = category?.name ?: "Không phân loại",
                        categoryIcon = category?.icon ?: "📌",
                        reminderTime = habit.reminder_time,
                        isCompletedToday = isCompletedToday,
                        isJourneyComplete = isJourneyComplete,
                        isFailed = isFailed,
                        currentStreak = currentStreak,
                        longestStreak = longestStreak,
                        weeklyProgress = weeklyProgress,
                        weeklyTotal = 7,
                        selectedTab = HabitDetailTab.HISTORY,
                        calendarMonth = calMonth,
                        calendarYear = calYear,
                        calendarDays = calendarDays,
                        calendarTitle = calendarTitle,
                        checkInLogs = checkInLogs,
                        stats = HabitStatUiModel(
                            totalCheckIns = doneLogs.size,
                            currentStreak = currentStreak,
                            longestStreak = longestStreak,
                            weeklyProgress = weeklyProgress,
                            totalDays = totalDays,
                            completionRate = completionRate
                        )
                    )
                }
            } catch (e: Exception) {
                updateState { copy(isLoading = false) }
            }
        }
    }

    // ============================================================
    // SELECT TAB
    // ============================================================
    private fun selectTab(tab: HabitDetailTab) {
        updateState { copy(selectedTab = tab) }
    }

    // ============================================================
    // CHECK-IN CAMERA FLOW
    // ============================================================

    /** Bước 1: Bấm nút → signal UI mở camera */
    private fun startCheckIn() {
        // Three locks guard the camera flow:
        // 1. isCompletedToday  — one check-in per day max; today's log is immutable.
        // 2. isJourneyComplete — the habit's full duration has been satisfied; no
        //    over-completion past target.
        // 3. isFailed          — the planned window has elapsed without completion;
        //    no further check-ins, but the history view stays browsable.
        if (currentState.isCompletedToday ||
            currentState.isJourneyComplete ||
            currentState.isFailed
        ) return
        sendEvent(HabitDetailEvent.LaunchCamera)
    }

    /** Bước 2: Camera trả ảnh về → chuyển sang CONFIRM */
    private fun photoCaptured(uri: Uri) {
        updateState {
            copy(
                checkInStep = CheckInStep.CONFIRM,
                checkInPhotoUri = uri,
                checkInNote = "",
                checkInTimestamp = System.currentTimeMillis()
            )
        }
    }

    /** Cập nhật ghi chú */
    private fun updateCheckInNote(note: String) {
        if (note.length <= 200) {
            updateState { copy(checkInNote = note) }
        }
    }

    /** Nhận location từ UI */
    private fun setLocation(lat: Double, lng: Double, name: String?) {
        updateState {
            copy(
                checkInLatitude = lat,
                checkInLongitude = lng,
                checkInLocationName = name
            )
        }
    }

    /** Bước 3: Xác nhận → lưu vào Room */
    private fun confirmCheckIn() {
        viewModelScope.launch {
            updateState { copy(isSavingCheckIn = true) }
            try {
                val state = currentState
                val today = getStartOfDay(Calendar.getInstance())
                val existingLog = habitLogRepository.getLogByDate(state.habitId, today)

                // Upload to Cloudinary first (passthrough when not configured) so the
                // value we persist is the durable cloud URL, not a transient local
                // FileProvider URI that becomes invalid after the cache rotates.
                val imageUriStr = state.checkInPhotoUri?.let { uri ->
                    imageUploadRepository.upload(
                        localUri = uri,
                        folder = ImageUploadRepository.Folder.HabitCheckIn
                    )
                }

                if (existingLog != null) {
                    // Cập nhật log hiện tại
                    habitLogRepository.updateLog(
                        existingLog.copy(
                            status = "DONE",
                            note = state.checkInNote.ifBlank { null },
                            image = imageUriStr,
                            created_at = state.checkInTimestamp,
                            latitude = state.checkInLatitude,
                            longitude = state.checkInLongitude
                        )
                    )
                } else {
                    // Tạo mới
                    habitLogRepository.addLog(
                        HabitLogEntity(
                            habit_id = state.habitId,
                            date = today,
                            status = "DONE",
                            note = state.checkInNote.ifBlank { null },
                            image = imageUriStr,
                            created_at = state.checkInTimestamp,
                            latitude = state.checkInLatitude,
                            longitude = state.checkInLongitude
                        )
                    )
                }

                updateState {
                    copy(
                        isSavingCheckIn = false,
                        checkInStep = CheckInStep.SUCCESS
                    )
                }
                sendEvent(HabitDetailEvent.CheckInSaved)
            } catch (e: Exception) {
                updateState { copy(isSavingCheckIn = false) }
                sendEvent(HabitDetailEvent.ShowMessage("Lỗi khi lưu check-in: ${e.message}"))
            }
        }
    }

    /** Hủy check-in flow → quay lại IDLE */
    private fun dismissCheckIn() {
        updateState {
            copy(
                checkInStep = CheckInStep.IDLE,
                checkInPhotoUri = null,
                checkInNote = "",
                checkInLatitude = null,
                checkInLongitude = null,
                checkInLocationName = null,
                checkInTimestamp = 0L
            )
        }
    }

    /** Sau SUCCESS, bấm "Tuyệt vời" → reload data & quay IDLE */
    fun onSuccessDismiss() {
        dismissCheckIn()
        loadHabit(currentState.habitId)
    }

    // ============================================================
    // CHANGE MONTH — Chuyển tháng calendar
    // ============================================================
    private fun changeMonth(offset: Int) {
        viewModelScope.launch {
            val newMonth = currentState.calendarMonth + offset
            val cal = Calendar.getInstance().apply {
                set(Calendar.YEAR, currentState.calendarYear)
                set(Calendar.MONTH, newMonth)
            }
            val month = cal.get(Calendar.MONTH)
            val year = cal.get(Calendar.YEAR)

            val allLogs = habitLogRepository.getLogs(currentState.habitId).first()
            val doneLogs = allLogs.filter { it.status == "DONE" }
            val calendarDays = buildCalendarDays(month, year, doneLogs)

            updateState {
                copy(
                    calendarMonth = month,
                    calendarYear = year,
                    calendarDays = calendarDays,
                    calendarTitle = formatMonthTitle(month, year)
                )
            }
        }
    }

    // ============================================================
    // HELPERS
    // ============================================================
    private fun getStartOfDay(calendar: Calendar): Long {
        return calendar.apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    private fun calculateCurrentStreak(doneLogs: List<HabitLogEntity>, todayMillis: Long): Int {
        if (doneLogs.isEmpty()) return 0
        val doneSet = doneLogs.map { it.date }.toSet()
        var streak = 0
        val cal = Calendar.getInstance()
        cal.timeInMillis = todayMillis

        // Nếu hôm nay chưa check-in, bắt đầu từ hôm qua
        if (todayMillis !in doneSet) {
            cal.add(Calendar.DAY_OF_YEAR, -1)
        }

        while (getStartOfDay(cal) in doneSet) {
            streak++
            cal.add(Calendar.DAY_OF_YEAR, -1)
        }
        return streak
    }

    private fun calculateLongestStreak(doneLogs: List<HabitLogEntity>): Int {
        if (doneLogs.isEmpty()) return 0
        val sortedDates = doneLogs.map { it.date }.distinct().sorted()
        var maxStreak = 1
        var current = 1
        val oneDay = 24 * 60 * 60 * 1000L

        for (i in 1 until sortedDates.size) {
            if (sortedDates[i] - sortedDates[i - 1] == oneDay) {
                current++
                maxStreak = maxOf(maxStreak, current)
            } else {
                current = 1
            }
        }
        return maxStreak
    }

    private fun calculateWeeklyProgress(doneLogs: List<HabitLogEntity>): Int {
        val cal = Calendar.getInstance()
        cal.set(Calendar.DAY_OF_WEEK, cal.firstDayOfWeek)
        val weekStart = getStartOfDay(cal)
        cal.add(Calendar.DAY_OF_WEEK, 7)
        val weekEnd = cal.timeInMillis

        return doneLogs.count { it.date in weekStart until weekEnd }
    }

    private fun buildCalendarDays(month: Int, year: Int, doneLogs: List<HabitLogEntity>): List<CalendarDayUiModel> {
        val doneSet = doneLogs.map { it.date }.toSet()
        val today = getStartOfDay(Calendar.getInstance())

        val cal = Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month)
            set(Calendar.DAY_OF_MONTH, 1)
        }

        // Ngày đầu tuần của tháng
        val firstDayOfWeek = cal.get(Calendar.DAY_OF_WEEK) // CN=1, T2=2...
        val daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)

        // Offset: bắt đầu từ thứ 2 (Calendar.MONDAY = 2)
        val startOffset = (firstDayOfWeek - Calendar.MONDAY + 7) % 7

        val result = mutableListOf<CalendarDayUiModel>()

        // Ngày tháng trước (fill padding)
        val prevCal = Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month)
            set(Calendar.DAY_OF_MONTH, 1)
            add(Calendar.DAY_OF_MONTH, -startOffset)
        }
        for (i in 0 until startOffset) {
            val dayMillis = getStartOfDay(prevCal)
            result.add(
                CalendarDayUiModel(
                    day = prevCal.get(Calendar.DAY_OF_MONTH),
                    dateMillis = dayMillis,
                    isCurrentMonth = false,
                    isToday = dayMillis == today,
                    isCheckedIn = dayMillis in doneSet
                )
            )
            prevCal.add(Calendar.DAY_OF_MONTH, 1)
        }

        // Ngày trong tháng hiện tại
        for (day in 1..daysInMonth) {
            cal.set(Calendar.DAY_OF_MONTH, day)
            val dayMillis = getStartOfDay(cal)
            result.add(
                CalendarDayUiModel(
                    day = day,
                    dateMillis = dayMillis,
                    isCurrentMonth = true,
                    isToday = dayMillis == today,
                    isCheckedIn = dayMillis in doneSet
                )
            )
        }

        // Fill đến 42 ô (6 tuần)
        val nextCal = Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month)
            set(Calendar.DAY_OF_MONTH, daysInMonth)
            add(Calendar.DAY_OF_MONTH, 1)
        }
        while (result.size < 42) {
            val dayMillis = getStartOfDay(nextCal)
            result.add(
                CalendarDayUiModel(
                    day = nextCal.get(Calendar.DAY_OF_MONTH),
                    dateMillis = dayMillis,
                    isCurrentMonth = false,
                    isToday = dayMillis == today,
                    isCheckedIn = dayMillis in doneSet
                )
            )
            nextCal.add(Calendar.DAY_OF_MONTH, 1)
        }

        return result
    }

    private fun formatMonthTitle(month: Int, year: Int): String {
        val cal = Calendar.getInstance().apply {
            set(Calendar.MONTH, month)
            set(Calendar.YEAR, year)
        }
        return SimpleDateFormat("'Tháng' M, yyyy", Locale.forLanguageTag("vi")).format(cal.time)
    }

    private fun formatLogDate(dateMillis: Long): String {
        val sdf = SimpleDateFormat("EEEE, dd/M/yyyy", Locale.forLanguageTag("vi"))
        return sdf.format(dateMillis).replaceFirstChar { it.uppercase() }
    }

    private fun formatLogTime(createdAtMillis: Long): String {
        return SimpleDateFormat("HH:mm", Locale.getDefault()).format(createdAtMillis)
    }
}
