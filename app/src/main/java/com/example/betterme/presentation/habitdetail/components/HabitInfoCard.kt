package com.example.betterme.presentation.habitdetail.components

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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.betterme.presentation.theme.BetterMeColors
import com.example.betterme.presentation.theme.BetterMeTypography

// ============================================================
// HABIT INFO CARD — Thông tin thói quen + trạng thái
// ============================================================
@Composable
fun HabitInfoCard(
    title: String,
    categoryName: String,
    categoryIcon: String,
    isCompletedToday: Boolean,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(BetterMeColors.White)
            .padding(16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Category icon
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(BetterMeColors.Primary.PrimaryBackground),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = categoryIcon,
                    style = BetterMeTypography.Headline.Small.Bold
                )
            }

            // Title + category + status — stacked vertically to prevent overflow
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = BetterMeTypography.Title.Small.Bold,
                    color = BetterMeColors.Text.TextPrimary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(6.dp))

                // Category + Status on separate row (wraps naturally)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Category name
                    Text(
                        text = categoryName,
                        style = BetterMeTypography.Body.Small.Medium,
                        color = BetterMeColors.Text.TextTertiary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.widthIn(max = 120.dp)
                    )

                    // Status badge — fixed size, won't push other elements
                    StatusBadge(isCompleted = isCompletedToday)
                }
            }
        }
    }
}

@Composable
private fun StatusBadge(isCompleted: Boolean) {
    val bgColor = if (isCompleted) BetterMeColors.Green.copy(alpha = 0.12f)
    else BetterMeColors.Primary.Primary.copy(alpha = 0.10f)
    val textColor = if (isCompleted) BetterMeColors.Green else BetterMeColors.Primary.Primary

    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(bgColor)
            .padding(horizontal = 8.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(textColor)
        )
        Text(
            text = if (isCompleted) "Đã hoàn thành" else "Đang thực hiện",
            style = BetterMeTypography.Body.Small.Medium,
            color = textColor,
            maxLines = 1
        )
    }
}

// ============================================================
// STREAK CARD — Chuỗi ngày + tiến độ tuần
// ============================================================
@Composable
fun StreakCard(
    currentStreak: Int,
    longestStreak: Int,
    weeklyProgress: Int,
    weeklyTotal: Int,
    modifier: Modifier = Modifier
) {
    val animatedFraction by animateFloatAsState(
        targetValue = if (weeklyTotal > 0) weeklyProgress.toFloat() / weeklyTotal else 0f,
        animationSpec = tween(600),
        label = "weekly_progress"
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(BetterMeColors.White)
            .padding(16.dp)
    ) {
        // Streak numbers
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            StreakItem(
                value = currentStreak.toString(),
                unit = "ngày",
                label = "Chuỗi hiện tại",
                modifier = Modifier.weight(1f)
            )
            Box(
                modifier = Modifier
                    .width(1.dp)
                    .height(56.dp)
                    .background(BetterMeColors.Border.BorderLight)
            )
            StreakItem(
                value = longestStreak.toString(),
                unit = "ngày",
                label = "Chuỗi dài nhất",
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Weekly progress label
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Tiến độ tuần này",
                style = BetterMeTypography.Body.Small.Medium,
                color = BetterMeColors.Text.TextTertiary
            )
            Text(
                text = "$weeklyProgress/$weeklyTotal ngày",
                style = BetterMeTypography.Body.Small.Medium,
                color = BetterMeColors.Text.TextPrimary
            )
        }
        Spacer(modifier = Modifier.height(8.dp))

        // Animated progress bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(BetterMeColors.Gray.Gray4)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(animatedFraction.coerceIn(0f, 1f))
                    .clip(RoundedCornerShape(4.dp))
                    .background(BetterMeColors.Primary.Primary)
            )
        }
    }
}

@Composable
private fun StreakItem(
    value: String,
    unit: String,
    label: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = value,
                style = BetterMeTypography.Headline.Small.Bold,
                color = BetterMeColors.Primary.Primary
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = unit,
                style = BetterMeTypography.Body.Small.Medium,
                color = BetterMeColors.Primary.Primary,
                modifier = Modifier.padding(bottom = 2.dp)
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            style = BetterMeTypography.Body.Small.Medium,
            color = BetterMeColors.Text.TextTertiary,
            textAlign = TextAlign.Center
        )
    }
}
