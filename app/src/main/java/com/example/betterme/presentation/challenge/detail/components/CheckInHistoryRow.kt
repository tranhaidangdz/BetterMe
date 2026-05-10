package com.example.betterme.presentation.challenge.detail.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.betterme.presentation.theme.BetterMeColors
import com.example.betterme.presentation.theme.BetterMeTypography

data class CheckInHistoryItemUi(
    val dateLabel: String,      // "20/04/2026"
    val timeLabel: String,      // "07:35"
    val note: String?
)

/**
 * Most-recent check-ins shown beneath the week-streak strip on Challenge Detail.
 * Empty state is rendered as a single dim row so the section always reserves space.
 */
@Composable
fun CheckInHistoryCard(
    items: List<CheckInHistoryItemUi>,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Column(
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
            .padding(16.dp)
    ) {
        if (items.isEmpty()) {
            Text(
                text = "Chưa có lịch sử check-in",
                style = BetterMeTypography.Body.Small.Medium,
                color = BetterMeColors.Text.TextTertiary
            )
        } else {
            items.forEachIndexed { index, item ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(accentColor.copy(alpha = 0.18f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = "✓", color = accentColor)
                    }
                    Spacer(modifier = Modifier.size(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = item.dateLabel,
                            style = BetterMeTypography.Body.Medium,
                            color = BetterMeColors.Text.TextPrimary
                        )
                        if (!item.note.isNullOrBlank()) {
                            Text(
                                text = item.note,
                                style = BetterMeTypography.Body.Small.Medium,
                                color = BetterMeColors.Text.TextTertiary,
                                maxLines = 2
                            )
                        }
                    }
                    Text(
                        text = item.timeLabel,
                        style = BetterMeTypography.Body.Small.Medium,
                        color = BetterMeColors.Text.TextTertiary
                    )
                }
                if (index < items.lastIndex) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(BetterMeColors.Border.BorderLight)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}

/**
 * Compact "Reminder + Estimated completion" two-stat row above the milestone bar.
 */
@Composable
fun ReminderEtaRow(
    reminderTimeLabel: String,
    estimatedCompletionLabel: String,
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
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        InfoCell("⏰ Nhắc hằng ngày", reminderTimeLabel, accentColor)
        Box(
            modifier = Modifier
                .size(width = 1.dp, height = 32.dp)
                .background(BetterMeColors.Border.BorderLight)
        )
        InfoCell("📅 Hoàn thành dự kiến", estimatedCompletionLabel, accentColor)
    }
}

@Composable
private fun InfoCell(label: String, value: String, accent: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            style = BetterMeTypography.Body.Small.Medium,
            color = BetterMeColors.Text.TextTertiary
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = value,
            style = BetterMeTypography.Title.Small.Bold,
            color = accent
        )
    }
}
