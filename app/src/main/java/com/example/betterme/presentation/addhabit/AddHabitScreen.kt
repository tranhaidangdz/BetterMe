package com.example.betterme.presentation.addhabit

import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.betterme.R
import com.example.betterme.presentation.addhabit.components.CategorySelector
import com.example.betterme.presentation.addhabit.components.HabitFormField
import com.example.betterme.presentation.components.view.BetterMeTopBar
import com.example.betterme.presentation.theme.BetterMeColors
import com.example.betterme.presentation.theme.BetterMeTypography
import org.koin.androidx.compose.koinViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddHabitScreen(
    onBackClick: () -> Unit = {},
    onHabitAdded: () -> Unit = {},
    viewModel: AddHabitViewModel = koinViewModel()
) {
    val state by viewModel.viewState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var showStartDatePicker by remember { mutableStateOf(false) }
    var showEndDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.singleEvent.collect { event ->
            when (event) {
                AddHabitEvent.SaveSuccess -> {
                    onHabitAdded()
                    snackbarHostState.showSnackbar("Đã thêm thói quen thành công!")
                    onBackClick()
                }
                is AddHabitEvent.ShowError -> {
                    snackbarHostState.showSnackbar(event.message)
                }
            }
        }
    }

    // Date pickers
    if (showStartDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = state.startDate
        )
        DatePickerDialog(
            onDismissRequest = { showStartDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let {
                        viewModel.processIntent(AddHabitIntent.InputStartDate(it))
                    }
                    showStartDatePicker = false
                }) {
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

    if (showEndDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = state.endDate ?: (state.startDate + 30L * 24 * 60 * 60 * 1000)
        )
        DatePickerDialog(
            onDismissRequest = { showEndDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let {
                        viewModel.processIntent(AddHabitIntent.InputEndDate(it))
                    }
                    showEndDatePicker = false
                }) {
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

    if (showTimePicker) {
        val timeParts = state.reminderTime.split(":")
        val initialHour = timeParts.getOrNull(0)?.toIntOrNull() ?: 7
        val initialMinute = timeParts.getOrNull(1)?.toIntOrNull() ?: 0
        val timePickerState = rememberTimePickerState(
            initialHour = initialHour,
            initialMinute = initialMinute,
            is24Hour = true
        )

        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            title = {
                Text("Chọn giờ nhắc nhở", style = BetterMeTypography.Title.Medium.SemiBold)
            },
            text = { TimePicker(state = timePickerState) },
            confirmButton = {
                TextButton(
                    onClick = {
                        val time = String.format("%02d:%02d", timePickerState.hour, timePickerState.minute)
                        viewModel.processIntent(AddHabitIntent.InputReminderTime(time))
                        showTimePicker = false
                    }
                ) {
                    Text("Xác nhận", color = BetterMeColors.Primary.Primary)
                }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) {
                    Text("Hủy", color = BetterMeColors.Text.TextTertiary)
                }
            },
            containerColor = BetterMeColors.BackGround.BackgroundPrimary,
            shape = RoundedCornerShape(20.dp)
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        containerColor = BetterMeColors.BackGround.BackgroundSecondary
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .statusBarsPadding()
                .background(BetterMeColors.BackGround.BackgroundSecondary)
        ) {
            // ===== TOP BAR =====
            BetterMeTopBar(
                leadingIconRes = R.drawable.ic_arrow_left,
                title = "Thêm thói quen",
                onLeadingClick = onBackClick
            )

            // ===== FORM =====
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {

                // Nhóm thói quen
                CategorySelector(
                    categories = state.categories,
                    selectedCategoryName = state.selectedCategoryName,
                    isExpanded = state.showCategorySelector,
                    onToggle = { viewModel.processIntent(AddHabitIntent.ToggleCategorySelector) },
                    onSelect = { id, name ->
                        viewModel.processIntent(AddHabitIntent.SelectCategory(id, name))
                    },
                    containerColor = Color(0xFFE6F3FF)
                )

                // Tên thói quen
                HabitFormField(
                    value = state.title,
                    onValueChange = { viewModel.processIntent(AddHabitIntent.InputTitle(it)) },
                    label = "Tên thói quen *",
                    placeholder = "VD: Đi bộ 10.000 bước mỗi ngày",
                    isError = state.titleError != null,
                    errorText = state.titleError,
                    containerColor = Color(0xFFEDEAF7)
                )

                // Mô tả
                HabitFormField(
                    value = state.description,
                    onValueChange = { viewModel.processIntent(AddHabitIntent.InputDescription(it)) },
                    label = "Mô tả",
                    placeholder = "Mô tả chi tiết về thói quen",
                    singleLine = false,
                    minLines = 3,
                    containerColor = Color(0xFFE7F1DE)
                )

                // Ngày bắt đầu
                DateField(
                    label = "Ngày bắt đầu",
                    dateMillis = state.startDate,
                    onClick = { showStartDatePicker = true },
                    containerColor = Color(0xFFFDF3D2)
                )

                // Hạn hoàn thành
                DateField(
                    label = "Hạn hoàn thành",
                    dateMillis = state.endDate,
                    placeholder = "Chọn ngày kết thúc",
                    onClick = { showEndDatePicker = true },
                    containerColor = Color(0xFFFDF3D2)
                )

                // Thời gian nhắc nhở
                TimeField(
                    value = state.reminderTime,
                    label = "Thời gian nhắc nhở",
                    placeholder = "Chọn giờ nhắc",
                    onClick = { showTimePicker = true },
                    containerColor = Color(0xFFFBEED8)
                )

                Spacer(modifier = Modifier.height(8.dp))

                // ===== SUBMIT BUTTON =====
                Button(
                    onClick = { viewModel.processIntent(AddHabitIntent.Submit) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = BetterMeColors.Primary.Primary,
                        disabledContainerColor = BetterMeColors.Primary.Primary.copy(alpha = 0.4f)
                    ),
                    enabled = !state.isLoading
                ) {
                    if (state.isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(
                            text = "Thêm thói quen",
                            style = BetterMeTypography.Title.Small.SemiBold,
                            color = Color.White
                        )
                    }
                }

                // Bottom spacing
                Spacer(modifier = Modifier.height(80.dp))
            }
        }
    }
}

