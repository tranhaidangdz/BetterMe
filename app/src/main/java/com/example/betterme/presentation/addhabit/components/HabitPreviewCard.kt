package com.example.betterme.presentation.addhabit.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.betterme.presentation.theme.BetterMeColors
import com.example.betterme.presentation.theme.BetterMeTokens
import com.example.betterme.presentation.theme.BetterMeTypography

/**
 * Live-preview card shown at the top of the Add Habit screen. Mirrors the actual
 * habit row a user will see on Home — same emoji disc grammar, same accent rhythm —
 * so the form feels like "you're sculpting the row" rather than "you're filling a form".
 *
 * Values update reactively as the user types / picks; `animateContentSize` smooths
 * the height transition when the description line wraps or appears for the first time.
 */
@Composable
fun HabitPreviewCard(
    title: String,
    categoryName: String,
    categoryIcon: String,
    accent: Color,
    reminderTime: String,
    targetDays: Int?,
    modifier: Modifier = Modifier
) {
    val effectiveTitle = title.ifBlank { "Thói quen mới của bạn" }
    val effectiveCategoryLabel = categoryName.ifBlank { "Chọn nhóm thói quen" }
    val effectiveIcon = categoryIcon.ifBlank { "✨" }

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
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color.White,
                        accent.copy(alpha = BetterMeTokens.AccentAlpha.Subtle)
                    )
                )
            )
            .border(
                width = 1.2.dp,
                color = accent.copy(alpha = BetterMeTokens.AccentAlpha.Medium),
                shape = RoundedCornerShape(BetterMeTokens.CardRadius.Hero)
            )
            .padding(20.dp)
            .animateContentSize(animationSpec = tween(220))
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(accent.copy(alpha = BetterMeTokens.AccentAlpha.Soft))
                    .border(
                        width = 1.dp,
                        color = accent.copy(alpha = BetterMeTokens.AccentAlpha.Medium),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                AnimatedContent(
                    targetState = effectiveIcon,
                    transitionSpec = {
                        (fadeIn(tween(180)) togetherWith fadeOut(tween(140)))
                    },
                    label = "preview_icon"
                ) { glyph ->
                    Text(text = glyph, fontSize = 28.sp)
                }
            }
            Spacer(Modifier.size(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Xem trước",
                    style = BetterMeTypography.Body.Small.Medium,
                    color = accent,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = effectiveTitle,
                    style = BetterMeTypography.Title.Medium.Bold,
                    color = BetterMeColors.Text.TextPrimary,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = effectiveCategoryLabel,
                    style = BetterMeTypography.Body.Small.Medium,
                    color = BetterMeColors.Text.TextTertiary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Spacer(Modifier.height(14.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            MetaPill(
                emoji = "⏰",
                label = if (reminderTime.isBlank()) "Chưa nhắc" else reminderTime,
                accent = accent
            )
            if (targetDays != null && targetDays > 0) {
                MetaPill(
                    emoji = "🎯",
                    label = "$targetDays ngày",
                    accent = accent
                )
            }
            MetaPill(
                emoji = "🔥",
                label = "Chuỗi 0",
                accent = accent
            )
        }
    }
}

@Composable
private fun MetaPill(emoji: String, label: String, accent: Color) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(BetterMeTokens.CardRadius.Pill))
            .background(accent.copy(alpha = BetterMeTokens.AccentAlpha.Soft))
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(text = emoji, fontSize = 13.sp)
        Text(
            text = label,
            style = BetterMeTypography.Body.Small.Medium,
            color = accent,
            fontWeight = FontWeight.SemiBold
        )
    }
}
