package com.example.betterme.presentation.habitdetail.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.betterme.presentation.habitdetail.CalendarDayUiModel
import com.example.betterme.presentation.theme.BetterMeColors
import com.example.betterme.presentation.theme.BetterMeTypography

private val weekDayHeaders = listOf("T2", "T3", "T4", "T5", "T6", "T7", "CN")

@Composable
fun CheckInCalendar(
    title: String,
    days: List<CalendarDayUiModel>,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(BetterMeColors.White)
            .padding(16.dp)
    ) {
        // Month header with arrows
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onPreviousMonth, modifier = Modifier.size(32.dp)) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                    contentDescription = "Tháng trước",
                    tint = BetterMeColors.Primary.Primary
                )
            }
            Text(
                text = title,
                style = BetterMeTypography.Title.Small.Bold,
                color = BetterMeColors.Text.TextPrimary
            )
            IconButton(onClick = onNextMonth, modifier = Modifier.size(32.dp)) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = "Tháng sau",
                    tint = BetterMeColors.Primary.Primary
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Weekday headers
        Row(modifier = Modifier.fillMaxWidth()) {
            weekDayHeaders.forEach { header ->
                Text(
                    text = header,
                    style = BetterMeTypography.Body.Small.Medium,
                    color = BetterMeColors.Text.TextTertiary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Calendar grid (6 rows x 7 cols)
        val rows = days.chunked(7)
        rows.forEach { week ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(40.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                week.forEach { day ->
                    CalendarDayCell(day = day, modifier = Modifier.weight(1f))
                }
                // Fill remaining if week has less than 7 days
                repeat(7 - week.size) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun CalendarDayCell(
    day: CalendarDayUiModel,
    modifier: Modifier = Modifier
) {
    val textColor = when {
        day.isToday -> BetterMeColors.White
        !day.isCurrentMonth -> BetterMeColors.Text.TextTertiary.copy(alpha = 0.4f)
        else -> BetterMeColors.Text.TextPrimary
    }

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(30.dp)
                    .clip(CircleShape)
                    .background(
                        when {
                            day.isToday -> BetterMeColors.Primary.Primary
                            else -> BetterMeColors.White
                        }
                    )
                    .then(
                        if (day.isToday) Modifier else Modifier
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "${day.day}",
                    style = BetterMeTypography.Body.Small.Medium,
                    color = textColor,
                    textAlign = TextAlign.Center
                )
            }
            // Check-in dot indicator
            if (day.isCheckedIn && day.isCurrentMonth) {
                Spacer(modifier = Modifier.height(1.dp))
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(BetterMeColors.Green)
                )
            } else {
                Spacer(modifier = Modifier.height(7.dp))
            }
        }
    }
}
