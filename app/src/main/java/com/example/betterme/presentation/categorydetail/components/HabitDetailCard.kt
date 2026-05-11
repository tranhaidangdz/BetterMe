package com.example.betterme.presentation.categorydetail.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.betterme.presentation.categorydetail.HabitDetailUiModel
import com.example.betterme.presentation.theme.BetterMeColors
import com.example.betterme.presentation.theme.BetterMeTokens
import com.example.betterme.presentation.theme.BetterMeTypography

/**
 * Habit row inside a Category Detail group.
 *
 * Solid white surface with a thin 1dp accent border (alpha = AccentAlpha.Medium),
 * neutral shadow, 20dp corner radius. The previous version stacked a white→soft
 * vertical gradient + accent-tinted shadow + per-row inline alphas; against the
 * pale blue page background that read as "washed out" and made rows feel separate
 * from the rest of the app.
 *
 * The accent now lives in three deliberate places: the border (visual identity),
 * the emoji disc tint, and the progress bar / percentage pill. The card body itself
 * stays neutral so the title is fully readable and the row feels grounded.
 *
 * Completion state: at 100%, the percent pill flips to solid accent + white ✓ glyph,
 * the day-count subtitle reads "Hoàn thành" in accent color, and the bar fills.
 */
@Composable
fun HabitDetailCard(
    habit: HabitDetailUiModel,
    emoji: String = "🎯",
    cardColor: Color = Color.White,  // kept for API compatibility; ignored now
    accentColor: Color = BetterMeColors.Primary.Primary,
    modifier: Modifier = Modifier
) {
    val animatedPercent by animateFloatAsState(
        targetValue = habit.completionPercent.toFloat(),
        animationSpec = tween(700),
        label = "habit_progress"
    )
    val isComplete = habit.completionPercent >= 100

    Row(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = BetterMeTokens.CardElevation.Body,
                shape = RoundedCornerShape(BetterMeTokens.CardRadius.Standard),
                ambientColor = BetterMeTokens.NeutralShadow.Ambient,
                spotColor = BetterMeTokens.NeutralShadow.Spot
            )
            .clip(RoundedCornerShape(BetterMeTokens.CardRadius.Standard))
            .background(Color.White)
            .border(
                width = 1.dp,
                color = accentColor.copy(alpha = BetterMeTokens.AccentAlpha.Medium),
                shape = RoundedCornerShape(BetterMeTokens.CardRadius.Standard)
            )
            .padding(horizontal = 14.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        CategoryEmojiDisc(
            emoji = emoji,
            accent = accentColor,
            size = 46.dp,
            fontSize = 22.sp
        )

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = habit.title,
                style = BetterMeTypography.Title.Small.SemiBold,
                color = BetterMeColors.Text.TextPrimary,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(8.dp))
            LinearProgressBar(
                percent = animatedPercent / 100f,
                color = accentColor,
                height = 6.dp
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = if (isComplete) "Hoàn thành"
                else "${habit.completedDays}/${habit.totalDays} ngày",
                style = BetterMeTypography.Body.Small.Medium,
                color = if (isComplete) accentColor else BetterMeColors.Text.TextTertiary,
                fontWeight = if (isComplete) FontWeight.SemiBold else FontWeight.Medium,
                maxLines = 1
            )
        }

        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(BetterMeTokens.CardRadius.Pill))
                .background(
                    if (isComplete) accentColor
                    else accentColor.copy(alpha = BetterMeTokens.AccentAlpha.Soft)
                )
                .padding(horizontal = 10.dp, vertical = 6.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = if (isComplete) "✓" else "${habit.completionPercent}%",
                style = BetterMeTypography.Body.Small.Medium,
                color = if (isComplete) Color.White else accentColor,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
