package com.example.betterme.presentation.challenge.discover.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.betterme.presentation.challenge.model.NewChallengeUiModel
import com.example.betterme.presentation.challenge.shared.ChallengeIconTile
import com.example.betterme.presentation.challenge.shared.DifficultyPill
import com.example.betterme.presentation.theme.BetterMeColors
import com.example.betterme.presentation.theme.BetterMeTypography

/**
 * Prestige-styled variant of [NewChallengeRow] used for LEGENDARY-tier
 * cards. Carries:
 *  - a purple→gold gradient border
 *  - a soft purple tinted background
 *  - bolder shadow elevation
 *  - "👑" prefix on the title for visual prestige
 *
 * The non-LEGENDARY rows stay on [NewChallengeRow] so they don't
 * compete visually with the elite tier — keeping the legendary
 * treatment rare is the whole point.
 */
@Composable
fun LegendaryChallengeRow(
    model: NewChallengeUiModel,
    onClick: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val gradient = Brush.linearGradient(
        colors = listOf(
            Color(0xFF7C3AED), // royal purple
            Color(0xFFB45309)  // bronze-gold
        )
    )
    Row(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = 6.dp,
                shape = RoundedCornerShape(18.dp),
                ambientColor = Color(0x337C3AED),
                spotColor = Color(0x4D7C3AED)
            )
            .clip(RoundedCornerShape(18.dp))
            // Two-layer background: solid surface beneath, gradient
            // border drawn on top. Box-on-box would be cleaner but
            // Row's single background slot keeps the composable simple.
            .background(Color(0xFFF7F3FF))
            .border(
                width = 1.5.dp,
                brush = gradient,
                shape = RoundedCornerShape(18.dp)
            )
            .clickable { onClick(model.challengeId) }
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        ChallengeIconTile(emoji = model.iconEmoji, accentColor = model.accentColor)
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "👑  ${model.title}",
                style = BetterMeTypography.Title.Small.Bold,
                color = BetterMeColors.Text.TextPrimary,
                fontWeight = FontWeight.Bold,
                maxLines = 2
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "🪙 ${formatCount(model.rewardCoins)} xu  ·  👥 ${formatCount(model.participantCount)} tham gia",
                style = BetterMeTypography.Body.Small.Medium,
                color = BetterMeColors.Text.TextTertiary
            )
        }
        DifficultyPill(model.difficulty)
    }
}

private fun formatCount(n: Int): String {
    if (n >= 1000) return "${"%.1f".format(n / 1000f)}K"
    return n.toString()
}
