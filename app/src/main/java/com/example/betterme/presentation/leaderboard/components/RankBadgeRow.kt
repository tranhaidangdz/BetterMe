package com.example.betterme.presentation.leaderboard.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.betterme.domain.leaderboard.RankBadge
import com.example.betterme.presentation.theme.BetterMeTokens
import com.example.betterme.presentation.theme.BetterMeTypography

/**
 * Horizontal row of badge chips. Renders up to [maxVisible] highest-
 * priority badges so the row never wraps awkwardly.
 *
 * Each badge is a small pill with the emoji only (full label is
 * omitted to save horizontal real estate in the leaderboard row) —
 * users can tap the chip for the label if we wire it up later. Today
 * the visual differentiation is the goal.
 */
@Composable
fun RankBadgeRow(
    badges: List<RankBadge>,
    modifier: Modifier = Modifier,
    maxVisible: Int = 3
) {
    if (badges.isEmpty()) return
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        badges.take(maxVisible).forEach { badge ->
            BadgeChip(badge)
        }
    }
}

@Composable
private fun BadgeChip(badge: RankBadge) {
    val tint = badge.tint()
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(BetterMeTokens.CardRadius.Pill))
            .background(tint.copy(alpha = 0.16f))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(
            text = badge.emoji,
            style = BetterMeTypography.Body.Small.Medium,
            color = tint,
            fontWeight = FontWeight.SemiBold
        )
    }
}

/**
 * Local tint chosen per badge — kept inline rather than on the enum
 * itself because Color is a UI concern and we don't want the domain
 * model to depend on Compose.
 */
private fun RankBadge.tint(): Color = when (this) {
    RankBadge.CHAMPION -> Color(0xFFB7791F)
    RankBadge.TOP_10_PERCENT -> Color(0xFF6366F1)
    RankBadge.CONSISTENCY_KING -> Color(0xFF0EA5E9)
    RankBadge.STREAK_MASTER -> Color(0xFFEA580C)
    RankBadge.FAST_CLIMBER -> Color(0xFF16A34A)
}
