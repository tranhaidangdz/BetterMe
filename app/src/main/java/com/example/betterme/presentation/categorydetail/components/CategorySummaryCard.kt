package com.example.betterme.presentation.categorydetail.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.betterme.presentation.categorydetail.CategoryDetailState
import com.example.betterme.presentation.theme.BetterMeColors
import com.example.betterme.presentation.theme.BetterMeTokens
import com.example.betterme.presentation.theme.BetterMeTypography

/**
 * Hero summary card for the Habit Group screen.
 *
 * Solid white surface (was a 0..0.55-alpha vertical gradient, which read as washed
 * out against the pale-blue page background). The accent identity now lives in
 * three discreet places: a 1.2dp accent-tinted border, the emoji-disc tint, and the
 * stat/percent pills. Neutral black shadow at Tokens.NeutralShadow keeps the lift
 * believable without the chromatic noise the prior tinted shadow produced.
 */
@Composable
fun CategorySummaryCard(
    state: CategoryDetailState,
    modifier: Modifier = Modifier
) {
    val palette = paletteFor(state.categoryId)
    val animatedPercent by animateFloatAsState(
        targetValue = state.groupCompletionPercent.toFloat(),
        animationSpec = tween(900),
        label = "group_progress"
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = BetterMeTokens.CardElevation.Hero,
                shape = RoundedCornerShape(BetterMeTokens.CardRadius.Hero),
                ambientColor = BetterMeTokens.NeutralShadow.Ambient,
                spotColor = BetterMeTokens.NeutralShadow.Spot
            )
            .clip(RoundedCornerShape(BetterMeTokens.CardRadius.Hero))
            .background(Color.White)
            .border(
                width = 1.2.dp,
                color = palette.accent.copy(alpha = BetterMeTokens.AccentAlpha.Medium),
                shape = RoundedCornerShape(BetterMeTokens.CardRadius.Hero)
            )
            .padding(18.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CategoryEmojiDisc(
                emoji = state.categoryIcon,
                accent = palette.accent,
                size = 56.dp,
                fontSize = 28.sp
            )

            Spacer(Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = state.categoryName,
                    style = BetterMeTypography.Title.Medium.Bold,
                    color = BetterMeColors.Text.TextPrimary,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = palette.subtitle,
                    style = BetterMeTypography.Body.Small.Medium,
                    color = BetterMeColors.Text.TextTertiary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(Modifier.width(12.dp))

            CircularProgressLabel(
                percent = animatedPercent.toInt(),
                size = 64.dp,
                color = palette.accent
            )
        }

        Spacer(Modifier.height(14.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            StatPill(label = "${state.habits.size} thói quen", accent = palette.accent)
            StatPill(label = "${state.completedHabitCount} hoàn thành", accent = palette.accent)
            StatPill(
                label = "${state.groupCompletionPercent}%",
                accent = palette.accent,
                emphasized = true
            )
        }

        Spacer(Modifier.height(12.dp))

        LinearProgressBar(
            percent = animatedPercent / 100f,
            color = palette.accent,
            height = 6.dp
        )
    }
}

/**
 * Two-ring tinted disc carrying the category emoji. The previous version computed an
 * inline `Brush.verticalGradient` per call; this shared component pulls the alpha
 * stops from [BetterMeTokens.AccentAlpha] so all disc tints stay in lockstep.
 */
@Composable
internal fun CategoryEmojiDisc(
    emoji: String,
    accent: Color,
    size: androidx.compose.ui.unit.Dp,
    fontSize: androidx.compose.ui.unit.TextUnit
) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(accent.copy(alpha = BetterMeTokens.AccentAlpha.Soft))
            .border(
                width = 1.dp,
                color = accent.copy(alpha = BetterMeTokens.AccentAlpha.Medium),
                shape = CircleShape
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(text = emoji, fontSize = fontSize)
    }
}

@Composable
private fun StatPill(
    label: String,
    accent: Color,
    emphasized: Boolean = false
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(BetterMeTokens.CardRadius.Pill))
            .background(
                accent.copy(
                    alpha = if (emphasized) BetterMeTokens.AccentAlpha.Medium
                    else BetterMeTokens.AccentAlpha.Soft
                )
            )
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(
            text = label,
            style = BetterMeTypography.Body.Small.Medium,
            color = accent,
            fontWeight = if (emphasized) FontWeight.Bold else FontWeight.SemiBold
        )
    }
}
