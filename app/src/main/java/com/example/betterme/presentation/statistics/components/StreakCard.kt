package com.example.betterme.presentation.statistics.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.automirrored.rounded.ShowChart
import androidx.compose.material.icons.rounded.Timer
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
import com.example.betterme.presentation.statistics.StreakAnalytics
import com.example.betterme.presentation.theme.BetterMeColors
import com.example.betterme.presentation.theme.BetterMeTypography

@Composable
fun StreakCard(
    streaks: StreakAnalytics,
    modifier: Modifier = Modifier
) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }

    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(800),
        label = "streakAlpha"
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .alpha(alpha)
    ) {
        Text(
            text = "Chuỗi (Streak) hiện tại",
            style = BetterMeTypography.Title.Medium.SemiBold,
            color = BetterMeColors.Text.TextPrimary,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            StreakStatItem(
                modifier = Modifier.weight(1f),
                icon = Icons.Rounded.LocalFireDepartment,
                iconColor = Color(0xFFFF5722),
                bgColor = Color(0xFFFBE9E7),
                value = "${streaks.longestStreak}",
                label = "Dài nhất",
                subtitle = "Thiền 10 phút"
            )
            StreakStatItem(
                modifier = Modifier.weight(1f),
                icon = Icons.AutoMirrored.Rounded.ShowChart,
                iconColor = Color(0xFF4A90D9),
                bgColor = Color(0xFFE3F2FD),
                value = "${streaks.averageStreak}",
                label = "Trung bình",
                subtitle = "${streaks.averageStreak} ngày"
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            StreakStatItem(
                modifier = Modifier.weight(1f),
                icon = Icons.Rounded.Timer,
                iconColor = Color(0xFF9C27B0),
                bgColor = Color(0xFFF3E5F5),
                value = "${streaks.currentStreak}",
                label = "Hiện tại",
                subtitle = "Đang hoạt động"
            )
            StreakStatItem(
                modifier = Modifier.weight(1f),
                icon = Icons.Rounded.CalendarMonth,
                iconColor = Color(0xFF00897B),
                bgColor = Color(0xFFE0F2F1),
                value = "${streaks.totalStreakDays}",
                label = "Tổng chuỗi",
                subtitle = "${streaks.totalStreakDays} ngày"
            )
        }
    }
}

@Composable
private fun StreakStatItem(
    icon: ImageVector,
    iconColor: Color,
    bgColor: Color,
    value: String,
    label: String,
    subtitle: String,
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
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(bgColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(22.dp)
                )
            }
            Column {
                Text(
                    text = label,
                    style = BetterMeTypography.Body.Small.Medium,
                    color = BetterMeColors.Text.TextTertiary
                )
                Text(
                    text = "$value ngày",
                    style = BetterMeTypography.Title.Small.Bold,
                    color = BetterMeColors.Text.TextPrimary
                )
                Text(
                    text = subtitle,
                    style = BetterMeTypography.Body.Small.Medium,
                    color = BetterMeColors.Text.TextTertiary,
                    maxLines = 1
                )
            }
        }
    }
}
