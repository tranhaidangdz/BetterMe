package com.example.betterme.presentation.habitdetail.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.betterme.presentation.theme.BetterMeColors
import com.example.betterme.presentation.theme.BetterMeTypography

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
            .clip(RoundedCornerShape(16.dp))
            .background(BetterMeColors.White)
            .padding(16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Icon
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(BetterMeColors.Primary.PrimaryBackground),
                contentAlignment = Alignment.Center
            ) {
                Text(text = categoryIcon, style = BetterMeTypography.Title.Medium.Bold)
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = BetterMeTypography.Title.Small.Bold,
                    color = BetterMeColors.Text.TextPrimary,
                    maxLines = 2
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = categoryName,
                        style = BetterMeTypography.Body.Small.Medium,
                        color = BetterMeColors.Text.TextTertiary
                    )
                    // Status badge
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(999.dp))
                            .background(
                                if (isCompletedToday) BetterMeColors.Green.copy(alpha = 0.15f)
                                else BetterMeColors.Primary.Primary.copy(alpha = 0.12f)
                            )
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = if (isCompletedToday) "✅ Đã hoàn thành" else "⏳ Đang thực hiện",
                            style = BetterMeTypography.Body.Small.Medium,
                            color = if (isCompletedToday) BetterMeColors.Green else BetterMeColors.Primary.Primary
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun StreakCard(
    currentStreak: Int,
    longestStreak: Int,
    weeklyProgress: Int,
    weeklyTotal: Int,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(BetterMeColors.White)
            .padding(16.dp)
    ) {
        // Streak numbers
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            StreakItem(value = "$currentStreak ngày", label = "Chuỗi hiện tại")
            Box(
                modifier = Modifier
                    .width(1.dp)
                    .height(48.dp)
                    .background(BetterMeColors.Border.BorderLight)
            )
            StreakItem(value = "$longestStreak ngày", label = "Chuỗi dài nhất")
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Weekly progress bar
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
        // Progress bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(BetterMeColors.Gray.Gray4)
        ) {
            val fraction = if (weeklyTotal > 0) weeklyProgress.toFloat() / weeklyTotal else 0f
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(fraction.coerceIn(0f, 1f))
                    .clip(RoundedCornerShape(3.dp))
                    .background(BetterMeColors.Primary.Primary)
            )
        }
    }
}

@Composable
private fun StreakItem(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = BetterMeTypography.Title.Medium.Bold,
            color = BetterMeColors.Primary.Primary,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label,
            style = BetterMeTypography.Body.Small.Medium,
            color = BetterMeColors.Text.TextTertiary,
            textAlign = TextAlign.Center
        )
    }
}
