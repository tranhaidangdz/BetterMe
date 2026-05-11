package com.example.betterme.presentation.statistics.components

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
import androidx.compose.ui.unit.sp
import com.example.betterme.presentation.statistics.AdditionalInsights
import com.example.betterme.presentation.statistics.ChallengeStats
import com.example.betterme.presentation.statistics.OverviewStats
import com.example.betterme.presentation.statistics.StreakAnalytics
import com.example.betterme.presentation.theme.BetterMeColors
import com.example.betterme.presentation.theme.BetterMeTokens
import com.example.betterme.presentation.theme.BetterMeTypography

/**
 * Derived insight panel. Builds 2-4 short motivational lines from the same analytics
 * inputs the rest of the screen renders — no random copy, no generated text. If a
 * particular signal isn't strong enough to form an insight (e.g. zero check-ins,
 * tied weekday distribution), the line is dropped from the output rather than
 * shown as a generic filler. When zero insights survive the filter the whole card
 * hides itself.
 */
@Composable
fun SmartInsightsCard(
    overview: OverviewStats,
    streaks: StreakAnalytics,
    insights: AdditionalInsights,
    challengeStats: ChallengeStats,
    modifier: Modifier = Modifier
) {
    val derived = buildList {
        // Best-day insight — only when we have at least 3 check-ins to draw a signal.
        if (insights.totalCompletions >= 3 && insights.mostProductiveDay != "—") {
            add(
                InsightLine(
                    emoji = "📈",
                    text = "${insights.mostProductiveDay} là ngày bạn nhất quán nhất.",
                    accent = Color(0xFF4F46E5)
                )
            )
        }
        // Current-streak insight.
        if (streaks.currentStreak >= 3) {
            val label = if (streaks.longestStreak > streaks.currentStreak)
                "Còn ${streaks.longestStreak - streaks.currentStreak} ngày nữa để phá kỷ lục cá nhân."
            else
                "Bạn đang lập kỷ lục mới cho chính mình."
            add(
                InsightLine(
                    emoji = "🔥",
                    text = "Chuỗi hiện tại: ${streaks.currentStreak} ngày. $label",
                    accent = Color(0xFFEA580C)
                )
            )
        }
        // Completion-rate insight.
        when {
            overview.completionRate >= 80 -> add(
                InsightLine(
                    emoji = "🏅",
                    text = "Tỷ lệ hoàn thành ${overview.completionRate}% — bạn đang ở phong độ tốt nhất.",
                    accent = Color(0xFF16A34A)
                )
            )
            overview.completionRate in 50..79 -> add(
                InsightLine(
                    emoji = "💪",
                    text = "Tỷ lệ hoàn thành ${overview.completionRate}% — vững vàng. Cố thêm chút nữa nhé.",
                    accent = Color(0xFF0EA5E9)
                )
            )
            overview.completionRate in 1..49 && overview.totalOngoing > 0 -> add(
                InsightLine(
                    emoji = "🌱",
                    text = "Mỗi ngày check-in là một viên gạch. Bắt đầu từ một thói quen dễ nhất hôm nay.",
                    accent = Color(0xFF7C3AED)
                )
            )
        }
        // Best-hour insight — only when we have a clear winner.
        if (insights.totalCompletions >= 5 && insights.mostActiveHour != "—") {
            add(
                InsightLine(
                    emoji = "⏰",
                    text = "Bạn năng suất nhất trong khung giờ ${insights.mostActiveHour}.",
                    accent = Color(0xFFDB2777)
                )
            )
        }
        // Challenge-side insight.
        if (challengeStats.completed >= 1) {
            add(
                InsightLine(
                    emoji = "🏆",
                    text = "Đã hoàn thành ${challengeStats.completed} thử thách — tỷ lệ thành công ${challengeStats.completionRatePct}%.",
                    accent = Color(0xFFEAB308)
                )
            )
        }
    }.take(4)

    if (derived.isEmpty()) return

    Column(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = BetterMeTokens.CardElevation.Body,
                shape = RoundedCornerShape(BetterMeTokens.CardRadius.Hero),
                ambientColor = BetterMeTokens.NeutralShadow.Ambient,
                spotColor = BetterMeTokens.NeutralShadow.Spot
            )
            .clip(RoundedCornerShape(BetterMeTokens.CardRadius.Hero))
            .background(Color.White)
            .border(
                width = 1.dp,
                color = BetterMeColors.Primary.Primary.copy(alpha = BetterMeTokens.AccentAlpha.Medium),
                shape = RoundedCornerShape(BetterMeTokens.CardRadius.Hero)
            )
            .padding(18.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = "✨", fontSize = 22.sp)
            Spacer(Modifier.size(8.dp))
            Text(
                text = "Gợi ý cho bạn",
                style = BetterMeTypography.Title.Small.Bold,
                color = BetterMeColors.Text.TextPrimary,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(Modifier.height(12.dp))
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            derived.forEach { line ->
                InsightRow(line)
            }
        }
    }
}

private data class InsightLine(
    val emoji: String,
    val text: String,
    val accent: Color
)

@Composable
private fun InsightRow(line: InsightLine) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier
                .size(30.dp)
                .clip(CircleShape)
                .background(line.accent.copy(alpha = BetterMeTokens.AccentAlpha.Soft)),
            contentAlignment = Alignment.Center
        ) {
            Text(text = line.emoji, fontSize = 16.sp)
        }
        Text(
            text = line.text,
            style = BetterMeTypography.Body.Medium,
            color = BetterMeColors.Text.TextPrimary,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(1f)
        )
    }
}
