package com.example.betterme.presentation.leaderboard.global.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.betterme.domain.leaderboard.LeagueProgress
import com.example.betterme.presentation.theme.BetterMeColors
import com.example.betterme.presentation.theme.BetterMeTokens
import com.example.betterme.presentation.theme.BetterMeTypography

/**
 * Horizontal tier progress bar shown on the user's own row + the
 * profile sheet. Animates fill width so promotion progress feels
 * tangible across refreshes.
 *
 * Surface is intentionally thin (8.dp tall) — it's a supporting
 * indicator, not the visual centerpiece.
 */
@Composable
fun LeagueProgressBar(
    progress: LeagueProgress,
    modifier: Modifier = Modifier
) {
    val tint = progress.tier.tint()
    val animatedPct by animateFloatAsState(
        targetValue = (progress.progressPercent.coerceIn(0, 100) / 100f),
        animationSpec = tween(durationMillis = 700),
        label = "tier_progress"
    )
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            LeagueTierBadge(tier = progress.tier, compact = true)
            Text(
                text = if (progress.pointsToNextTier > 0)
                    "Còn ${progress.pointsToNextTier} điểm để lên hạng tiếp"
                else "Bạn đang ở đỉnh hạng!",
                style = BetterMeTypography.Body.Small.Medium,
                color = BetterMeColors.Text.TextTertiary,
                modifier = Modifier.padding(start = 8.dp).fillMaxWidth()
            )
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .padding(top = 6.dp)
                .clip(RoundedCornerShape(BetterMeTokens.CardRadius.Pill))
                .background(tint.copy(alpha = 0.12f))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(animatedPct)
                    .clip(RoundedCornerShape(BetterMeTokens.CardRadius.Pill))
                    .background(tint)
            )
        }
    }
}
