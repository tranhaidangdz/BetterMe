package com.example.betterme.presentation.habitdetail

import android.net.Uri
import com.example.betterme.base.MviIntent
import com.example.betterme.base.MviSingleEvent
import com.example.betterme.base.MviViewState

// ============================================================
// ENUM — tab bên dưới
// ============================================================
enum class HabitDetailTab(val label: String) {
    HISTORY("Lịch sử check in"),
    AI_SUGGEST("Nhắc nhở"),
    STATS("Thống kê")
}

// ============================================================
// ENUM — bước check-in
// ============================================================
enum class CheckInStep {
    IDLE,       // Chưa bắt đầu
    CONFIRM,    // Đang xem ảnh + ghi chú trước khi xác nhận
    SUCCESS     // Đã check-in thành công
}

// ============================================================
// UI MODELS
// ============================================================
data class CheckInLogUiModel(
    val logId: Int,
    val dateFormatted: String,   // "Thứ Ba, 28/1/2026"
    val timeFormatted: String,   // "21:30"
    val note: String?,
    val imageUri: String?,
    val status: String           // "DONE" or "SKIPPED"
)

data class CalendarDayUiModel(
    val day: Int,
    val dateMillis: Long,
    val isCurrentMonth: Boolean,
    val isToday: Boolean,
    val isCheckedIn: Boolean
)

data class HabitStatUiModel(
    val totalCheckIns: Int = 0,
    val currentStreak: Int = 0,
    val longestStreak: Int = 0,
    val weeklyProgress: Int = 0,    // x/7
    val totalDays: Int = 0,
    val completionRate: Int = 0     // %
)

// ============================================================
// STATE
// ============================================================
data class HabitDetailState(
    val isLoading: Boolean = false,
    val habitId: Int = -1,
    val habitTitle: String = "",
    val habitDescription: String? = null,
    val categoryName: String = "",
    val categoryIcon: String = "📌",
    val reminderTime: String? = null,
    val isCompletedToday: Boolean = false,
    /**
     * True when DONE log count ≥ planned duration. Once set, the camera/check-in flow is
     * locked (no over-completion past target). Open-ended habits (no `end_date`) never
     * flip this to true and stay perpetually check-inable.
     */
    val isJourneyComplete: Boolean = false,

    // Streak info
    val currentStreak: Int = 0,
    val longestStreak: Int = 0,
    val weeklyProgress: Int = 0,
    val weeklyTotal: Int = 7,

    // Tab
    val selectedTab: HabitDetailTab = HabitDetailTab.HISTORY,

    // Calendar
    val calendarMonth: Int = 0,      // 0-indexed
    val calendarYear: Int = 2026,
    val calendarDays: List<CalendarDayUiModel> = emptyList(),
    val calendarTitle: String = "",

    // History tab
    val checkInLogs: List<CheckInLogUiModel> = emptyList(),

    // Stats tab
    val stats: HabitStatUiModel = HabitStatUiModel(),

    // ===== CHECK-IN FLOW =====
    val checkInStep: CheckInStep = CheckInStep.IDLE,
    val checkInPhotoUri: Uri? = null,
    val checkInNote: String = "",
    val checkInLatitude: Double? = null,
    val checkInLongitude: Double? = null,
    val checkInLocationName: String? = null,  // Tên vị trí (reverse geocode)
    val checkInTimestamp: Long = 0L,
    val isSavingCheckIn: Boolean = false
) : MviViewState

// ============================================================
// INTENT
// ============================================================
sealed class HabitDetailIntent : MviIntent {
    data class LoadHabit(val habitId: Int) : HabitDetailIntent()
    data class SelectTab(val tab: HabitDetailTab) : HabitDetailIntent()
    data object PreviousMonth : HabitDetailIntent()
    data object NextMonth : HabitDetailIntent()

    // Check-in camera flow
    data object StartCheckIn : HabitDetailIntent()          // Bấm nút "Check in" → mở camera
    data class PhotoCaptured(val uri: Uri) : HabitDetailIntent()  // Camera trả ảnh về
    data class UpdateCheckInNote(val note: String) : HabitDetailIntent()
    data class SetLocation(val lat: Double, val lng: Double, val name: String?) : HabitDetailIntent()
    data object ConfirmCheckIn : HabitDetailIntent()        // Xác nhận → lưu vào DB
    data object DismissCheckIn : HabitDetailIntent()        // Hủy check-in flow
    // Note: UndoCheckIn was intentionally removed. Check-ins are immutable once
    // committed — no toggle-back, no accidental undo. Daily logs are permanent.
}

// ============================================================
// EVENT
// ============================================================
sealed class HabitDetailEvent : MviSingleEvent {
    data class ShowMessage(val message: String) : HabitDetailEvent()
    data object LaunchCamera : HabitDetailEvent()           // Signal UI to open camera
    data object CheckInSaved : HabitDetailEvent()           // Lưu thành công → hiện success
}
