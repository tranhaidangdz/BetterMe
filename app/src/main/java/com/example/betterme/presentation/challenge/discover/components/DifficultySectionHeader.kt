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
 * screen. Label format follows the spec exactly:
 *
 *   "{Difficulty Name} ({actualCount})"
 *
 * e.g. "Trung bình (37)". The [count] passed in MUST be the size of the
 * tier's full grouped list — the caller (ChallengeDiscoverScreen) drives
 * it from `groupedList[difficulty].size`, never from a truncated slice.
 *
 * LEGENDARY tier badge gets a "👑" suffix as a small prestige cue —
 * still inside the count parentheses contract.
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
                // Exact spec format: "{label} ({count})" — count is the
                // single source of truth surfaced inside the tier pill
                // so it tracks the visible list 1:1.
                text = buildString {
                    append(difficulty.label)
                    append(" (")
                    append(count)
                    append(")")
                    if (isLegendary) append("  👑")
                },
                style = BetterMeTypography.Body.Small.Medium,
                color = difficulty.color,
                fontWeight = FontWeight.SemiBold
            )
        }
        Spacer(Modifier.size(8.dp))
        // Trailing helper string only — never holds an independent
        // count value so it can't drift from the pill's number.
        Text(
            text = "thử thách",
            style = BetterMeTypography.Body.Small.Medium,
            color = BetterMeColors.Text.TextTertiary,
            modifier = Modifier.weight(1f)
        )
    }
}
