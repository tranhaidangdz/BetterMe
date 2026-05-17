package com.example.betterme.presentation.share.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.betterme.domain.share.VerifiedShare
import com.example.betterme.presentation.theme.BetterMeColors
import com.example.betterme.presentation.theme.BetterMeTokens
import com.example.betterme.presentation.theme.BetterMeTypography

/**
 * Stat grid used by both the share viewer + public profile screens.
 *
 * Four cells in a single row at standard density (phone widths
 * 360-420dp): Check-in / Streak / Best streak / Trophies. On
 * narrower screens the grid still fits because each cell's content
 * is short — the parent Row clips gracefully via fillMaxWidth.
 */
@Composable
fun ShareSummaryGrid(
    share: VerifiedShare,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        StatCell(
            label = "Check-in",
            value = "${share.summary.totalCheckIns}",
            modifier = Modifier.weight(1f)
        )
        StatCell(
            label = "Chuỗi hiện tại",
            value = "${share.summary.currentStreakDays}🔥",
            modifier = Modifier.weight(1f)
        )
        StatCell(
            label = "Kỷ lục",
            value = "${share.summary.longestStreakDays}",
            modifier = Modifier.weight(1f)
        )
        StatCell(
            label = "Thử thách",
            value = "${share.summary.completedChallenges}🏆",
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun StatCell(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(BetterMeTokens.CardRadius.Body))
            .background(Color.White)
            .border(
                width = 1.dp,
                color = BetterMeColors.Border.BorderLight,
                shape = RoundedCornerShape(BetterMeTokens.CardRadius.Body)
            )
            .padding(vertical = 14.dp, horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            style = BetterMeTypography.Title.Small.Bold,
            color = BetterMeColors.Text.TextPrimary,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = label,
            style = BetterMeTypography.Body.Small.Medium,
            color = BetterMeColors.Text.TextTertiary
        )
    }
}
