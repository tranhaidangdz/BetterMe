package com.example.betterme.presentation.addhabit

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.example.betterme.presentation.addhabit.components.AddHabitFieldCard
import com.example.betterme.presentation.addhabit.components.AddHabitTopBar
import com.example.betterme.presentation.components.button.BetterMeButton
import com.example.betterme.presentation.theme.BetterMeColors
import org.koin.androidx.compose.koinViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun AddHabitScreen(
    onBackClick: () -> Unit,
    viewModel: AddHabitViewModel = koinViewModel()
) {
    val state by viewModel.viewState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.singleEvent.collect { event ->
            when (event) {
                is AddHabitEvent.ShowError -> snackbarHostState.showSnackbar(event.message)
                is AddHabitEvent.ShowSuccess -> snackbarHostState.showSnackbar(event.message)
            }
        }
    }

    AddHabitContent(
        state = state,
        repeatOptions = viewModel.repeatOptions,
        onIntent = viewModel::processIntent,
        onBackClick = onBackClick,
        snackbarHostState = snackbarHostState
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddHabitContent(
    state: AddHabitState,
    repeatOptions: List<String>,
    onIntent: (AddHabitIntent) -> Unit,
    onBackClick: () -> Unit,
    snackbarHostState: SnackbarHostState
) {
    var showCategoryMenu by remember { mutableStateOf(false) }
    var showRepeatMenu by remember { mutableStateOf(false) }
    var showStartDatePicker by remember { mutableStateOf(false) }
    var showEndDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }

    val startDateText = remember(state.startDateMillis) { state.startDateMillis.toDateText() }
    val endDateText = remember(state.endDateMillis) { state.endDateMillis.toDateText() }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(BetterMeColors.BackGround.BackgroundSecondary)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(top = 12.dp, bottom = 120.dp)
    ) {
        item {
            AddHabitTopBar(onBackClick = onBackClick)
        }
        item {
            AddHabitFieldCard(
                label = "Nhóm thói quen",
                value = state.selectedCategoryName.ifBlank { "Chọn nhóm thói quen" },
                leadingIcon = state.categories.firstOrNull { it.id == state.selectedCategoryId }?.icon,
                onClick = { showCategoryMenu = true }
            )
            DropdownMenu(
                expanded = showCategoryMenu,
                onDismissRequest = { showCategoryMenu = false }
            ) {
                state.categories.forEach { category ->
                    DropdownMenuItem(
                        text = { Text("${category.icon}  ${category.name}") },
                        onClick = {
                            onIntent(AddHabitIntent.SelectCategory(category.id))
                            showCategoryMenu = false
                        }
                    )
                }
            }
        }
        item {
            OutlinedTextField(
                value = state.title,
                onValueChange = { onIntent(AddHabitIntent.ChangeTitle(it)) },
                label = { Text("Tên thói quen") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                singleLine = true
            )
        }
        item {
            OutlinedTextField(
                value = state.description,
                onValueChange = { onIntent(AddHabitIntent.ChangeDescription(it)) },
                label = { Text("Mô tả") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3
            )
        }
        item {
            AddHabitFieldCard(
                label = "Ngày bắt đầu",
                value = startDateText,
                leadingIcon = "📅",
                onClick = { showStartDatePicker = true }
            )
        }
        item {
            AddHabitFieldCard(
                label = "Ngày kết thúc",
                value = endDateText,
                leadingIcon = "📅",
                onClick = { showEndDatePicker = true }
            )
        }
        item {
            AddHabitFieldCard(
                label = "Giờ nhắc nhở",
                value = state.reminderTimeFormatted,
                leadingIcon = "⏰",
                onClick = { showTimePicker = true }
            )
        }
        item {
            AddHabitFieldCard(
                label = "Tần suất nhắc nhở",
                value = state.repeatPattern,
                leadingIcon = "⏳",
                onClick = { showRepeatMenu = true }
            )
            DropdownMenu(
                expanded = showRepeatMenu,
                onDismissRequest = { showRepeatMenu = false }
            ) {
                repeatOptions.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option) },
                        onClick = {
                            onIntent(AddHabitIntent.SetRepeatPattern(option))
                            showRepeatMenu = false
                        }
                    )
                }
            }
        }
        item {
            BetterMeButton(
                onClick = { onIntent(AddHabitIntent.Submit) },
                text = "Thêm thói quen"
            )
        }
        item {
            SnackbarHost(hostState = snackbarHostState)
        }
    }

    if (showStartDatePicker) {
        val pickerState = rememberDatePickerState(initialSelectedDateMillis = state.startDateMillis)
        DatePickerDialog(
            onDismissRequest = { showStartDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    pickerState.selectedDateMillis?.let { onIntent(AddHabitIntent.SetStartDate(it)) }
                    showStartDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showStartDatePicker = false }) { Text("Hủy") }
            }
        ) { DatePicker(state = pickerState) }
    }

    if (showEndDatePicker) {
        val pickerState = rememberDatePickerState(initialSelectedDateMillis = state.endDateMillis)
        DatePickerDialog(
            onDismissRequest = { showEndDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    pickerState.selectedDateMillis?.let { onIntent(AddHabitIntent.SetEndDate(it)) }
                    showEndDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showEndDatePicker = false }) { Text("Hủy") }
            }
        ) { DatePicker(state = pickerState) }
    }

    if (showTimePicker) {
        val timeState = rememberTimePickerState(
            initialHour = state.reminderHour,
            initialMinute = state.reminderMinute,
            is24Hour = true
        )
        DatePickerDialog(
            onDismissRequest = { showTimePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    onIntent(AddHabitIntent.SetReminderTime(timeState.hour, timeState.minute))
                    showTimePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) { Text("Hủy") }
            }
        ) { TimePicker(state = timeState) }
    }
}

private fun Long.toDateText(): String {
    return SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(this))
}
