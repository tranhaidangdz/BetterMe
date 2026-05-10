package com.example.betterme.presentation.home.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.betterme.presentation.home.model.HomeProgress
import com.example.betterme.presentation.theme.BetterMeColors
import com.example.betterme.presentation.theme.BetterMeTypography

@Composable
fun HomeProgressCard(
    progress: HomeProgress,
    onViewProgress: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val animatedProgress by animateFloatAsState(
        targetValue = progress.percentage / 100f,
        animationSpec = tween(durationMillis = 1000),
        label = "progress"
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(
                Brush.horizontalGradient(BetterMeColors.Gradient.HomeCard)
            )
            .padding(20.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                val statusText = when {
                    progress.totalHabits == 0 -> "Chưa có nhiệm vụ\nnào hôm nay"
                    progress.percentage >= 100 -> "Tất cả nhiệm vụ\nđã hoàn thành! 🎉"
                    progress.completedHabits > 0 -> "${progress.completedHabits}/${progress.totalHabits} nhiệm vụ\nđã hoàn thành!"
                    else -> "Nhiệm vụ hôm nay\nchờ bạn hoàn thành!"
                }
                Text(
                    text = statusText,
                    style = BetterMeTypography.Title.Small.Bold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(12.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(BetterMeColors.White15)
                        .clickable { onViewProgress() }
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = "Xem tiến độ",
                        style = BetterMeTypography.Body.Small.Medium,
                        color = Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(80.dp)
            ) {
                Canvas(modifier = Modifier.size(80.dp)) {
                    val strokeWidth = 8.dp.toPx()
                    val arcSize = size.minDimension - strokeWidth
                    drawArc(
                        color = Color.White.copy(alpha = 0.2f),
                        startAngle = 0f, sweepAngle = 360f,
                        useCenter = false,
                        topLeft = Offset(strokeWidth / 2, strokeWidth / 2),
                        size = Size(arcSize, arcSize),
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                    )
                    drawArc(
                        color = Color.White,
                        startAngle = -90f,
                        sweepAngle = animatedProgress * 360f,
                        useCenter = false,
                        topLeft = Offset(strokeWidth / 2, strokeWidth / 2),
                        size = Size(arcSize, arcSize),
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                    )
                }
                Text(
                    text = "${progress.percentage}%",
                    style = BetterMeTypography.Title.Medium.Bold,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )
            }
        }

        // Today's challenge check-in tracker — only shown when the user has at least one
        // active challenge. Sits below the habit progress so the existing layout stays.
        if (progress.totalChallenges > 0) {
            Spacer(modifier = Modifier.height(16.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(Color.White.copy(alpha = 0.18f))
            )
            Spacer(modifier = Modifier.height(12.dp))
            ChallengeCheckInTracker(
                checkedIn = progress.checkedInChallenges,
                total = progress.totalChallenges,
                percentage = progress.challengePercentage
            )
        }
    }
}

@Composable
private fun ChallengeCheckInTracker(checkedIn: Int, total: Int, percentage: Int) {
    val animatedFraction by animateFloatAsState(
        targetValue = (percentage / 100f).coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 800),
        label = "challenge-progress"
    )
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = "🏆", style = BetterMeTypography.Title.Small.Bold)
        Spacer(modifier = Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            val label = when {
                checkedIn == 0 -> "Hôm nay $total thử thách đang chờ check-in"
                checkedIn >= total -> "Tuyệt vời! Đã check-in tất cả $total thử thách 🎉"
                else -> "Đã check-in $checkedIn/$total thử thách hôm nay"
            }
            Text(
                text = label,
                style = BetterMeTypography.Body.Small.Medium,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(6.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(Color.White.copy(alpha = 0.22f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(animatedFraction)
                        .fillMaxSize()
                        .clip(RoundedCornerShape(999.dp))
                        .background(Color.White)
                )
            }
        }
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = "$percentage%",
            style = BetterMeTypography.Title.Small.Bold,
            color = Color.White
        )
    }
}
