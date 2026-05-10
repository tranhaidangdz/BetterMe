package com.example.betterme.presentation.challenge.overview.components

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.betterme.presentation.challenge.model.UpcomingChallengeUiModel
import com.example.betterme.presentation.challenge.shared.ChallengeIconTile
import com.example.betterme.presentation.challenge.shared.ChipTheme
import com.example.betterme.presentation.challenge.shared.RewardChip
import com.example.betterme.presentation.theme.BetterMeColors
import com.example.betterme.presentation.theme.BetterMeTypography

@Composable
fun UpcomingChallengeRow(
    model: UpcomingChallengeUiModel,
    onToggleReminder: (Int) -> Unit,
    onClick: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = 3.dp,
                shape = RoundedCornerShape(16.dp),
                ambientColor = Color(0x0D000000),
                spotColor = Color(0x1A000000)
            )
            .clip(RoundedCornerShape(16.dp))
            .background(BetterMeColors.White)
            .clickable { onClick(model.challengeId) }
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        ChallengeIconTile(emoji = model.iconEmoji, accentColor = model.accentColor)

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = model.title,
                style = BetterMeTypography.Title.Small.Bold,
                color = BetterMeColors.Text.TextPrimary,
                maxLines = 2
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "Bắt đầu sau ${model.daysUntilStart} ngày",
                style = BetterMeTypography.Body.Small.Medium,
                color = BetterMeColors.Text.TextTertiary
            )
            Spacer(modifier = Modifier.height(8.dp))
            RewardChip(coins = model.rewardCoins, theme = ChipTheme.Light)
        }

        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(
                    if (model.reminderEnabled) BetterMeColors.Primary.PrimaryBackground
                    else BetterMeColors.Gray.Gray3
                )
                .clickable { onToggleReminder(model.challengeId) },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (model.reminderEnabled) Icons.Filled.Notifications
                else Icons.Outlined.Notifications,
                contentDescription = "Nhắc tôi",
                modifier = Modifier.size(20.dp),
                tint = if (model.reminderEnabled) BetterMeColors.Primary.Primary
                else BetterMeColors.Text.TextTertiary
            )
        }
    }
}
