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
 * Interaction contract
 * - Tap-to-open-detail always works (read-only screens can still navigate in).
 * - [isInteractive] gates the visual "you can check in here" treatment.
 *   When false (past or future date), the status disc dims and a tiny 🔒
 *   badge pins to the top-right of the card so the user understands they're
 *   viewing history/preview, not a place to act. The daily check-in itself
 *   still happens inside HabitDetailScreen — this card never mutates state.
 */
@Composable
fun HabitCard(
    habit: HabitUiModel,
    onCardClick: () -> Unit = {},
    isInteractive: Boolean = true,
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

    // Outer Box lets the read-only badge float in the top-right without
    // stealing horizontal space from the row content.
    Box(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
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
                // Opening detail is always allowed — user-spec: "opening task detail
                // is still allowed, scrolling/viewing history is allowed".
                .clickable { onCardClick() }
                .padding(horizontal = 14.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Emoji disc — same tinted-bubble pattern as Habit Group / Notification
            // Center icons so the screen reads as one family.
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
                    // No strikethrough on completed rows: a checked-in habit lives
                    // in the DONE tab and signals its finished state through the
                    // green border + green check disc, which reads as a reward
                    // rather than deactivation.
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

            // Animated status disc. Status-only as before — check-in still happens
            // on HabitDetailScreen. Dims to 40% alpha on non-today rows so the
            // "you can't act here" signal is gentle but unmistakable.
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .scale(indicatorScale)
                    .clip(CircleShape)
                    .background(
                        if (isInteractive) indicatorBg
                        else indicatorBg.copy(alpha = 0.4f)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = if (habit.isCheckedInToday) "Đã check-in"
                    else "Chưa check-in",
                    tint = if (isInteractive) indicatorIconColor
                    else indicatorIconColor.copy(alpha = 0.5f),
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        // Read-only badge: small lock pill floating in the top-right corner.
        // Only mounts on non-today days — keeps interactive rows visually clean.
        if (!isInteractive) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(end = 10.dp, top = 8.dp)
                    .clip(RoundedCornerShape(BetterMeTokens.CardRadius.Pill))
                    .background(BetterMeColors.Text.TextTertiary.copy(alpha = 0.12f))
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            ) {
                Text(
                    text = "🔒 Chỉ xem",
                    style = BetterMeTypography.Body.Small.Medium,
                    color = BetterMeColors.Text.TextSecondary,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}
