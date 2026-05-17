package com.example.betterme.presentation.leaderboard.global.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.example.betterme.domain.leaderboard.RivalInsight
import com.example.betterme.presentation.theme.BetterMeColors
import com.example.betterme.presentation.theme.BetterMeTokens
import com.example.betterme.presentation.theme.BetterMeTypography

/**
 * Narrative card surfaced inside the Friends tab. Three visual
 * variants keyed by [RivalInsight.Kind]:
 *  - PASSED_YOU   — red accent ("X passed you")
 *  - WITHIN_REACH — blue accent ("only N points behind Y")
 *  - DEFEATED     — green accent ("you defeated Z this week")
 */
@Composable
fun RivalInsightCard(
    insight: RivalInsight,
    modifier: Modifier = Modifier
) {
    val accent = when (insight.kind) {
        RivalInsight.Kind.PASSED_YOU -> Color(0xFFDC2626)
        RivalInsight.Kind.WITHIN_REACH -> BetterMeColors.Primary.Primary
        RivalInsight.Kind.DEFEATED -> Color(0xFF16A34A)
    }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(BetterMeTokens.CardRadius.Body))
            .background(accent.copy(alpha = 0.08f))
            .border(
                width = 1.dp,
                color = accent.copy(alpha = 0.25f),
                shape = RoundedCornerShape(BetterMeTokens.CardRadius.Body)
            )
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(accent.copy(alpha = 0.18f))
        ) {
            insight.rivalAvatarUrl?.let {
                AsyncImage(
                    model = it,
                    contentDescription = insight.rivalDisplayName,
                    modifier = Modifier.size(36.dp).clip(CircleShape)
                )
            }
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = insight.message,
                style = BetterMeTypography.Body.Medium,
                color = BetterMeColors.Text.TextPrimary,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2
            )
        }
    }
}
