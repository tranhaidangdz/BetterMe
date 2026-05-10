package com.example.betterme.presentation.challenge.detail.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.betterme.presentation.challenge.shared.Difficulty
import com.example.betterme.presentation.challenge.shared.DifficultyPill
import com.example.betterme.presentation.theme.BetterMeColors
import com.example.betterme.presentation.theme.BetterMeTypography

/**
 * Centered hero badge for the Challenge Detail screen — matches the design with a large
 * round emblem (emoji on tinted disc), stylized side ribbons, then title + difficulty pill,
 * then a short subtitle line. Renders without any wide bordering card so it sits flush
 * against the screen background.
 */
@Composable
fun ChallengeHeroCard(
    title: String,
    subtitle: String,
    iconEmoji: String,
    accentColor: Color,
    difficulty: Difficulty,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        BadgeEmblem(emoji = iconEmoji, accentColor = accentColor)
        Spacer(modifier = Modifier.height(18.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = title,
                style = BetterMeTypography.Title.Large.Bold,
                color = BetterMeColors.Text.TextPrimary,
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Bold
            )
            DifficultyPill(difficulty)
        }
        if (subtitle.isNotBlank()) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = subtitle,
                style = BetterMeTypography.Body.Medium,
                color = BetterMeColors.Text.TextTertiary,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }
    }
}

/**
 * Centered emblem with a circular tinted disc carrying the emoji and two small ribbon
 * triangles flanking it on either side. Matches the design's "trophy ribbon" feel without
 * needing per-challenge artwork.
 */
@Composable
private fun BadgeEmblem(emoji: String, accentColor: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        RibbonShape(color = accentColor, mirrored = false)
        Spacer(modifier = Modifier.width(6.dp))
        Box(
            modifier = Modifier
                .size(112.dp)
                .clip(CircleShape)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            accentColor.copy(alpha = 0.20f),
                            accentColor.copy(alpha = 0.06f)
                        )
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(78.dp)
                    .clip(CircleShape)
                    .background(accentColor.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center
            ) {
                Text(text = emoji, fontSize = 44.sp)
            }
        }
        Spacer(modifier = Modifier.width(6.dp))
        RibbonShape(color = accentColor, mirrored = true)
    }
}

@Composable
private fun RibbonShape(color: Color, mirrored: Boolean) {
    Column(
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Box(
            modifier = Modifier
                .size(width = 22.dp, height = 16.dp)
                .rotate(if (mirrored) -8f else 8f)
                .background(color.copy(alpha = 0.55f))
        )
        Box(
            modifier = Modifier
                .size(width = 22.dp, height = 16.dp)
                .rotate(if (mirrored) 8f else -8f)
                .background(color.copy(alpha = 0.85f))
        )
    }
}
