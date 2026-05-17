package com.example.betterme.presentation.leaderboard.global.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.ui.graphics.Color
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.example.betterme.domain.leaderboard.MonthlyWinner
import com.example.betterme.presentation.leaderboard.components.RankBadgeRow
import com.example.betterme.presentation.theme.BetterMeColors
import com.example.betterme.presentation.theme.BetterMeTokens
import com.example.betterme.presentation.theme.BetterMeTypography

/**
 * Card for one season's champion + runners-up. Renders the season
 * label as a header and a podium-style three-row body.
 */
@Composable
fun MonthlyWinnerCard(
    seasonLabel: String,
    winners: List<MonthlyWinner>,
    modifier: Modifier = Modifier
) {
    if (winners.isEmpty()) return
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(BetterMeTokens.CardRadius.Hero))
            .background(BetterMeColors.BackGround.BackgroundSecondary)
            .border(
                width = 1.dp,
                color = BetterMeColors.Border.BorderLight,
                shape = RoundedCornerShape(BetterMeTokens.CardRadius.Hero)
            )
            .padding(14.dp)
    ) {
        Text(
            text = seasonLabel,
            style = BetterMeTypography.Title.Small.Bold,
            color = BetterMeColors.Text.TextPrimary,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(10.dp))
        winners.forEachIndexed { idx, w ->
            WinnerRow(winner = w)
            if (idx < winners.lastIndex) Spacer(Modifier.height(6.dp))
        }
    }
}

@Composable
private fun WinnerRow(winner: MonthlyWinner) {
    val tint = winner.leagueTier.tint()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(BetterMeTokens.CardRadius.Body))
            .background(podiumTintFor(winner.rank))
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = medalFor(winner.rank),
            fontSize = 22.sp
        )
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(BetterMeColors.Gray.Gray3)
        ) {
            winner.avatarUrl?.let {
                AsyncImage(
                    model = it,
                    contentDescription = winner.displayName,
                    modifier = Modifier.size(34.dp).clip(CircleShape)
                )
            }
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = winner.displayName,
                style = BetterMeTypography.Body.Medium,
                color = BetterMeColors.Text.TextPrimary,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1
            )
            if (winner.badges.isNotEmpty()) {
                Spacer(Modifier.height(2.dp))
                RankBadgeRow(badges = winner.badges, maxVisible = 2)
            }
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = "${winner.totalScore}",
                style = BetterMeTypography.Body.Medium,
                color = tint,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "điểm",
                style = BetterMeTypography.Body.Small.Medium,
                color = BetterMeColors.Text.TextTertiary
            )
        }
    }
}

private fun medalFor(rank: Int): String = when (rank) {
    1 -> "🥇"
    2 -> "🥈"
    3 -> "🥉"
    else -> "#$rank"
}

/** Podium row tint per rank — keeps the gold/silver/bronze palette
 *  consistent with the per-challenge leaderboard. */
private fun podiumTintFor(rank: Int): Color = when (rank) {
    1 -> Color(0xFFFFF4D6)
    2 -> Color(0xFFEBEEF3)
    3 -> Color(0xFFF6E2CF)
    else -> Color.White
}
