package com.example.betterme.presentation.statistics.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.automirrored.rounded.TrendingUp
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.example.betterme.presentation.statistics.OverviewStats
import com.example.betterme.presentation.theme.BetterMeColors
import com.example.betterme.presentation.theme.BetterMeTypography

@Composable
fun OverviewCards(
    stats: OverviewStats,
    modifier: Modifier = Modifier
) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }

    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(600),
        label = "overviewAlpha"
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .alpha(alpha)
    ) {
        Text(
            text = "Tổng quan tuần này",
            style = BetterMeTypography.Title.Medium.SemiBold,
            color = BetterMeColors.Text.TextPrimary,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OverviewStatCard(
                modifier = Modifier.weight(1f),
                icon = Icons.AutoMirrored.Rounded.TrendingUp,
                iconColor = Color(0xFF4A90D9),
                bgColor = Color(0xFFE8F4FD),
                value = "${stats.completionRate}%",
                label = "Tỷ lệ hoàn thành",
                trendText = "↑ 12%"
            )
            OverviewStatCard(
                modifier = Modifier.weight(1f),
                icon = Icons.Rounded.CheckCircle,
                iconColor = Color(0xFF4CAF50),
                bgColor = Color(0xFFE8F5E9),
                value = "${stats.totalCompleted}",
                label = "Thói quen hoàn thành"
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OverviewStatCard(
                modifier = Modifier.weight(1f),
                icon = Icons.Rounded.Close,
                iconColor = Color(0xFFE57373),
                bgColor = Color(0xFFFCE4EC),
                value = "${stats.totalFailed}",
                label = "Thói quen thất bại"
            )
            OverviewStatCard(
                modifier = Modifier.weight(1f),
                icon = Icons.Rounded.Refresh,
                iconColor = Color(0xFFFF9800),
                bgColor = Color(0xFFFFF3E0),
                value = "${stats.totalOngoing}",
                label = "Đang thực hiện"
            )
        }
    }
}

@Composable
private fun OverviewStatCard(
    icon: ImageVector,
    iconColor: Color,
    bgColor: Color,
    value: String,
    label: String,
    trendText: String? = null,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .shadow(
                elevation = 4.dp,
                shape = RoundedCornerShape(16.dp),
                ambientColor = Color(0x0D000000),
                spotColor = Color(0x1A000000)
            )
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White)
            .padding(16.dp)
    ) {
        Column {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(bgColor),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconColor,
                        modifier = Modifier.size(20.dp)
                    )
                }
                if (trendText != null) {
                    Text(
                        text = trendText,
                        style = BetterMeTypography.Body.Small.Medium,
                        color = Color(0xFF4CAF50)
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = value,
                style = BetterMeTypography.Title.Large.Bold,
                color = BetterMeColors.Text.TextPrimary
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = label,
                style = BetterMeTypography.Body.Small.Medium,
                color = BetterMeColors.Text.TextTertiary,
                maxLines = 1
            )
        }
    }
}
