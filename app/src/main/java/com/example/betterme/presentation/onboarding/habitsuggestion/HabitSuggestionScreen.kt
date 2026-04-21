package com.example.betterme.presentation.onboarding.habitsuggestion

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.betterme.presentation.components.button.BetterMeButton
import com.example.betterme.presentation.theme.BetterMeColors
import com.example.betterme.presentation.theme.BetterMeShapes
import com.example.betterme.presentation.theme.BetterMeTypography
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// =========================
// SCREEN
// =========================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HabitSuggestionScreen(
    selectedCategoryIds: List<Int>,
    navigateToMain: () -> Unit,
    viewModel: HabitSuggestionViewModel = koinViewModel(
        parameters = { parametersOf(selectedCategoryIds) }
    )
) {
    val state by viewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var showStartDatePicker by remember { mutableStateOf(false) }
    var showEndDatePicker by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.event.collect { event ->
            when (event) {
                HabitSuggestionEvent.NavigateToMain -> navigateToMain()
                is HabitSuggestionEvent.ShowError -> snackbarHostState.showSnackbar(event.message)
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = BetterMeColors.BackGround.BackgroundPrimary
    ) { padding ->
        HabitSuggestionContent(
            modifier = Modifier.padding(padding),
            state = state,
            repeatOptions = viewModel.repeatOptions,
            onIntent = viewModel::onIntent
        )
    }

    // === HABIT SETTINGS DIALOG ===
    val editingHabit = state.editingHabit
    if (showStartDatePicker && editingHabit != null) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = editingHabit.startDate
        )
        DatePickerDialog(
            onDismissRequest = { showStartDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let {
                            viewModel.onIntent(HabitSuggestionIntent.SetStartDate(editingHabit.id, it))
                        }
                        showStartDatePicker = false
                    }
                ) {
                    Text("Xác nhận", color = BetterMeColors.Primary.Primary)
                }
            },
            dismissButton = {
                TextButton(onClick = { showStartDatePicker = false }) {
                    Text("Hủy", color = BetterMeColors.Text.TextTertiary)
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (showEndDatePicker && editingHabit != null) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = editingHabit.endDate ?: editingHabit.startDate
        )
        DatePickerDialog(
            onDismissRequest = { showEndDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.onIntent(
                            HabitSuggestionIntent.SetEndDate(editingHabit.id, datePickerState.selectedDateMillis)
                        )
                        showEndDatePicker = false
                    }
                ) {
                    Text("Xác nhận", color = BetterMeColors.Primary.Primary)
                }
            },
            dismissButton = {
                TextButton(onClick = { showEndDatePicker = false }) {
                    Text("Hủy", color = BetterMeColors.Text.TextTertiary)
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (editingHabit != null && !state.showReminderPicker && !state.showRepeatPicker) {
        HabitSettingsDialog(
            habit = editingHabit,
            repeatOptions = viewModel.repeatOptions,
            onSetReminder = { viewModel.onIntent(HabitSuggestionIntent.ShowReminderPicker(editingHabit.id)) },
            onSetRepeat = { viewModel.onIntent(HabitSuggestionIntent.ShowRepeatPicker(editingHabit.id)) },
            onSetStartDate = { showStartDatePicker = true },
            onSetEndDate = { showEndDatePicker = true },
            onConfirm = { viewModel.onIntent(HabitSuggestionIntent.ConfirmHabitSettings(editingHabit.id)) },
            onDismiss = {
                showStartDatePicker = false
                showEndDatePicker = false
                viewModel.onIntent(HabitSuggestionIntent.DismissHabitSettings(editingHabit.id))
            }
        )
    }

    // === REPEAT PICKER DIALOG ===
    if (state.showRepeatPicker && editingHabit != null) {
        RepeatPickerDialog(
            options = viewModel.repeatOptions,
            selected = editingHabit.repeatLabel,
            onSelect = { viewModel.onIntent(HabitSuggestionIntent.SetRepeat(editingHabit.id, it)) },
            onDismiss = { viewModel.onIntent(HabitSuggestionIntent.DismissRepeatPicker) }
        )
    }

    // === REMINDER TIME PICKER DIALOG ===
    if (state.showReminderPicker && editingHabit != null) {
        ReminderTimePickerDialog(
            currentHour = editingHabit.reminderHour,
            currentMinute = editingHabit.reminderMinute,
            onConfirm = { hour, minute ->
                viewModel.onIntent(HabitSuggestionIntent.SetReminderTime(editingHabit.id, hour, minute))
            },
            onDismiss = { viewModel.onIntent(HabitSuggestionIntent.DismissReminderPicker) }
        )
    }
}

// =========================
// CONTENT
// =========================
@Composable
fun HabitSuggestionContent(
    modifier: Modifier = Modifier,
    state: HabitSuggestionState,
    repeatOptions: List<String>,
    onIntent: (HabitSuggestionIntent) -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(BetterMeColors.BackGround.BackgroundPrimary)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 20.dp)
    ) {
        Spacer(modifier = Modifier.height(32.dp))

        // ===== HEADER =====
        Text(
            text = "AI gợi ý thói quen cho bạn ✨",
            style = BetterMeTypography.Headline.Small.Bold,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Chọn thói quen bạn muốn rèn luyện. Mỗi thói quen sẽ có giờ nhắc và lịch lặp riêng.",
            style = BetterMeTypography.Body.Medium,
            color = BetterMeColors.Text.TextTertiary,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(24.dp))

        // ===== HABIT LIST (LazyColumn) =====
        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            state.categoryHabits.forEach { category ->
                // Category Header
                item(key = "header_${category.categoryName}") {
                    CategoryHeader(
                        icon = category.categoryIcon,
                        name = category.categoryName,
                        selectedCount = category.habits.count { it.isChecked },
                        totalCount = category.habits.size
                    )
                }

                // Habit items
                items(
                    items = category.habits,
                    key = { it.id }
                ) { habit ->
                    HabitCheckItem(
                        habit = habit,
                        onToggle = { onIntent(HabitSuggestionIntent.ToggleHabit(habit.id)) }
                    )
                }

                // Spacer between categories
                item(key = "spacer_${category.categoryName}") {
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // ===== SELECTED COUNT =====
        Text(
            text = "Đã chọn ${state.selectedHabitCount} thói quen",
            style = BetterMeTypography.Body.Medium,
            color = BetterMeColors.Text.TextTertiary,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )

        Spacer(modifier = Modifier.height(12.dp))

        // ===== BUTTON =====
        BetterMeButton(
            onClick = { onIntent(HabitSuggestionIntent.StartJourney) },
            text = "Bắt đầu hành trình",
            modifier = Modifier.heightIn(min = 56.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))
    }
}

// =========================
// CATEGORY HEADER
// =========================
@Composable
fun CategoryHeader(
    icon: String,
    name: String,
    selectedCount: Int,
    totalCount: Int
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = icon,
            style = BetterMeTypography.Title.Medium.Medium
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = name,
            style = BetterMeTypography.Title.Small.Bold,
            color = BetterMeColors.Text.TextPrimary,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = "$selectedCount/$totalCount",
            style = BetterMeTypography.Body.Small.Medium,
            color = BetterMeColors.Text.TextTertiary
        )
    }
}

