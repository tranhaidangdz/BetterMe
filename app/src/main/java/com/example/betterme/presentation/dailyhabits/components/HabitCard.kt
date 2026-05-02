package com.example.betterme.presentation.dailyhabits.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.betterme.presentation.dailyhabits.model.HabitUiModel
import com.example.betterme.presentation.theme.BetterMeColors
import com.example.betterme.presentation.theme.BetterMeTypography

@Composable
fun HabitCard(
    habit: HabitUiModel,
    onToggleCompletion: () -> Unit = {},
    onCardClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(BetterMeColors.White)
            .border(
                width = 1.dp,
                color = if (habit.isCompleted) BetterMeColors.Green.copy(alpha = 0.35f)
                else BetterMeColors.Primary.Primary.copy(alpha = 0.35f),
                shape = RoundedCornerShape(14.dp)
            )
            .clickable { onCardClick() }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Text(
                text = habit.category,
                style = BetterMeTypography.Body.Small.Medium,
                color = BetterMeColors.Text.TextTertiary
            )
            Text(
                text = habit.title,
                style = BetterMeTypography.Body.Medium.copy(fontWeight = FontWeight.SemiBold),
                color = BetterMeColors.Text.TextPrimary
            )
            if (habit.time.isNotBlank()) {
                Text(
                    text = "⏰ ${habit.time}",
                    style = BetterMeTypography.Body.Small.Medium,
                    color = BetterMeColors.Primary.Primary
                )
            }
        }

        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = habit.icon,
                style = BetterMeTypography.Headline.Small.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))

            // Check-in button
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(
                        if (habit.isCompleted) BetterMeColors.Green
                        else BetterMeColors.Gray.Gray4
                    )
                    .clickable { onToggleCompletion() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = if (habit.isCompleted) "Hoàn thành" else "Chưa hoàn thành",
                    tint = if (habit.isCompleted) BetterMeColors.White
                    else BetterMeColors.Text.TextTertiary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}
