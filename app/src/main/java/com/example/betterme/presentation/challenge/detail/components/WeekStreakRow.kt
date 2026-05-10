package com.example.betterme.presentation.challenge.detail.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.betterme.presentation.challenge.model.DayCellUi
import com.example.betterme.presentation.challenge.model.DayStatus
import com.example.betterme.presentation.theme.BetterMeColors
import com.example.betterme.presentation.theme.BetterMeTypography

@Composable
fun WeekStreakRow(
    days: List<DayCellUi>,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = 3.dp,
                shape = RoundedCornerShape(16.dp),
                ambientColor = Color(0x0D000000),
                spotColor = Color(0x1A000000)
            )
            .clip(RoundedCornerShape(16.dp))
            .background(BetterMeColors.White)
            .padding(horizontal = 8.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        days.forEach { d ->
            DayCell(d, accentColor, Modifier.weight(1f))
        }
    }
}

@Composable
private fun DayCell(day: DayCellUi, accent: Color, modifier: Modifier) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = day.label,
            style = BetterMeTypography.Body.Small.Medium,
            color = BetterMeColors.Text.TextTertiary
        )
        Text(
            text = day.dateLabel,
            style = BetterMeTypography.Body.Small.Medium,
            color = BetterMeColors.Text.TextTertiary
        )
        Spacer(modifier = Modifier.height(6.dp))
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .then(
                    when (day.status) {
                        DayStatus.Done -> Modifier.background(accent)
                        DayStatus.Today -> Modifier
                            .background(BetterMeColors.White)
                            .border(2.dp, accent, CircleShape)
                        DayStatus.Missed -> Modifier
                            .background(BetterMeColors.Gray.Gray3)
                        DayStatus.Future -> Modifier
                            .background(BetterMeColors.White)
                            .border(1.dp, BetterMeColors.Border.BorderLight, CircleShape)
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            when (day.status) {
                DayStatus.Done -> Text(
                    text = "✓",
                    color = BetterMeColors.White,
                    fontWeight = FontWeight.Bold
                )
                DayStatus.Today -> Text(
                    text = "•",
                    color = accent,
                    fontWeight = FontWeight.Bold
                )
                else -> Unit
            }
        }
    }
}
