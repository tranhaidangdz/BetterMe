package com.example.betterme.presentation.dailyhabits.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.betterme.presentation.dailyhabits.model.HabitUiModel
import com.example.betterme.presentation.theme.BetterMeColors
import com.example.betterme.presentation.theme.BetterMeTokens
import com.example.betterme.presentation.theme.BetterMeTypography

/**
 * Premium habit row used by the Tasks tab.
 *
 * Visual language matches Challenge Detail / Notification Center / Category
 * Detail cards: neutral shadow, white surface, accent border tint, 20dp radius.
 * Completed rows pick up a soft green accent (border + emoji disc tint) so the
 * "done" state reads as a small celebration rather than a gray strikeout.
 *
 * Animation
 * - Border + indicator colors animate with [animateColorAsState] so the row
 *   visibly turns green the moment a check-in lands instead of swapping.
 * - The status indicator scales briefly when entering the completed state.
 *
 * Interaction contract unchanged: the row is tappable, the indicator is
 * status-only — daily check-in still goes through HabitDetailScreen's camera.
 */
@Composable
fun HabitCard(
    habit: HabitUiModel,
    onCardClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val accent = BetterMeColors.Primary.Primary
    val doneColor = Color(0xFF16A34A)

    val borderColor by animateColorAsState(
        targetValue = if (habit.isCheckedInToday) doneColor.copy(alpha = 0.32f)
        else accent.copy(alpha = BetterMeTokens.AccentAlpha.Medium),
        animationSpec = tween(300),
        label = "habit_border"
    )
    val indicatorBg by animateColorAsState(
        targetValue = if (habit.isCheckedInToday) doneColor else BetterMeColors.Gray.Gray4,
        animationSpec = tween(300),
        label = "habit_indicator_bg"
    )
    val indicatorIconColor by animateColorAsState(
        targetValue = if (habit.isCheckedInToday) BetterMeColors.White
        else BetterMeColors.Text.TextTertiary,
        animationSpec = tween(300),
        label = "habit_indicator_icon"
    )
    val indicatorScale by animateFloatAsState(
        targetValue = if (habit.isCheckedInToday) 1.06f else 1f,
        animationSpec = tween(280),
        label = "habit_indicator_scale"
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = BetterMeTokens.CardElevation.Body,
                shape = RoundedCornerShape(BetterMeTokens.CardRadius.Standard),
                ambientColor = BetterMeTokens.NeutralShadow.Ambient,
                spotColor = BetterMeTokens.NeutralShadow.Spot
            )
            .clip(RoundedCornerShape(BetterMeTokens.CardRadius.Standard))
            .background(
                if (habit.isCheckedInToday) doneColor.copy(alpha = 0.05f)
                else BetterMeColors.White
            )
            .border(
                width = 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(BetterMeTokens.CardRadius.Standard)
            )
            .clickable { onCardClick() }
            .padding(horizontal = 14.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Emoji disc — sits in the same tinted-bubble pattern as Habit Group /
        // Notification Center icons so the screen reads as one family.
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(
                    if (habit.isCheckedInToday) doneColor.copy(alpha = 0.14f)
                    else accent.copy(alpha = BetterMeTokens.AccentAlpha.Soft)
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(text = habit.icon.ifBlank { "🎯" }, fontSize = 22.sp)
        }

        Spacer(Modifier.width(12.dp))

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = habit.category,
                style = BetterMeTypography.Body.Small.Medium,
                color = BetterMeColors.Text.TextTertiary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = habit.title,
                style = BetterMeTypography.Body.Medium.copy(fontWeight = FontWeight.SemiBold),
                color = BetterMeColors.Text.TextPrimary,
                // Subtle strike-through when done — rewarding visual without
                // making the row feel "dimmed" or deactivated.
                textDecoration = if (habit.isCheckedInToday) TextDecoration.LineThrough else null,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            if (habit.time.isNotBlank()) {
                Spacer(Modifier.height(2.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(BetterMeTokens.CardRadius.Pill))
                        .background(accent.copy(alpha = BetterMeTokens.AccentAlpha.Soft))
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "⏰ ${habit.time}",
                        style = BetterMeTypography.Body.Small.Medium,
                        color = accent,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        // Animated status disc. Same status-only semantics as before — check-in
        // still happens on HabitDetailScreen — but the animation makes the state
        // change feel intentional.
        Box(
            modifier = Modifier
                .size(36.dp)
                .scale(indicatorScale)
                .clip(CircleShape)
                .background(indicatorBg),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = if (habit.isCheckedInToday) "Đã check-in hôm nay"
                else "Chưa check-in hôm nay",
                tint = indicatorIconColor,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
