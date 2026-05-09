package com.example.betterme.presentation.statistics.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.EmojiEvents
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
import com.example.betterme.presentation.statistics.HabitJourney
import com.example.betterme.presentation.theme.BetterMeColors
import com.example.betterme.presentation.theme.BetterMeTypography

@Composable
fun HabitJourneyCard(
    journey: HabitJourney,
    modifier: Modifier = Modifier
) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }

    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(700),
        label = "journeyAlpha"
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
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "Toàn bộ hành trình",
                style = BetterMeTypography.Title.Medium.SemiBold,
                color = BetterMeColors.Text.TextPrimary
            )
            Text(
                text = "Từ ngày 01/01/2025 đến nay",
                style = BetterMeTypography.Body.Small.Medium,
                color = BetterMeColors.Text.TextTertiary,
                modifier = Modifier.padding(top = 2.dp, bottom = 16.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                JourneyStatItem(
                    icon = Icons.Rounded.Add,
                    iconColor = Color(0xFF2196F3),
                    bgColor = Color(0xFFE3F2FD),
                    value = "${journey.totalCreated}",
                    label = "Tạo mới"
                )
                JourneyStatItem(
                    icon = Icons.Rounded.CheckCircle,
                    iconColor = Color(0xFF4CAF50),
                    bgColor = Color(0xFFE8F5E9),
                    value = "${journey.totalCompleted}",
                    label = "Hoàn thành"
                )
                JourneyStatItem(
                    icon = Icons.Rounded.Close,
                    iconColor = Color(0xFFE57373),
                    bgColor = Color(0xFFFCE4EC),
                    value = "${journey.totalFailed}",
                    label = "Thất bại"
                )
                JourneyStatItem(
                    icon = Icons.Rounded.EmojiEvents,
                    iconColor = Color(0xFFFFC107),
                    bgColor = Color(0xFFFFF8E1),
                    value = "${journey.badgesEarned}",
                    label = "Huy hiệu"
                )
                JourneyStatItem(
                    icon = Icons.Rounded.CalendarMonth,
                    iconColor = Color(0xFF9C27B0),
                    bgColor = Color(0xFFF3E5F5),
                    value = "${journey.activeDays}",
                    label = "Ngày hoạt động"
                )
            }
        }
    }
}

@Composable
private fun JourneyStatItem(
    icon: ImageVector,
    iconColor: Color,
    bgColor: Color,
    value: String,
    label: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
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
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = value,
            style = BetterMeTypography.Title.Small.Bold,
            color = BetterMeColors.Text.TextPrimary
        )
        Text(
            text = label,
            style = BetterMeTypography.Body.Small.Medium,
            color = BetterMeColors.Text.TextTertiary,
            maxLines = 1
        )
    }
}
