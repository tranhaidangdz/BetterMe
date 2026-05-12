package com.example.betterme.presentation.dailyhabits.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.betterme.presentation.dailyhabits.DailyHabitFilter
import com.example.betterme.presentation.theme.BetterMeColors
import com.example.betterme.presentation.theme.BetterMeTokens
import com.example.betterme.presentation.theme.BetterMeTypography

/**
 * Filter-aware empty state. Copy + emoji change so the screen never reads as a
 * generic "no data" gray panel:
 *
 * - ALL          → onboarding tone ("hôm nay chưa có nhiệm vụ")
 * - IN_PROGRESS  → celebratory ("bạn đã làm xong hết")
 * - DONE         → encouragement ("chưa hoàn thành nhiệm vụ nào")
 */
@Composable
fun TasksEmptyState(
    filter: DailyHabitFilter,
    modifier: Modifier = Modifier
) {
    val (emoji, headline, subtext, accent) = copyFor(filter)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = BetterMeTokens.CardElevation.Body,
                shape = RoundedCornerShape(BetterMeTokens.CardRadius.Hero),
                ambientColor = BetterMeTokens.NeutralShadow.Ambient,
                spotColor = BetterMeTokens.NeutralShadow.Spot
            )
            .clip(RoundedCornerShape(BetterMeTokens.CardRadius.Hero))
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color.White, accent.copy(alpha = 0.10f))
                )
            )
            .padding(horizontal = 22.dp, vertical = 28.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(accent.copy(alpha = BetterMeTokens.AccentAlpha.Soft)),
                contentAlignment = Alignment.Center
            ) {
                Text(text = emoji, fontSize = 40.sp)
            }
            Spacer(Modifier.height(14.dp))
            Text(
                text = headline,
                style = BetterMeTypography.Title.Small.Bold,
                color = BetterMeColors.Text.TextPrimary,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = subtext,
                style = BetterMeTypography.Body.Small.Medium,
                color = BetterMeColors.Text.TextSecondary,
                textAlign = TextAlign.Center
            )
        }
    }
}

private data class EmptyCopy(
    val emoji: String,
    val headline: String,
    val subtext: String,
    val accent: Color
)

private fun copyFor(filter: DailyHabitFilter): EmptyCopy = when (filter) {
    DailyHabitFilter.ALL -> EmptyCopy(
        emoji = "🌱",
        headline = "Chưa có nhiệm vụ nào",
        subtext = "Thêm thói quen đầu tiên để bắt đầu ngày của bạn.",
        accent = BetterMeColors.Primary.Primary
    )
    DailyHabitFilter.IN_PROGRESS -> EmptyCopy(
        emoji = "🎉",
        headline = "Tuyệt vời!",
        subtext = "Bạn đã hoàn thành mọi nhiệm vụ cho hôm nay. Tận hưởng nhé.",
        accent = Color(0xFF16A34A)
    )
    DailyHabitFilter.DONE -> EmptyCopy(
        emoji = "💪",
        headline = "Bắt đầu nhỏ nhé",
        subtext = "Hoàn thành 1 nhiệm vụ đầu tiên sẽ tạo đà cho cả ngày.",
        accent = BetterMeColors.Primary.Primary
    )
}
