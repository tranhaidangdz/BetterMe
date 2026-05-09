package com.example.betterme.presentation.statistics.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.betterme.presentation.statistics.OverviewStats
import com.example.betterme.presentation.theme.BetterMeColors
import com.example.betterme.presentation.theme.BetterMeTypography

private val CompletedColor = Color(0xFF4CAF50)
private val FailedColor = Color(0xFFE57373)
private val OngoingColor = Color(0xFFFF9800)

@Composable
fun PieChartCard(
    stats: OverviewStats,
    modifier: Modifier = Modifier
) {
    val total = (stats.totalCompleted + stats.totalFailed + stats.totalOngoing).coerceAtLeast(1)
    val completedPct = (stats.totalCompleted.toFloat() / total * 100).toInt()
    val failedPct = (stats.totalFailed.toFloat() / total * 100).toInt()
    val ongoingPct = 100 - completedPct - failedPct

    val animatable = remember { Animatable(0f) }
    LaunchedEffect(stats) {
        animatable.snapTo(0f)
        animatable.animateTo(
            targetValue = 1f,
            animationSpec = tween(1200, easing = FastOutSlowInEasing)
        )
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = 6.dp,
                shape = RoundedCornerShape(20.dp),
                ambientColor = Color(0x0D000000),
                spotColor = Color(0x1A000000)
            )
            .clip(RoundedCornerShape(20.dp))
            .background(Color.White)
            .padding(24.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Phân bổ trạng thái thói quen",
                style = BetterMeTypography.Title.Medium.SemiBold,
                color = BetterMeColors.Text.TextPrimary,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 20.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Donut chart
                Box(
                    modifier = Modifier.size(160.dp),
                    contentAlignment = Alignment.Center
                ) {
                    val sweepProgress = animatable.value

                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val strokeWidth = 28.dp.toPx()
                        val arcSize = size.minDimension - strokeWidth
                        val topLeft = androidx.compose.ui.geometry.Offset(
                            (size.width - arcSize) / 2f,
                            (size.height - arcSize) / 2f
                        )
                        val arcSizeObj = androidx.compose.ui.geometry.Size(arcSize, arcSize)

                        val completedSweep = (stats.totalCompleted.toFloat() / total) * 360f * sweepProgress
                        val failedSweep = (stats.totalFailed.toFloat() / total) * 360f * sweepProgress
                        val ongoingSweep = (stats.totalOngoing.toFloat() / total) * 360f * sweepProgress

                        // Completed arc
                        drawArc(
                            color = CompletedColor,
                            startAngle = -90f,
                            sweepAngle = completedSweep,
                            useCenter = false,
                            topLeft = topLeft,
                            size = arcSizeObj,
                            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                        )

                        // Failed arc
                        drawArc(
                            color = FailedColor,
                            startAngle = -90f + completedSweep,
                            sweepAngle = failedSweep,
                            useCenter = false,
                            topLeft = topLeft,
                            size = arcSizeObj,
                            style = Stroke(width = strokeWidth, cap = StrokeCap.Butt)
                        )

                        // Ongoing arc
                        drawArc(
                            color = OngoingColor,
                            startAngle = -90f + completedSweep + failedSweep,
                            sweepAngle = ongoingSweep,
                            useCenter = false,
                            topLeft = topLeft,
                            size = arcSizeObj,
                            style = Stroke(width = strokeWidth, cap = StrokeCap.Butt)
                        )
                    }

                    // Center text
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "$total",
                            style = BetterMeTypography.Headline.Small.Bold,
                            color = BetterMeColors.Text.TextPrimary,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = "Tổng số thói quen",
                            style = BetterMeTypography.Body.Small.Medium,
                            color = BetterMeColors.Text.TextTertiary,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                // Legend
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    LegendItem(
                        color = CompletedColor,
                        label = "Hoàn thành",
                        count = stats.totalCompleted,
                        percentage = completedPct
                    )
                    LegendItem(
                        color = FailedColor,
                        label = "Thất bại",
                        count = stats.totalFailed,
                        percentage = failedPct
                    )
                    LegendItem(
                        color = OngoingColor,
                        label = "Đang thực hiện",
                        count = stats.totalOngoing,
                        percentage = ongoingPct
                    )
                }
            }
        }
    }
}

@Composable
private fun LegendItem(
    color: Color,
    label: String,
    count: Int,
    percentage: Int
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .clip(CircleShape)
                .background(color)
        )
        Column {
            Text(
                text = label,
                style = BetterMeTypography.Body.Small.Medium,
                color = BetterMeColors.Text.TextPrimary
            )
            Text(
                text = "$count ($percentage%)",
                style = BetterMeTypography.Body.Small.Medium,
                color = BetterMeColors.Text.TextTertiary
            )
        }
    }
}
