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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.betterme.presentation.categorydetail.components.LinearProgressBar
import com.example.betterme.presentation.challenge.model.ChallengeProgressUiModel
import com.example.betterme.presentation.challenge.shared.ChallengeIconTile
import com.example.betterme.presentation.challenge.shared.ChipTheme
import com.example.betterme.presentation.challenge.shared.DifficultyPill
import com.example.betterme.presentation.challenge.shared.RewardChip
import com.example.betterme.presentation.theme.BetterMeColors
import com.example.betterme.presentation.theme.BetterMeTypography

@Composable
fun ChallengeProgressCard(
    model: ChallengeProgressUiModel,
    onContinue: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = 4.dp,
                shape = RoundedCornerShape(16.dp),
                ambientColor = Color(0x0D000000),
                spotColor = Color(0x1A000000)
            )
            .clip(RoundedCornerShape(16.dp))
            .background(BetterMeColors.White)
            .clickable { onContinue(model.userChallengeId) }
            .padding(14.dp)
    ) {
        // ===== HEADER ROW: icon + title + difficulty pill =====
        Row(
            modifier = Modifier.fillMaxWidth(),
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
                    text = "🔥 Ngày ${model.currentStreak}/${model.targetStreak}",
                    style = BetterMeTypography.Body.Small.Medium,
                    color = BetterMeColors.Text.TextSecondary
                )
            }

            DifficultyPill(model.difficulty)
        }

        Spacer(modifier = Modifier.height(12.dp))

        // ===== PROGRESS =====
        LinearProgressBar(
            percent = model.progressPct / 100f,
            color = model.accentColor,
            modifier = Modifier.fillMaxWidth()
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            RewardChip(
                coins = model.rewardCoins,
                badgeName = model.rewardBadgeName,
                theme = ChipTheme.Light
            )
            Text(
                text = "${model.progressPct}%",
                style = BetterMeTypography.Body.Small.Medium,
                color = BetterMeColors.Text.TextSecondary
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // ===== STATUS ROW: days remaining + today check-in pill =====
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "⏳ Còn ${model.daysRemaining} ngày",
                style = BetterMeTypography.Body.Small.Medium,
                color = BetterMeColors.Text.TextTertiary
            )
            val (statusLabel, statusColor) = if (model.isCheckedInToday) {
                "✓ Đã check-in" to Color(0xFF10B981)
            } else {
                "Chưa check-in hôm nay" to Color(0xFFF59E0B)
            }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(statusColor.copy(alpha = 0.14f))
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text(
                    text = statusLabel,
                    style = BetterMeTypography.Body.Small.Medium,
                    color = statusColor
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // ===== CONTINUE BUTTON =====
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(40.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(BetterMeColors.Primary.Primary)
                .clickable { onContinue(model.userChallengeId) },
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Tiếp tục thử thách →",
                style = BetterMeTypography.Body.Medium,
                color = BetterMeColors.White
            )
        }
    }
}
