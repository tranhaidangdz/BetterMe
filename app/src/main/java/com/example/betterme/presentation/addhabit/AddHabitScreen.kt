package com.example.betterme.presentation.addhabit

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.betterme.R
import com.example.betterme.presentation.addhabit.aicreation.HabitCreationAssistantBottomSheet
import com.example.betterme.presentation.addhabit.aicreation.HabitCreationAssistantEvent
import com.example.betterme.presentation.addhabit.aicreation.HabitCreationAssistantIntent
import com.example.betterme.presentation.addhabit.aicreation.HabitCreationAssistantViewModel
import com.example.betterme.presentation.addhabit.components.CategoryChipRow
import com.example.betterme.presentation.addhabit.components.HabitPreviewCard
import com.example.betterme.presentation.addhabit.components.SectionCard
import com.example.betterme.presentation.categorydetail.components.paletteFor
import com.example.betterme.presentation.components.view.BetterMeTopBar
import com.example.betterme.presentation.theme.BetterMeColors
import com.example.betterme.presentation.theme.BetterMeTokens
import com.example.betterme.presentation.theme.BetterMeTypography
import org.koin.androidx.compose.koinViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Add Habit — premium redesign.
 *
 * Layout (information architecture unchanged):
 *   - elegant top bar with back arrow
 *   - hero header with motivational subtitle
 *   - live preview card mirroring the Home row that will be created
 *   - section cards: name, description, category, schedule, reminder
 *   - sticky bottom CTA outside the scroll viewport
 *
 * ViewModel contract is untouched — same intents, same state, same submit flow.
 * The redesign is pure UI/UX on top of the existing reactive state.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddHabitScreen(
    onBackClick: () -> Unit = {},
    onHabitAdded: () -> Unit = {},
    viewModel: AddHabitViewModel = koinViewModel(),
    assistantVm: HabitCreationAssistantViewModel = koinViewModel()
) {
    val state by viewModel.viewState.collectAsState()
    val assistantState by assistantVm.viewState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    // Picker visibility flags — transient UI state, screen-local.
    var showStartDatePicker by remember { mutableStateOf(false) }
    var showEndDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }

    val titleFocusRequester = remember { FocusRequester() }
    // Auto-focus the name field once on screen entry so the keyboard is ready
    // immediately. Subsequent recompositions don't re-focus.
    LaunchedEffect(Unit) {
        runCatching { titleFocusRequester.requestFocus() }
    }

    LaunchedEffect(Unit) {
        viewModel.singleEvent.collect { event ->
            when (event) {
                AddHabitEvent.SaveSuccess -> {
                    onHabitAdded()
                    snackbarHostState.showSnackbar("Đã thêm thói quen — bắt đầu hành trình!")
                    onBackClick()
                }
                is AddHabitEvent.ShowError -> snackbarHostState.showSnackbar(event.message)
            }
        }
    }

    // Bridge from the assistant VM back to the AddHabit submit pipeline.
    // The sheet's "Vẫn tạo" button fires ConfirmedSave; we forward to the
    // existing AddHabitIntent.Submit. Form state is unchanged — the assistant
    // never mutates AddHabitState directly.
    LaunchedEffect(Unit) {
        assistantVm.singleEvent.collect { event ->
            when (event) {
                HabitCreationAssistantEvent.ConfirmedSave -> {
                    viewModel.processIntent(AddHabitIntent.Submit)
                }
            }
        }
    }

    // ===== Pickers =====
    if (showStartDatePicker) {
        val pickerState = rememberDatePickerState(initialSelectedDateMillis = state.startDate)
        DatePickerDialog(
            onDismissRequest = { showStartDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    pickerState.selectedDateMillis?.let {
                        viewModel.processIntent(AddHabitIntent.InputStartDate(it))
                    }
                    showStartDatePicker = false
                }) { Text("Xác nhận", color = BetterMeColors.Primary.Primary) }
            },
            dismissButton = {
                TextButton(onClick = { showStartDatePicker = false }) {
                    Text("Hủy", color = BetterMeColors.Text.TextTertiary)
                }
            }
        ) { DatePicker(state = pickerState) }
    }
    if (showEndDatePicker) {
        val pickerState = rememberDatePickerState(
            initialSelectedDateMillis = state.endDate
                ?: (state.startDate + 30L * 24 * 60 * 60 * 1000)
        )
        DatePickerDialog(
            onDismissRequest = { showEndDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    pickerState.selectedDateMillis?.let {
                        viewModel.processIntent(AddHabitIntent.InputEndDate(it))
                    }
                    showEndDatePicker = false
                }) { Text("Xác nhận", color = BetterMeColors.Primary.Primary) }
            },
            dismissButton = {
                TextButton(onClick = { showEndDatePicker = false }) {
                    Text("Hủy", color = BetterMeColors.Text.TextTertiary)
                }
            }
        ) { DatePicker(state = pickerState) }
    }
    if (showTimePicker) {
        val parts = state.reminderTime.split(":")
        val pickerState = rememberTimePickerState(
            initialHour = parts.getOrNull(0)?.toIntOrNull() ?: 7,
            initialMinute = parts.getOrNull(1)?.toIntOrNull() ?: 0,
            is24Hour = true
        )
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            title = {
                Text(
                    text = "Đặt giờ nhắc",
                    style = BetterMeTypography.Title.Small.Bold,
                    color = BetterMeColors.Text.TextPrimary,
                    fontWeight = FontWeight.Bold
                )
            },
            text = { TimePicker(state = pickerState) },
            confirmButton = {
                TextButton(onClick = {
                    val t = String.format("%02d:%02d", pickerState.hour, pickerState.minute)
                    viewModel.processIntent(AddHabitIntent.InputReminderTime(t))
                    showTimePicker = false
                }) { Text("Lưu", color = BetterMeColors.Primary.Primary) }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) {
                    Text("Hủy", color = BetterMeColors.Text.TextTertiary)
                }
            },
            containerColor = Color.White,
            shape = RoundedCornerShape(BetterMeTokens.CardRadius.Hero)
        )
    }

    // Derived UI values. `remember(keys)` keeps recomposition stable.
    val targetDays = remember(state.startDate, state.endDate) {
        val end = state.endDate ?: return@remember null
        val days = ((end - state.startDate) / (24L * 60 * 60 * 1000)).toInt() + 1
        days.takeIf { it > 0 }
    }
    val accent = remember(state.selectedCategoryId) {
        state.selectedCategoryId?.let { paletteFor(it).accent }
            ?: BetterMeColors.Primary.Primary
    }
    val canSubmit = !state.isLoading && state.title.isNotBlank()

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        containerColor = BetterMeColors.BackGround.BackgroundSecondary
    ) { inner ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(inner)
                .background(BetterMeColors.BackGround.BackgroundSecondary)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .verticalScroll(rememberScrollState())
                    .imePadding()
            ) {
                // Shared app top bar — kept consistent with Tasks / Habit Group / Habit
                // Detail / Notification Center. statusBarsPadding is already applied
                // to the outer Column, so the top bar lays inside the safe area.
                BetterMeTopBar(
                    leadingIconRes = R.drawable.ic_arrow_left,
                    title = "Tạo thói quen mới",
                    onLeadingClick = onBackClick
                )

                Spacer(Modifier.height(2.dp))

                // Motivational subtitle on its own line, padded to content edge so the
                // text starts in line with the section cards below.
                Text(
                    text = "Một thói quen nhỏ hôm nay là phiên bản tốt hơn của bạn ngày mai.",
                    style = BetterMeTypography.Body.Medium,
                    color = BetterMeColors.Text.TextTertiary,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
                )

                Spacer(Modifier.height(14.dp))

                HabitPreviewCard(
                    title = state.title,
                    categoryName = state.selectedCategoryName,
                    categoryIcon = state.categories
                        .firstOrNull { it.id == state.selectedCategoryId }?.icon
                        .orEmpty(),
                    accent = accent,
                    reminderTime = state.reminderTime,
                    targetDays = targetDays,
                    modifier = Modifier.padding(horizontal = 20.dp)
                )

                Spacer(Modifier.height(18.dp))

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // ===== Name =====
                    SectionCard(
                        icon = "✍️",
                        title = "Tên thói quen",
                        helper = "Đặt tên cụ thể, dễ nhớ"
                    ) {
                        OutlinedTextField(
                            value = state.title,
                            onValueChange = {
                                viewModel.processIntent(AddHabitIntent.InputTitle(it))
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .focusRequester(titleFocusRequester),
                            placeholder = {
                                Text(
                                    text = "VD: Đi bộ 10.000 bước mỗi ngày",
                                    style = BetterMeTypography.Body.Medium,
                                    color = BetterMeColors.Text.TextTertiary
                                )
                            },
                            singleLine = true,
                            isError = state.titleError != null,
                            shape = RoundedCornerShape(BetterMeTokens.CardRadius.Body),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = accent,
                                unfocusedBorderColor = BetterMeColors.Border.BorderLight,
                                errorBorderColor = BetterMeColors.Red,
                                cursorColor = accent
                            )
                        )
                        if (state.titleError != null) {
                            Spacer(Modifier.height(6.dp))
                            Text(
                                text = state.titleError!!,
                                style = BetterMeTypography.Body.Small.Medium,
                                color = BetterMeColors.Red
                            )
                        }
                    }

                    // ===== Description =====
                    SectionCard(
                        icon = "📝",
                        title = "Mô tả",
                        helper = "Tùy chọn — vì sao thói quen này quan trọng với bạn?"
                    ) {
                        OutlinedTextField(
                            value = state.description,
                            onValueChange = {
                                viewModel.processIntent(AddHabitIntent.InputDescription(it))
                            },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = {
                                Text(
                                    text = "VD: Cải thiện sức khoẻ tim mạch và năng lượng cả ngày.",
                                    style = BetterMeTypography.Body.Medium,
                                    color = BetterMeColors.Text.TextTertiary
                                )
                            },
                            minLines = 3,
                            maxLines = 5,
                            shape = RoundedCornerShape(BetterMeTokens.CardRadius.Body),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = accent,
                                unfocusedBorderColor = BetterMeColors.Border.BorderLight,
                                cursorColor = accent
                            )
                        )
                    }

                    // ===== Category =====
                    SectionCard(
                        icon = "🗂️",
                        title = "Nhóm thói quen",
                        helper = "Mỗi nhóm có màu sắc và nhịp riêng"
                    ) {
                        CategoryChipRow(
                            categories = state.categories,
                            selectedCategoryId = state.selectedCategoryId,
                            accent = accent,
                            onSelect = { id, name ->
                                viewModel.processIntent(AddHabitIntent.SelectCategory(id, name))
                            }
                        )
                    }

                    // ===== Schedule =====
                    SectionCard(
                        icon = "📅",
                        title = "Lịch trình",
                        helper = "Khoảng thời gian bạn cam kết"
                    ) {
                        ScheduleField(
                            emoji = "🚀",
                            label = "Ngày bắt đầu",
                            value = formatDate(state.startDate),
                            accent = accent,
                            onClick = { showStartDatePicker = true }
                        )
                        Spacer(Modifier.height(10.dp))
                        ScheduleField(
                            emoji = "🎯",
                            label = "Ngày hoàn thành",
                            value = state.endDate?.let { formatDate(it) }
                                ?: "Chưa chọn ngày kết thúc",
                            accent = accent,
                            placeholderTone = state.endDate == null,
                            onClick = { showEndDatePicker = true }
                        )
                    }

                    // ===== Reminder =====
                    SectionCard(
                        icon = "⏰",
                        title = "Nhắc nhở",
                        helper = if (state.reminderTime.isBlank()) "Không nhắc"
                        else "Mỗi ngày lúc ${state.reminderTime} — thông báo tự động"
                    ) {
                        ScheduleField(
                            emoji = "🔔",
                            label = "Giờ nhắc hằng ngày",
                            value = state.reminderTime.ifBlank { "Bấm để chọn giờ" },
                            accent = accent,
                            placeholderTone = state.reminderTime.isBlank(),
                            onClick = { showTimePicker = true }
                        )
                    }

                    // Bottom-bar breathing room so the last card never sits flush against
                    // the sticky CTA shadow.
                    Spacer(Modifier.height(100.dp))
                }
            }

            // ===== Sticky CTA =====
            // Save first triggers the AI Creation Assistant for a quick pre-save
            // review. The assistant emits ConfirmedSave on "Vẫn tạo" — handled
            // by the LaunchedEffect above which dispatches the real Submit.
            // The assistant never blocks: an Error state still surfaces "Vẫn
            // tạo" so the user always reaches the original submit path.
            StickyCta(
                accent = accent,
                enabled = canSubmit,
                isLoading = state.isLoading,
                onClick = {
                    assistantVm.processIntent(
                        HabitCreationAssistantIntent.Analyze(
                            title = state.title.trim(),
                            categoryId = state.selectedCategoryId,
                            reminderTime = state.reminderTime
                        )
                    )
                },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(20.dp)
            )

            // AI Creation Assistant bottom sheet — modal, mounted at screen
            // scope so it overlays the form and any open pickers cleanly.
            HabitCreationAssistantBottomSheet(
                state = assistantState,
                onIntent = assistantVm::processIntent
            )
        }
    }
}