// =========================
// HABIT CHECK ITEM
// =========================
@Composable
fun HabitCheckItem(
    habit: SuggestedHabitUiModel,
    onToggle: () -> Unit
) {
    val backgroundColor by animateColorAsState(
        targetValue = if (habit.isChecked)
            BetterMeColors.Primary.PrimaryBackground
        else
            BetterMeColors.BackGround.BackgroundSecondary,
        animationSpec = tween(200),
        label = ""
    )

    val borderColor by animateColorAsState(
        targetValue = if (habit.isChecked)
            BetterMeColors.Primary.Primary
        else
            BetterMeColors.Border.BorderLight,
        animationSpec = tween(200),
        label = ""
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(BetterMeShapes.medium)
            .background(backgroundColor)
            .border(1.dp, borderColor, BetterMeShapes.medium)
            .clickable { onToggle() }
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            AnimatedCheckbox(isChecked = habit.isChecked)
            Text(
                text = habit.title,
                style = BetterMeTypography.Body.Medium,
                color = BetterMeColors.Text.TextPrimary,
                modifier = Modifier.weight(1f)
            )
        }

        // Hiện thông tin reminder nếu đã tích
        if (habit.isChecked) {
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.padding(start = 36.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "⏰ ${habit.reminderTimeFormatted}",
                    style = BetterMeTypography.Body.Small.Medium,
                    color = BetterMeColors.Primary.Primary
                )
                Text(
                    text = "📊 ${habit.repeatLabel}",
                    style = BetterMeTypography.Body.Small.Medium,
                    color = BetterMeColors.Primary.Primary
                )
                Text(
                    text = "📅 ${formatDate(habit.startDate)}",
                    style = BetterMeTypography.Body.Small.Medium,
                    color = BetterMeColors.Primary.Primary
                )
            }

            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "🏁 ${habit.endDate?.let(::formatDate) ?: "Chưa chọn ngày kết thúc"}",
                style = BetterMeTypography.Body.Small.Medium,
                color = if (habit.endDate != null) BetterMeColors.Primary.Primary
                else BetterMeColors.Border.Wrong,
                modifier = Modifier.padding(start = 36.dp)
            )
        }
    }
}

