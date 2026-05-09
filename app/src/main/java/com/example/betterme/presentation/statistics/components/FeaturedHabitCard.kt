package com.example.betterme.presentation.statistics.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.betterme.presentation.statistics.FeaturedHabit
import com.example.betterme.presentation.theme.BetterMeColors
import com.example.betterme.presentation.theme.BetterMeTypography

@Composable
fun FeaturedHabitCard(
    habit: FeaturedHabit,
    index: Int,
    modifier: Modifier = Modifier
) {
    var animateProgress by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { animateProgress = true }

    val progressAnim by animateFloatAsState(
        targetValue = if (animateProgress) habit.completionPercentage / 100f else 0f,
        animationSpec = tween(
            durationMillis = 800,
            delayMillis = index * 100
        ),
        label = "progress_$index"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = 3.dp,
                shape = RoundedCornerShape(16.dp),
                ambientColor = Color(0x08000000),
                spotColor = Color(0x14000000)
            )
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White)
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Habit icon
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFFF0F4FF)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = habit.icon,
                    style = BetterMeTypography.Title.Large.Medium
                )
            }

            // Title + progress bar + info
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = habit.title,
                        style = BetterMeTypography.Title.Small.SemiBold,
                        color = BetterMeColors.Text.TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "${habit.completionPercentage}%",
                        style = BetterMeTypography.Title.Small.Bold,
                        color = getProgressColor(habit.completionPercentage)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                LinearProgressIndicator(
                    progress = { progressAnim },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = getProgressColor(habit.completionPercentage),
                    trackColor = Color(0xFFF0F0F0),
                    strokeCap = StrokeCap.Round
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "Hoàn thành ${habit.completedDays}/${habit.totalDays} ngày",
                    style = BetterMeTypography.Body.Small.Medium,
                    color = BetterMeColors.Text.TextTertiary
                )
            }
        }
    }
}

private fun getProgressColor(percentage: Int): Color {
    return when {
        percentage >= 80 -> Color(0xFF4CAF50)
        percentage >= 60 -> Color(0xFF2196F3)
        percentage >= 40 -> Color(0xFFFF9800)
        else -> Color(0xFFE57373)
    }
}
