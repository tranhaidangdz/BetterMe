package com.example.betterme.presentation.habitdetail

import android.Manifest
import android.content.Context
import android.location.Geocoder
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.example.betterme.R
import com.example.betterme.presentation.components.view.BetterMeTopBar
import com.example.betterme.presentation.habitdetail.components.*
import com.example.betterme.presentation.theme.BetterMeColors
import com.example.betterme.presentation.theme.BetterMeTypography
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.flow.collectLatest
import org.koin.androidx.compose.koinViewModel
import java.io.File
import java.util.Locale

@Composable
fun HabitDetailScreen(
    habitId: Int,
    onBackClick: () -> Unit,
    viewModel: HabitDetailViewModel = koinViewModel()
) {
    val state by viewModel.viewState.collectAsState()
    val context = LocalContext.current

    // ===== CAMERA URI (FileProvider) =====
    var photoUri by remember { mutableStateOf<android.net.Uri?>(null) }

    // ===== CAMERA LAUNCHER =====
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && photoUri != null) {
            viewModel.processIntent(HabitDetailIntent.PhotoCaptured(photoUri!!))
        }
    }

    // ===== PERMISSION LAUNCHER (Camera + Location) =====
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val cameraGranted = permissions[Manifest.permission.CAMERA] == true
        val locationGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true
                || permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true

        if (cameraGranted) {
            // Lấy GPS trước (nếu được cấp quyền)
            if (locationGranted) {
                fetchLocation(context, viewModel)
            }

            // Tạo file tạm cho camera
            val uri = createImageUri(context)
            photoUri = uri
            cameraLauncher.launch(uri)
        } else {
            Toast.makeText(context, "Cần quyền camera để check-in", Toast.LENGTH_SHORT).show()
        }
    }

    // ===== LISTEN EVENTS =====
    LaunchedEffect(habitId) {
        viewModel.processIntent(HabitDetailIntent.LoadHabit(habitId))
    }

    LaunchedEffect(Unit) {
        viewModel.singleEvent.collectLatest { event ->
            when (event) {
                is HabitDetailEvent.ShowMessage ->
                    Toast.makeText(context, event.message, Toast.LENGTH_SHORT).show()

                is HabitDetailEvent.LaunchCamera -> {
                    // Request permissions then launch camera
                    permissionLauncher.launch(
                        arrayOf(
                            Manifest.permission.CAMERA,
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                        )
                    )
                }

                is HabitDetailEvent.CheckInSaved -> {
                    // State đã chuyển sang SUCCESS — UI sẽ tự render
                }
            }
        }
    }

    // ===== MAIN CONTENT =====
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BetterMeColors.BackGround.BackgroundSecondary)
    ) {
        HabitDetailMainContent(
            state = state,
            onBackClick = onBackClick,
            onIntent = viewModel::processIntent
        )

        // ===== CHECK-IN CONFIRM OVERLAY =====
        if (state.checkInStep == CheckInStep.CONFIRM) {
            CheckInConfirmSheet(
                state = state,
                onNoteChanged = { viewModel.processIntent(HabitDetailIntent.UpdateCheckInNote(it)) },
                onConfirm = { viewModel.processIntent(HabitDetailIntent.ConfirmCheckIn) },
                onDismiss = { viewModel.processIntent(HabitDetailIntent.DismissCheckIn) }
            )
        }

        // ===== CHECK-IN SUCCESS OVERLAY =====
        if (state.checkInStep == CheckInStep.SUCCESS) {
            CheckInSuccessSheet(
                habitTitle = state.habitTitle,
                currentStreak = state.currentStreak + 1, // +1 vì vừa check-in
                onDismiss = { viewModel.onSuccessDismiss() },
                onViewHistory = {
                    viewModel.onSuccessDismiss()
                    viewModel.processIntent(HabitDetailIntent.SelectTab(HabitDetailTab.HISTORY))
                }
            )
        }

        // Loading overlay
        if (state.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(BetterMeColors.Black.copy(alpha = 0.08f)),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(40.dp),
                    color = BetterMeColors.Primary.Primary,
                    strokeWidth = 3.dp
                )
            }
        }
    }
}