// =========================
// ANIMATED CHECKBOX
// =========================
@Composable
fun AnimatedCheckbox(isChecked: Boolean) {
    val bgColor by animateColorAsState(
        targetValue = if (isChecked) BetterMeColors.Primary.Primary else Color.Transparent,
        animationSpec = tween(200),
        label = ""
    )
    val borderColor by animateColorAsState(
        targetValue = if (isChecked) BetterMeColors.Primary.Primary else BetterMeColors.Border.BorderMedium,
        animationSpec = tween(200),
        label = ""
    )

    Box(
        modifier = Modifier
            .size(24.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(bgColor)
            .border(1.5.dp, borderColor, RoundedCornerShape(6.dp)),
        contentAlignment = Alignment.Center
    ) {
        if (isChecked) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

// =========================
// HABIT SETTINGS DIALOG
// =========================
@Composable
fun HabitSettingsDialog(
    habit: SuggestedHabitUiModel,
    repeatOptions: List<String>,
    onSetReminder: () -> Unit,
    onSetRepeat: () -> Unit,
    onSetStartDate: () -> Unit,
    onSetEndDate: () -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "⚙️ Cài đặt thói quen",
                style = BetterMeTypography.Title.Medium.SemiBold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                // Tên thói quen
                Text(
                    text = habit.title,
                    style = BetterMeTypography.Body.Medium,
                    color = BetterMeColors.Text.TextPrimary
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Giờ nhắc nhở
                SettingRow(
                    icon = "⏰",
                    label = "Nhắc nhở lúc:",
                    value = habit.reminderTimeFormatted,
                    onClick = onSetReminder
                )

                // Lịch lặp
                SettingRow(
                    icon = "📊",
                    label = "Lặp lại:",
                    value = habit.repeatLabel,
                    onClick = onSetRepeat
                )

                SettingRow(
                    icon = "📅",
                    label = "Ngày bắt đầu:",
                    value = formatDate(habit.startDate),
                    onClick = onSetStartDate
                )

                SettingRow(
                    icon = "🏁",
                    label = "Ngày kết thúc:",
                    value = habit.endDate?.let { formatDate(it) } ?: "Chưa chọn",
                    onClick = onSetEndDate
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(
                    text = "Xác nhận",
                    style = BetterMeTypography.Body.Medium,
                    color = BetterMeColors.Primary.Primary
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = "Huỷ",
                    style = BetterMeTypography.Body.Medium,
                    color = BetterMeColors.Text.TextTertiary
                )
            }
        },
        shape = BetterMeShapes.large,
        containerColor = BetterMeColors.BackGround.BackgroundPrimary
    )
}

private fun formatDate(dateMillis: Long): String {
    val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    return dateFormat.format(Date(dateMillis))
}

// =========================
// SETTING ROW
// =========================
@Composable
fun SettingRow(
    icon: String,
    label: String,
    value: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(BetterMeShapes.medium)
            .background(BetterMeColors.BackGround.BackgroundSecondary)
            .border(1.dp, BetterMeColors.Border.BorderLight, BetterMeShapes.medium)
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = icon, style = BetterMeTypography.Title.Medium.Medium)
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = label,
            style = BetterMeTypography.Body.Medium,
            color = BetterMeColors.Text.TextPrimary,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = value,
            style = BetterMeTypography.Body.Medium,
            color = BetterMeColors.Text.TextTertiary
        )
        Spacer(modifier = Modifier.width(4.dp))
        Icon(
            imageVector = Icons.Default.KeyboardArrowDown,
            contentDescription = null,
            tint = BetterMeColors.Text.TextTertiary,
            modifier = Modifier.size(20.dp)
        )
    }
}

