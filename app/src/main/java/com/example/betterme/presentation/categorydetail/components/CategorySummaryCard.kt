package com.example.betterme.presentation.categorydetail.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.betterme.presentation.categorydetail.CategoryDetailState
import com.example.betterme.presentation.theme.BetterMeColors
import com.example.betterme.presentation.theme.BetterMeTypography

/**
 * Hero summary card for the Habit Group / Category Detail screen.
 *
 * Carries the category's full visual identity: a soft accent-tinted vertical gradient
 * over a white surface, a tinted emoji disc, a motivational subtitle and the right-hand
 * circular progress label. Matches the design language used by HomeCard / CantMissCard
 * on the redesigned Home screen — same white surface, same gradient hint, same tinted
 * shadow.
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
                elevation = 6.dp,
                shape = RoundedCornerShape(22.dp),
                ambientColor = palette.accent.copy(alpha = 0.22f),
                spotColor = palette.accent.copy(alpha = 0.28f)
            )
            .clip(RoundedCornerShape(22.dp))
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color.White,
                        palette.soft.copy(alpha = 0.55f)
                    )
                )
            )
            .padding(18.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Emoji disc — tinted gradient identity glyph.
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                palette.accent.copy(alpha = 0.28f),
                                palette.accent.copy(alpha = 0.14f)
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(text = state.categoryIcon, fontSize = 28.sp)
            }

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

        // Stats pills row. Each pill carries one signal so the card reads as a
        // glanceable summary instead of a paragraph.
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            StatPill(
                label = "${state.habits.size} thói quen",
                accent = palette.accent
            )
            StatPill(
                label = "${state.completedHabitCount} hoàn thành",
                accent = palette.accent
            )
            StatPill(
                label = "${state.groupCompletionPercent}%",
                accent = palette.accent,
                emphasized = true
            )
        }

        Spacer(Modifier.height(12.dp))

        // Inline thin progress bar — gives the percent a visual referent the user
        // can read at a glance without parsing the circle on the right.
        LinearProgressBar(
            percent = animatedPercent / 100f,
            color = palette.accent,
            height = 6.dp
        )
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
            .clip(RoundedCornerShape(999.dp))
            .background(
                if (emphasized) accent.copy(alpha = 0.18f) else accent.copy(alpha = 0.10f)
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
