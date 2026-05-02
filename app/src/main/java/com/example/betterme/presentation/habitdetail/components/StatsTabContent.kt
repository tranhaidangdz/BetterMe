package com.example.betterme.presentation.habitdetail.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.betterme.presentation.habitdetail.HabitStatUiModel
import com.example.betterme.presentation.theme.BetterMeColors
import com.example.betterme.presentation.theme.BetterMeTypography

@Composable
fun StatsTabContent(
    stats: HabitStatUiModel,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Top row: 2 stat cards
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            StatCard(
                label = "Tổng check-in",
                value = "${stats.totalCheckIns}",
                emoji = "✅",
                modifier = Modifier.weight(1f)
            )
            StatCard(
                label = "Tỷ lệ hoàn thành",
                value = "${stats.completionRate}%",
                emoji = "📊",
                modifier = Modifier.weight(1f)
            )
        }

        // Bottom row: 2 stat cards
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            StatCard(
                label = "Chuỗi hiện tại",
                value = "${stats.currentStreak} ngày",
                emoji = "🔥",
                modifier = Modifier.weight(1f)
            )
            StatCard(
                label = "Chuỗi dài nhất",
                value = "${stats.longestStreak} ngày",
                emoji = "🏆",
                modifier = Modifier.weight(1f)
            )
        }

        // Progress summary
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            StatCard(
                label = "Tuần này",
                value = "${stats.weeklyProgress}/7 ngày",
                emoji = "📅",
                modifier = Modifier.weight(1f)
            )
            StatCard(
                label = "Tổng ngày theo dõi",
                value = "${stats.totalDays} ngày",
                emoji = "📌",
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun StatCard(
    label: String,
    value: String,
    emoji: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(BetterMeColors.White)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = emoji,
            style = BetterMeTypography.Title.Medium.Bold,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = value,
            style = BetterMeTypography.Title.Small.Bold,
            color = BetterMeColors.Primary.Primary,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            style = BetterMeTypography.Body.Small.Medium,
            color = BetterMeColors.Text.TextTertiary,
            textAlign = TextAlign.Center
        )
    }
}
