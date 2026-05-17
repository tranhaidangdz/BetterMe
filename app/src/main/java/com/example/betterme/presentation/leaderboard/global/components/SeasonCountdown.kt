package com.example.betterme.presentation.leaderboard.global.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.betterme.presentation.theme.BetterMeColors
import com.example.betterme.presentation.theme.BetterMeTokens
import com.example.betterme.presentation.theme.BetterMeTypography

/**
 * Strip that surfaces season urgency at the top of the screen.
 *
 *   "Còn 3 ngày sẽ kết thúc mùa giải"
 *
 * Renders with an orange accent when ≤ 5 days left to subtly raise
 * the stakes; otherwise neutral muted. Returns no UI when [daysLeft]
 * is 0 — we don't lie about a finished season.
 */
@Composable
fun SeasonCountdown(
    daysLeft: Int,
    modifier: Modifier = Modifier
) {
    if (daysLeft <= 0) return
    val urgent = daysLeft <= 5
    val accent = if (urgent) androidx.compose.ui.graphics.Color(0xFFEA580C)
    else BetterMeColors.Text.TextTertiary
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(BetterMeTokens.CardRadius.Pill))
            .background(accent.copy(alpha = 0.10f))
            .border(
                width = 1.dp,
                color = accent.copy(alpha = if (urgent) 0.35f else 0.18f),
                shape = RoundedCornerShape(BetterMeTokens.CardRadius.Pill)
            )
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Text(
            text = if (urgent) "🔥 " else "🗓️ ",
            style = BetterMeTypography.Body.Small.Medium
        )
        Text(
            text = if (urgent) "Chỉ còn $daysLeft ngày — bứt phá nào!"
            else "Mùa giải kết thúc sau $daysLeft ngày",
            style = BetterMeTypography.Body.Small.Medium,
            color = accent,
            fontWeight = FontWeight.SemiBold
        )
    }
}
