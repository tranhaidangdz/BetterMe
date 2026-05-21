package com.example.betterme.presentation.challenge.overview.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import com.example.betterme.presentation.challenge.model.CompletedChallengeUiModel
import com.example.betterme.presentation.challenge.model.TerminalStatus
import com.example.betterme.presentation.challenge.shared.ChallengeIconTile
import com.example.betterme.presentation.challenge.shared.ChipTheme
import com.example.betterme.presentation.challenge.shared.RewardChip
import com.example.betterme.presentation.theme.BetterMeColors
import com.example.betterme.presentation.theme.BetterMeTypography

@Composable
fun CompletedChallengeRow(
    model: CompletedChallengeUiModel,
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
            .clickable { onClick(model.userChallengeId) }
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        ChallengeIconTile(
            emoji = model.iconEmoji,
            accentColor = if (model.isCompleted) model.accentColor else BetterMeColors.Gray.Gray2
        )

        Column(modifier = Modifier.weight(1f)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = model.title,
                    style = BetterMeTypography.Title.Small.Bold,
                    color = if (model.isCompleted) BetterMeColors.Text.TextPrimary
                    else BetterMeColors.Text.TextTertiary,
                    maxLines = 2,
                    modifier = Modifier.weight(1f, fill = false)
                )
                when (model.terminalStatus) {
                    TerminalStatus.Failed -> StatusBadge(text = "Đã thất bại")
                    TerminalStatus.Abandoned -> StatusBadge(text = "Đã bỏ")
                    TerminalStatus.Completed -> Unit
                }
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = when (model.terminalStatus) {
                    TerminalStatus.Completed -> "Hoàn thành ${model.finishedDateLabel}"
                    TerminalStatus.Failed -> "Thất bại ngày ${model.finishedDateLabel}"
                    TerminalStatus.Abandoned -> "Kết thúc ${model.finishedDateLabel}"
                },
                style = BetterMeTypography.Body.Small.Medium,
                color = BetterMeColors.Text.TextTertiary
            )
            if (model.isCompleted && (model.rewardCoins > 0 || model.rewardBadgeName != null)) {
                Spacer(modifier = Modifier.height(8.dp))
                RewardChip(
                    coins = model.rewardCoins.takeIf { it > 0 },
                    badgeName = model.rewardBadgeName,
                    theme = ChipTheme.Light
                )
            }
        }
    }
}

@Composable
private fun StatusBadge(text: String) {
    Text(
        text = text,
        style = BetterMeTypography.Body.Small.Medium,
        color = BetterMeColors.Red,
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(BetterMeColors.Red.copy(alpha = 0.1f))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    )
}
