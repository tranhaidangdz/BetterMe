package com.example.betterme.presentation.leaderboard.global.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
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
import com.example.betterme.domain.leaderboard.LeagueTier
import com.example.betterme.presentation.theme.BetterMeTokens
import com.example.betterme.presentation.theme.BetterMeTypography

/**
 * Compact tier badge (emoji + name) used in row corners + the profile
 * sheet. Color palette is local to the UI layer — kept off
 * [LeagueTier] so the domain stays Compose-free.
 */
@Composable
fun LeagueTierBadge(
    tier: LeagueTier,
    modifier: Modifier = Modifier,
    compact: Boolean = false
) {
    val tint = tier.tint()
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(BetterMeTokens.CardRadius.Pill))
            .background(tint.copy(alpha = 0.18f))
            .padding(horizontal = if (compact) 8.dp else 10.dp, vertical = if (compact) 3.dp else 5.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = tier.emoji, style = BetterMeTypography.Body.Small.Medium)
            if (!compact) {
                Text(
                    text = "  ${tier.displayName}",
                    style = BetterMeTypography.Body.Small.Medium,
                    color = tint,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

/** Tier palette colocated with the UI. */
fun LeagueTier.tint(): Color = when (this) {
    LeagueTier.BRONZE -> Color(0xFFA8702E)
    LeagueTier.SILVER -> Color(0xFF7C8493)
    LeagueTier.GOLD -> Color(0xFFB7791F)
    LeagueTier.PLATINUM -> Color(0xFF0EA5E9)
    LeagueTier.DIAMOND -> Color(0xFF6366F1)
    LeagueTier.MASTER -> Color(0xFF8B5CF6)
}
