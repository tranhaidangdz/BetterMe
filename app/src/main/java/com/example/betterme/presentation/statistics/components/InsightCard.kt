package com.example.betterme.presentation.statistics.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccessTime
import androidx.compose.material.icons.rounded.CalendarToday
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.LocalFireDepartment
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
import com.example.betterme.presentation.statistics.AdditionalInsights
import com.example.betterme.presentation.theme.BetterMeColors
import com.example.betterme.presentation.theme.BetterMeTypography

@Composable
fun InsightCard(
    insights: AdditionalInsights,
    modifier: Modifier = Modifier
) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }

    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(800),
        label = "insightAlpha"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .alpha(alpha)
            .shadow(
                elevation = 6.dp,
                shape = RoundedCornerShape(20.dp),
                ambientColor = Color(0x0D000000),
                spotColor = Color(0x1A000000)
            )
            .clip(RoundedCornerShape(20.dp))
            .background(Color.White)
            .padding(20.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "Thống kê bổ sung",
                style = BetterMeTypography.Title.Medium.SemiBold,
                color = BetterMeColors.Text.TextPrimary,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                InsightItem(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Rounded.CalendarToday,
                    iconColor = Color(0xFF2196F3),
                    bgColor = Color(0xFFE3F2FD),
                    label = "Giờ hoạt động nhiều nhất",
                    value = insights.mostActiveHour
                )
                InsightItem(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Rounded.AccessTime,
                    iconColor = Color(0xFF9C27B0),
                    bgColor = Color(0xFFF3E5F5),
                    label = "Ngày về sinh nhật",
                    value = insights.mostProductiveDay
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                InsightItem(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Rounded.LocalFireDepartment,
                    iconColor = Color(0xFFFF5722),
                    bgColor = Color(0xFFFBE9E7),
                    label = "Chuỗi dài nhất",
                    value = "${insights.bestStreak} ngày"
                )
                InsightItem(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Rounded.EmojiEvents,
                    iconColor = Color(0xFFFFC107),
                    bgColor = Color(0xFFFFF8E1),
                    label = "Tổng lần hoàn thành",
                    value = "${insights.totalCompletions} lần"
                )
            }
        }
    }
}

@Composable
private fun InsightItem(
    icon: ImageVector,
    iconColor: Color,
    bgColor: Color,
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFFF8F9FC))
            .padding(14.dp)
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
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = value,
            style = BetterMeTypography.Title.Small.Bold,
            color = BetterMeColors.Text.TextPrimary
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label,
            style = BetterMeTypography.Body.Small.Medium,
            color = BetterMeColors.Text.TextTertiary,
            maxLines = 2
        )
    }
}