// =========================
// REPEAT PICKER DIALOG
// =========================
@Composable
fun RepeatPickerDialog(
    options: List<String>,
    selected: String,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Chọn lịch lặp lại",
                style = BetterMeTypography.Title.Medium.SemiBold
            )
        },
        text = {
            Column {
                options.forEach { option ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(option) }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = option,
                            style = BetterMeTypography.Body.Medium,
                            color = if (option == selected) BetterMeColors.Primary.Primary
                            else BetterMeColors.Text.TextPrimary
                        )
                        if (option == selected) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = BetterMeColors.Primary.Primary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                    if (option != options.last()) {
                        HorizontalDivider(color = BetterMeColors.Border.BorderLight)
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = "Đóng",
                    style = BetterMeTypography.Body.Medium,
                    color = BetterMeColors.Text.TextTertiary
                )
            }
        },
        shape = BetterMeShapes.large,
        containerColor = BetterMeColors.BackGround.BackgroundPrimary
    )
}

// =========================
// REMINDER TIME PICKER DIALOG
// =========================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReminderTimePickerDialog(
    currentHour: Int,
    currentMinute: Int,
    onConfirm: (Int, Int) -> Unit,
    onDismiss: () -> Unit
) {
    val timePickerState = rememberTimePickerState(
        initialHour = currentHour,
        initialMinute = currentMinute,
        is24Hour = true
    )

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = BetterMeShapes.large,
            color = BetterMeColors.BackGround.BackgroundPrimary,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Chọn giờ nhắc nhở",
                    style = BetterMeTypography.Title.Medium.SemiBold,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                )

                TimePicker(state = timePickerState)

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(
                            text = "Hủy",
                            style = BetterMeTypography.Body.Medium,
                            color = BetterMeColors.Text.TextTertiary
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    TextButton(
                        onClick = { onConfirm(timePickerState.hour, timePickerState.minute) }
                    ) {
                        Text(
                            text = "Xác nhận",
                            style = BetterMeTypography.Body.Medium,
                            color = BetterMeColors.Primary.Primary
                        )
                    }
                }
            }
        }
    }
}

// =========================
// PREVIEW
// =========================
@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun HabitSuggestionPreview() {
    HabitSuggestionContent(
        state = HabitSuggestionState(
            categoryHabits = listOf(
                CategoryWithHabits(
                    categoryName = "Vận động & thể chất",
                    categoryIcon = "🏃",
                    categoryId = 1,
                    habits = listOf(
                        SuggestedHabitUiModel(1, "Đi bộ 10.000 bước mỗi ngày", true, 7, 0, "Hàng ngày"),
                        SuggestedHabitUiModel(2, "Tập Gym 30 phút", true, 18, 0, "Các ngày trong tuần"),
                        SuggestedHabitUiModel(3, "Dãn cơ 15 phút mỗi sáng", false),
                    )
                ),
                CategoryWithHabits(
                    categoryName = "Tinh thần & sức khỏe tâm lý",
                    categoryIcon = "🧠",
                    categoryId = 3,
                    habits = listOf(
                        SuggestedHabitUiModel(4, "Thiền 10 phút mỗi sáng", true, 6, 30, "Hàng ngày"),
                        SuggestedHabitUiModel(5, "Viết nhật ký trước khi ngủ", false),
                    )
                )
            )
        ),
        repeatOptions = listOf("Hàng ngày", "Các ngày trong tuần", "Cuối tuần"),
        onIntent = {}
    )
}