// ============================================================
// MAIN CONTENT — LazyColumn + sticky button
// ============================================================
@Composable
private fun HabitDetailMainContent(
    state: HabitDetailState,
    onBackClick: () -> Unit,
    onIntent: (HabitDetailIntent) -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding(),
            contentPadding = PaddingValues(bottom = 100.dp)
        ) {
            // ===== TOP BAR =====
            item(key = "topbar") {
                BetterMeTopBar(
                    leadingIconRes = R.drawable.ic_arrow_left,
                    title = "Chi tiết thói quen",
                    onLeadingClick = onBackClick
                )
            }

            // ===== HABIT INFO CARD =====
            item(key = "info") {
                Spacer(modifier = Modifier.height(8.dp))
                HabitInfoCard(
                    title = state.habitTitle,
                    categoryName = state.categoryName,
                    categoryIcon = state.categoryIcon,
                    isCompletedToday = state.isCompletedToday,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }

            // ===== STREAK CARD =====
            item(key = "streak") {
                Spacer(modifier = Modifier.height(10.dp))
                StreakCard(
                    currentStreak = state.currentStreak,
                    longestStreak = state.longestStreak,
                    weeklyProgress = state.weeklyProgress,
                    weeklyTotal = state.weeklyTotal,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }

            // ===== MINI TABS =====
            item(key = "tabs") {
                Spacer(modifier = Modifier.height(14.dp))
                MiniTabBar(
                    selectedTab = state.selectedTab,
                    onSelectTab = { onIntent(HabitDetailIntent.SelectTab(it)) },
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
                Spacer(modifier = Modifier.height(10.dp))
            }

            // ===== TAB CONTENT =====
            when (state.selectedTab) {
                HabitDetailTab.HISTORY -> {
                    item(key = "calendar") {
                        CheckInCalendar(
                            title = state.calendarTitle,
                            days = state.calendarDays,
                            onPreviousMonth = { onIntent(HabitDetailIntent.PreviousMonth) },
                            onNextMonth = { onIntent(HabitDetailIntent.NextMonth) },
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }

                    if (state.checkInLogs.isEmpty()) {
                        item(key = "empty_logs") {
                            Spacer(modifier = Modifier.height(8.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(BetterMeColors.White)
                                    .padding(vertical = 32.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "📋 Chưa có lịch sử check-in",
                                    style = BetterMeTypography.Body.Medium,
                                    color = BetterMeColors.Text.TextTertiary
                                )
                            }
                        }
                    } else {
                        items(
                            items = state.checkInLogs,
                            key = { it.logId }
                        ) { log ->
                            Spacer(modifier = Modifier.height(8.dp))
                            CheckInLogCard(
                                log = log,
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )
                        }
                    }
                }

                HabitDetailTab.AI_SUGGEST -> {
                    item(key = "ai_suggest") {
                        AiSuggestTabContent(
                            habitTitle = state.habitTitle,
                            categoryName = state.categoryName,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }
                }

                HabitDetailTab.STATS -> {
                    item(key = "stats") {
                        StatsTabContent(
                            stats = state.stats,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }
                }
            }
        }

        // ===== STICKY BOTTOM BUTTON =====
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .shadow(
                    elevation = 8.dp,
                    shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
                    clip = false
                )
                .background(
                    color = BetterMeColors.White,
                    shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
                )
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 14.dp)
        ) {
            if (state.isCompletedToday) {
                // Đã check-in → nút undo
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Undo button
                    Button(
                        onClick = { onIntent(HabitDetailIntent.UndoCheckIn) },
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = BetterMeColors.Red.copy(alpha = 0.1f)
                        ),
                        elevation = ButtonDefaults.buttonElevation(0.dp)
                    ) {
                        Text(
                            text = "Bỏ check-in",
                            style = BetterMeTypography.Body.Medium.copy(fontWeight = FontWeight.SemiBold),
                            color = BetterMeColors.Red
                        )
                    }

                    // Status button (disabled)
                    Button(
                        onClick = { },
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = BetterMeColors.Green
                        ),
                        enabled = false,
                        elevation = ButtonDefaults.buttonElevation(0.dp)
                    ) {
                        Text(
                            text = "✓ Đã hoàn thành",
                            style = BetterMeTypography.Title.Small.Bold,
                            color = BetterMeColors.White
                        )
                    }
                }
            } else {
                // Chưa check-in → nút mở camera
                Button(
                    onClick = { onIntent(HabitDetailIntent.StartCheckIn) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = BetterMeColors.Primary.Primary
                    ),
                    elevation = ButtonDefaults.buttonElevation(
                        defaultElevation = 2.dp,
                        pressedElevation = 0.dp
                    )
                ) {
                    Text(
                        text = "📸 Check in bằng camera",
                        style = BetterMeTypography.Title.Small.Bold,
                        color = BetterMeColors.White
                    )
                }
            }
        }
    }
}

