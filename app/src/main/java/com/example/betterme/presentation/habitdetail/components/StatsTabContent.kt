package com.example.betterme.presentation.habitdetail.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
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
                tintColor = Color(0xFFE8F4FD),  // Pastel blue
                modifier = Modifier.weight(1f)
            )
            StatCard(
                label = "Tỷ lệ hoàn thành",
                value = "${stats.completionRate}%",
                emoji = "📊",
                tintColor = Color(0xFFFFF3E0),  // Pastel amber
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
                tintColor = Color(0xFFE8F5E9),  // Pastel green
                modifier = Modifier.weight(1f)
            )
            StatCard(
                label = "Chuỗi dài nhất",
                value = "${stats.longestStreak} ngày",
                emoji = "🏆",
                tintColor = Color(0xFFF3E5F5),  // Pastel purple
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
                tintColor = Color(0xFFE0F7FA),  // Pastel cyan
                modifier = Modifier.weight(1f)
            )
            StatCard(
                label = "Tổng ngày theo dõi",
                value = "${stats.totalDays} ngày",
                emoji = "📌",
                tintColor = Color(0xFFFCE4EC),  // Pastel pink
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
    tintColor: Color = BetterMeColors.White,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .shadow(
                elevation = 4.dp,
                shape = RoundedCornerShape(16.dp),
                ambientColor = Color.Black.copy(alpha = 0.06f),
                spotColor = Color.Black.copy(alpha = 0.04f)
            )
            .clip(RoundedCornerShape(16.dp))
            .background(tintColor)
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
