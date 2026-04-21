package com.example.betterme.presentation.dailyhabits.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
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
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(BetterMeColors.White)
            .border(
                width = 1.dp,
                color = BetterMeColors.Primary.Primary.copy(alpha = 0.35f),
                shape = RoundedCornerShape(14.dp)
            )
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
            Text(
                text = "⏰ ${habit.time}",
                style = BetterMeTypography.Body.Small.Medium,
                color = BetterMeColors.Primary.Primary
            )
            if (habit.daysRemaining != null) {
                Text(
                    text = "Còn ${habit.daysRemaining} ngày đến hạn",
                    style = BetterMeTypography.Body.Small.Medium,
                    color = BetterMeColors.Text.TextTertiary
                )
            }
        }

        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = habit.icon,
                style = BetterMeTypography.Headline.Small.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(
                        if (habit.isCompleted) {
                            BetterMeColors.Primary.Primary.copy(alpha = 0.20f)
                        } else {
                            BetterMeColors.Primary.Primary.copy(alpha = 0.10f)
                        }
                    )
                    .padding(horizontal = 10.dp, vertical = 5.dp)
            ) {
                Text(
                    text = habit.statusLabel,
                    style = BetterMeTypography.Body.Small.Medium,
                    color = BetterMeColors.Primary.Primary
                )
            }
        }
    }
}