// ============================================================
// MINI TAB BAR — Lịch sử | Nhắc nhở | Thống kê
// ============================================================
@Composable
private fun MiniTabBar(
    selectedTab: HabitDetailTab,
    onSelectTab: (HabitDetailTab) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(BetterMeColors.White)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        HabitDetailTab.entries.forEach { tab ->
            val isSelected = tab == selectedTab
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(
                        if (isSelected) BetterMeColors.Primary.Primary.copy(alpha = 0.1f)
                        else BetterMeColors.White
                    )
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { onSelectTab(tab) }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = tab.label,
                    style = BetterMeTypography.Body.Small.Medium.copy(
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                    ),
                    color = if (isSelected) BetterMeColors.Primary.Primary
                    else BetterMeColors.Text.TextTertiary,
                    maxLines = 1
                )
            }
        }
    }
}

// ============================================================
// HELPERS
// ============================================================

/** Tạo URI cho camera output qua FileProvider */
private fun createImageUri(context: Context): android.net.Uri {
    val imageDir = File(context.cacheDir, "checkin_images").apply { mkdirs() }
    val imageFile = File(imageDir, "checkin_${System.currentTimeMillis()}.jpg")
    return FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        imageFile
    )
}

/** Lấy GPS location hiện tại (last known hoặc fresh) */
@Suppress("MissingPermission")
private fun fetchLocation(context: Context, viewModel: HabitDetailViewModel) {
    try {
        val fusedClient = LocationServices.getFusedLocationProviderClient(context)
        val cancellationToken = CancellationTokenSource()

        fusedClient.getCurrentLocation(
            Priority.PRIORITY_HIGH_ACCURACY,
            cancellationToken.token
        ).addOnSuccessListener { location ->
            if (location != null) {
                // Reverse geocode
                val locationName = try {
                    val geocoder = Geocoder(context, Locale("vi"))
                    @Suppress("DEPRECATION")
                    val addresses = geocoder.getFromLocation(location.latitude, location.longitude, 1)
                    addresses?.firstOrNull()?.let { addr ->
                        buildString {
                            addr.thoroughfare?.let { append(it) }
                            addr.subAdminArea?.let {
                                if (isNotEmpty()) append(", ")
                                append(it)
                            }
                            addr.adminArea?.let {
                                if (isNotEmpty()) append(", ")
                                append(it)
                            }
                        }.ifBlank { null }
                    }
                } catch (_: Exception) { null }

                viewModel.processIntent(
                    HabitDetailIntent.SetLocation(
                        lat = location.latitude,
                        lng = location.longitude,
                        name = locationName
                    )
                )
            }
        }
    } catch (_: Exception) { }
}
