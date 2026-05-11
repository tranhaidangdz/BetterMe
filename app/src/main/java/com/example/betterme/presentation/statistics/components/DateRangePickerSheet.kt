package com.example.betterme.presentation.statistics.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDateRangePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.betterme.presentation.theme.BetterMeColors
import com.example.betterme.presentation.theme.BetterMeTypography

/**
 * Bottom sheet wrapper around Material 3's `DateRangePicker`.
 *
 * Surfaced from the Statistics screen when the user picks the "Tùy chọn" tab. The
 * sheet stays open until the user explicitly confirms a range or dismisses it.
 *
 * Timezone-safe: M3's `DateRangePickerState` returns UTC midnight millis. The caller
 * is responsible for clamping to local-day boundaries via `DateUtils.startOfDay` —
 * which the `SelectCustomRange` intent handler already does.
 *
 * Survives recomposition: `rememberDateRangePickerState` and `rememberModalBottomSheetState`
 * are both `rememberSaveable`-aware, so the user's partial selection is preserved across
 * configuration changes.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateRangePickerSheet(
    initialStart: Long?,
    initialEnd: Long?,
    onDismiss: () -> Unit,
    onConfirm: (startMillis: Long, endMillis: Long) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val pickerState = rememberDateRangePickerState(
        initialSelectedStartDateMillis = initialStart,
        initialSelectedEndDateMillis = initialEnd
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color.White
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            DateRangePicker(
                state = pickerState,
                title = {
                    Text(
                        text = "Chọn khoảng ngày",
                        style = BetterMeTypography.Title.Small.Bold,
                        color = BetterMeColors.Text.TextPrimary,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 4.dp)
                    )
                },
                headline = {
                    val s = pickerState.selectedStartDateMillis
                    val e = pickerState.selectedEndDateMillis
                    val text = when {
                        s == null -> "Chọn ngày bắt đầu"
                        e == null -> "${formatShort(s)} → Chọn ngày kết thúc"
                        else -> "${formatShort(s)} → ${formatShort(e)}"
                    }
                    Text(
                        text = text,
                        style = BetterMeTypography.Body.Medium,
                        color = BetterMeColors.Text.TextSecondary,
                        modifier = Modifier.padding(start = 16.dp, bottom = 12.dp)
                    )
                },
                showModeToggle = false,
                colors = DatePickerDefaults.colors(
                    selectedDayContainerColor = BetterMeColors.Primary.Primary,
                    selectedDayContentColor = Color.White,
                    todayDateBorderColor = BetterMeColors.Primary.Primary,
                    dayInSelectionRangeContainerColor = BetterMeColors.Primary.Primary.copy(alpha = 0.18f),
                    dayInSelectionRangeContentColor = BetterMeColors.Primary.Primary
                )
            )

            Spacer(Modifier.height(8.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                SheetTextButton(
                    label = "Hủy",
                    accent = BetterMeColors.Text.TextTertiary,
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f)
                )
                val s = pickerState.selectedStartDateMillis
                val e = pickerState.selectedEndDateMillis
                val canConfirm = s != null && e != null && e >= s
                SheetPrimaryButton(
                    label = "Áp dụng",
                    enabled = canConfirm,
                    onClick = {
                        if (canConfirm) onConfirm(s!!, e!!)
                    },
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun SheetTextButton(
    label: String,
    accent: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .height(50.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(BetterMeColors.Gray.Gray3)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = BetterMeTypography.Body.Medium,
            color = accent,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun SheetPrimaryButton(
    label: String,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .height(50.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(
                if (enabled) BetterMeColors.Primary.Primary
                else BetterMeColors.Gray.Gray2
            )
            .clickable(enabled = enabled) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = BetterMeTypography.Title.Small.Bold,
            color = Color.White,
            fontWeight = FontWeight.Bold
        )
    }
}

/** "dd/MM/yyyy" — used as the sheet headline. */
private fun formatShort(ms: Long): String {
    val sdf = java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale("vi"))
    return sdf.format(java.util.Date(ms))
}