// ============================================================
// DATE FIELD — Composable hiển thị ngày đã chọn
// ============================================================
@Composable
private fun DateField(
    label: String,
    dateMillis: Long?,
    placeholder: String = "",
    onClick: () -> Unit,
    containerColor: Color = BetterMeColors.BackGround.BackgroundPrimary
) {
    val dateFormat = remember { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()) }
    val displayText = if (dateMillis != null) {
        dateFormat.format(Date(dateMillis))
    } else {
        placeholder
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label,
            style = BetterMeTypography.Body.Small.Medium,
            color = BetterMeColors.Text.TextSecondary,
            modifier = Modifier.padding(bottom = 6.dp)
        )
        OutlinedCard(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onClick() },
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(1.dp, BetterMeColors.Border.BorderLight),
            colors = CardDefaults.outlinedCardColors(
                containerColor = containerColor
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "📅",
                    modifier = Modifier.padding(end = 10.dp)
                )
                Text(
                    text = displayText,
                    style = BetterMeTypography.Body.Medium,
                    color = if (dateMillis != null) BetterMeColors.Text.TextPrimary
                    else BetterMeColors.Text.TextTertiary,
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    tint = BetterMeColors.Text.TextTertiary
                )
            }
        }
    }
}

@Composable
private fun TimeField(
    value: String,
    label: String,
    placeholder: String,
    onClick: () -> Unit,
    containerColor: Color
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label,
            style = BetterMeTypography.Body.Small.Medium,
            color = BetterMeColors.Text.TextSecondary,
            modifier = Modifier.padding(bottom = 6.dp)
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(containerColor)
                .border(1.dp, BetterMeColors.Border.BorderLight, RoundedCornerShape(14.dp))
                .clickable { onClick() }
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = "⏰", modifier = Modifier.padding(end = 10.dp))
            Text(
                text = value.ifBlank { placeholder },
                style = BetterMeTypography.Body.Medium,
                color = if (value.isBlank()) BetterMeColors.Text.TextTertiary else BetterMeColors.Text.TextPrimary,
                modifier = Modifier.weight(1f)
            )
            Icon(
                imageVector = Icons.Default.KeyboardArrowDown,
                contentDescription = null,
                tint = BetterMeColors.Text.TextTertiary
            )
        }
    }
}