// =====================================================================
// SUB-COMPOSABLES
// =====================================================================

@Composable
private fun ScheduleField(
    emoji: String,
    label: String,
    value: String,
    accent: Color,
    placeholderTone: Boolean = false,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(BetterMeTokens.CardRadius.Body))
            .background(BetterMeColors.BackGround.BackgroundSecondary)
            .border(
                width = 1.dp,
                color = BetterMeColors.Border.BorderLight,
                shape = RoundedCornerShape(BetterMeTokens.CardRadius.Body)
            )
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(accent.copy(alpha = BetterMeTokens.AccentAlpha.Soft)),
            contentAlignment = Alignment.Center
        ) {
            Text(text = emoji, fontSize = 14.sp)
        }
        Spacer(Modifier.size(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = BetterMeTypography.Body.Small.Medium,
                color = BetterMeColors.Text.TextTertiary
            )
            Text(
                text = value,
                style = BetterMeTypography.Body.Medium,
                color = if (placeholderTone) BetterMeColors.Text.TextTertiary
                else BetterMeColors.Text.TextPrimary,
                fontWeight = FontWeight.SemiBold
            )
        }
        Icon(
            imageVector = Icons.Default.KeyboardArrowDown,
            contentDescription = null,
            tint = BetterMeColors.Text.TextTertiary
        )
    }
}

@Composable
private fun StickyCta(
    accent: Color,
    enabled: Boolean,
    isLoading: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val containerColor = if (enabled) accent else accent.copy(alpha = 0.45f)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
            .shadow(
                elevation = 10.dp,
                shape = RoundedCornerShape(BetterMeTokens.CardRadius.Body),
                ambientColor = accent.copy(alpha = 0.32f),
                spotColor = accent.copy(alpha = 0.40f)
            )
            .clip(RoundedCornerShape(BetterMeTokens.CardRadius.Body))
            .background(
                Brush.horizontalGradient(
                    colors = listOf(
                        containerColor,
                        containerColor.copy(alpha = 0.85f)
                    )
                )
            )
            .clickable(enabled = enabled) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(24.dp),
                color = Color.White,
                strokeWidth = 2.dp
            )
        } else {
            Text(
                text = "🚀  Bắt đầu hành trình",
                style = BetterMeTypography.Title.Small.Bold,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

private fun formatDate(ms: Long): String {
    val sdf = SimpleDateFormat("dd/MM/yyyy", Locale("vi"))
    return sdf.format(Date(ms))
}
