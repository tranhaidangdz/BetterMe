package com.example.betterme.presentation.challenge.discover.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.betterme.presentation.challenge.shared.Difficulty
import com.example.betterme.presentation.theme.BetterMeColors
import com.example.betterme.presentation.theme.BetterMeTokens
import com.example.betterme.presentation.theme.BetterMeTypography

/**
 * Section header for the "Theo độ khó" grouped section on the Discover
 * screen. Tier badge on the left (color-tinted per [Difficulty]) +
 * label + optional count chip on the right.
 *
 * LEGENDARY tier badge gets a slightly different treatment — gold-tinted
 * label text and a "👑" suffix — to lean into the prestige feel the spec
 * calls for.
 */
@Composable
fun DifficultySectionHeader(
    difficulty: Difficulty,
    count: Int,
    modifier: Modifier = Modifier
) {
    val isLegendary = difficulty == Difficulty.LEGENDARY
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(BetterMeTokens.CardRadius.Pill))
                .background(difficulty.color.copy(alpha = 0.16f))
                .padding(horizontal = 10.dp, vertical = 4.dp)
        ) {
            Text(
                text = difficulty.label + if (isLegendary) "  👑" else "",
                style = BetterMeTypography.Body.Small.Medium,
                color = difficulty.color,
                fontWeight = FontWeight.SemiBold
            )
        }
        Spacer(Modifier.size(8.dp))
        Text(
            text = "${count} thử thách",
            style = BetterMeTypography.Body.Small.Medium,
            color = BetterMeColors.Text.TextTertiary,
            modifier = Modifier.weight(1f)
        )
    }
}
