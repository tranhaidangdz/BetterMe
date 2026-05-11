package com.example.betterme.presentation.categorydetail.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.betterme.presentation.categorydetail.HabitDetailUiModel
import com.example.betterme.presentation.theme.BetterMeColors
import com.example.betterme.presentation.theme.BetterMeTypography

/**
 * Habit row inside a Category Detail group.
 *
 * White surface with a soft accent-tinted vertical gradient (matches HomeCard /
 * CantMissCard / CategorySummaryCard so the whole flow reads as one design system),
 * an emoji disc on the left, the habit title with day-count subtitle, an animated
 * pill-style progress bar, and a percentage pill on the right.
 *
 * Completion state: when `completionPercent == 100`, the percentage pill flips to
 * solid accent with a white check glyph and the day-count subtitle reads "Hoàn thành".
 * The bar fills 100% with the accent color so the row still reads as "done" at a
 * glance even after the user scrolls past.
 */
@Composable
fun HabitDetailCard(
    habit: HabitDetailUiModel,
    emoji: String = "🎯",
    cardColor: Color = Color.White,
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
                elevation = 3.dp,
                shape = RoundedCornerShape(18.dp),
                ambientColor = accentColor.copy(alpha = 0.16f),
                spotColor = accentColor.copy(alpha = 0.20f)
            )
            .clip(RoundedCornerShape(18.dp))
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color.White,
                        cardColor.copy(alpha = 0.65f)
                    )
                )
            )
            .padding(horizontal = 14.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Emoji disc — soft gradient identity, matches the Home design language.
        Box(
            modifier = Modifier
                .size(46.dp)
                .clip(CircleShape)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            accentColor.copy(alpha = 0.28f),
                            accentColor.copy(alpha = 0.14f)
                        )
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(text = emoji, fontSize = 22.sp)
        }

        // Title + progress block
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

        // Percentage pill — flips to solid accent + ✓ glyph at 100%.
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(999.dp))
                .background(
                    if (isComplete) accentColor else accentColor.copy(alpha = 0.14f)
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
