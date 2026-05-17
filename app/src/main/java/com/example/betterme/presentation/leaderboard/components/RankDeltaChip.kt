package com.example.betterme.presentation.leaderboard.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
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
import com.example.betterme.domain.leaderboard.RankDelta
import com.example.betterme.presentation.theme.BetterMeColors
import com.example.betterme.presentation.theme.BetterMeTokens
import com.example.betterme.presentation.theme.BetterMeTypography

/**
 * Animated chip that visualises a row's [RankDelta]:
 *  - Up   → "↑+N" green
 *  - Down → "↓-N" red
 *  - New  → "NEW" indigo
 *  - Unchanged → "•" muted dot
 *  - Hidden    → renders nothing.
 *
 * The directional variants slide-and-fade on change so a fresh refresh
 * with rank-shifts feels alive rather than static. AnimatedContent
 * keyed on the [delta] value handles the in/out for free.
 */
@Composable
fun RankDeltaChip(
    delta: RankDelta,
    modifier: Modifier = Modifier
) {
    if (delta is RankDelta.Hidden) return

    AnimatedContent(
        targetState = delta,
        transitionSpec = {
            (slideInVertically { it / 2 } + fadeIn(tween(220))) togetherWith
                (slideOutVertically { -it / 2 } + fadeOut(tween(160)))
        },
        label = "rank_delta_chip",
        modifier = modifier
    ) { current ->
        val (label, color) = when (current) {
            is RankDelta.Up -> "↑+${current.by}" to Color(0xFF16A34A)
            is RankDelta.Down -> "↓-${current.by}" to Color(0xFFDC2626)
            RankDelta.New -> "NEW" to Color(0xFF6366F1)
            RankDelta.Unchanged -> "•" to BetterMeColors.Text.TextTertiary
            RankDelta.Hidden -> "" to Color.Transparent
        }
        if (label.isEmpty()) return@AnimatedContent
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(BetterMeTokens.CardRadius.Pill))
                .background(color.copy(alpha = 0.12f))
                .padding(horizontal = 8.dp, vertical = 3.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = label,
                style = BetterMeTypography.Body.Small.Medium,
                color = color,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}
