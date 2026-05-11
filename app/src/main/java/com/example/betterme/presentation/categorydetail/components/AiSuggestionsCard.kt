package com.example.betterme.presentation.categorydetail.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.betterme.domain.ai.SuggestedHabit
import com.example.betterme.presentation.categorydetail.AiSuggestionsState
import com.example.betterme.presentation.theme.BetterMeColors
import com.example.betterme.presentation.theme.BetterMeTokens
import com.example.betterme.presentation.theme.BetterMeTypography

/**
 * Premium AI suggestions panel. Shows up to 4 habit suggestions as rich cards with
 * emoji + title + description + difficulty badge + estimated-impact tagline. Each
 * card has a "+ Thêm" button that hands the suggestion back to the screen for
 * insertion via the existing AddHabit flow.
 *
 * Three states piped through [AiSuggestionsState]:
 * - Loading → spinner + "AI đang tạo gợi ý cá nhân hoá…"
 * - Success → list of [SuggestionRow]s
 * - Error   → red retry pill
 *
 * Visual surface matches AiReviewCard so the two AI panels read as one design.
 */
@Composable
fun AiSuggestionsCard(
    state: AiSuggestionsState,
    accent: Color,
    onAddSuggestion: (SuggestedHabit) -> Unit,
    onRegenerate: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = state !is AiSuggestionsState.Idle,
        enter = fadeIn(tween(220)) + slideInVertically(
            initialOffsetY = { it / 4 },
            animationSpec = tween(220)
        ),
        exit = fadeOut(tween(160)) + slideOutVertically(
            targetOffsetY = { it / 4 },
            animationSpec = tween(160)
        )
    ) {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .shadow(
                    elevation = BetterMeTokens.CardElevation.Body,
                    shape = RoundedCornerShape(BetterMeTokens.CardRadius.Hero),
                    ambientColor = BetterMeTokens.NeutralShadow.Ambient,
                    spotColor = BetterMeTokens.NeutralShadow.Spot
                )
                .clip(RoundedCornerShape(BetterMeTokens.CardRadius.Hero))
                .background(Color.White)
                .border(
                    width = 1.2.dp,
                    color = accent.copy(alpha = BetterMeTokens.AccentAlpha.Medium),
                    shape = RoundedCornerShape(BetterMeTokens.CardRadius.Hero)
                )
                .padding(18.dp)
        ) {
            // Header
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(accent.copy(alpha = BetterMeTokens.AccentAlpha.Soft))
                        .border(
                            width = 1.dp,
                            color = accent.copy(alpha = BetterMeTokens.AccentAlpha.Medium),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "💡", fontSize = 18.sp)
                }
                Spacer(Modifier.size(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Gợi ý từ AI",
                        style = BetterMeTypography.Title.Small.Bold,
                        color = BetterMeColors.Text.TextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = when (state) {
                            is AiSuggestionsState.Loading -> "AI đang tạo gợi ý cá nhân hoá…"
                            is AiSuggestionsState.Success -> "${state.items.size} thói quen phù hợp"
                            is AiSuggestionsState.Error -> "Không thể tạo gợi ý"
                            AiSuggestionsState.Idle -> ""
                        },
                        style = BetterMeTypography.Body.Small.Medium,
                        color = BetterMeColors.Text.TextTertiary
                    )
                }
                if (state !is AiSuggestionsState.Loading) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(BetterMeTokens.CardRadius.Pill))
                            .clickable { onDismiss() }
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "✕",
                            style = BetterMeTypography.Title.Small.Bold,
                            color = BetterMeColors.Text.TextTertiary
                        )
                    }
                }
            }

            Spacer(Modifier.height(14.dp))

            when (state) {
                is AiSuggestionsState.Loading -> LoadingRow(accent)
                is AiSuggestionsState.Success -> SuccessList(
                    items = state.items,
                    accent = accent,
                    onAdd = onAddSuggestion,
                    onRegenerate = onRegenerate
                )
                is AiSuggestionsState.Error -> ErrorRow(state.message, onRegenerate)
                AiSuggestionsState.Idle -> Unit
            }
        }
    }
}

@Composable
private fun LoadingRow(accent: Color) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(20.dp),
            color = accent,
            strokeWidth = 2.dp
        )
        Text(
            text = "Đang tổng hợp gợi ý dựa trên thói quen hiện tại của bạn…",
            style = BetterMeTypography.Body.Medium,
            color = BetterMeColors.Text.TextSecondary
        )
    }
}

@Composable
private fun SuccessList(
    items: List<SuggestedHabit>,
    accent: Color,
    onAdd: (SuggestedHabit) -> Unit,
    onRegenerate: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        items.forEach { suggestion ->
            SuggestionRow(suggestion = suggestion, accent = accent, onAdd = onAdd)
        }
        Spacer(Modifier.height(4.dp))
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(BetterMeTokens.CardRadius.Pill))
                .background(accent.copy(alpha = BetterMeTokens.AccentAlpha.Soft))
                .clickable { onRegenerate() }
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Text(
                text = "↻  Tạo lại",
                style = BetterMeTypography.Body.Small.Medium,
                color = accent,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun SuggestionRow(
    suggestion: SuggestedHabit,
    accent: Color,
    onAdd: (SuggestedHabit) -> Unit
) {
    val (badgeColor, badgeLabel) = when (suggestion.difficulty.uppercase()) {
        "EASY" -> Color(0xFF16A34A) to "Dễ"
        "HARD" -> Color(0xFFDC2626) to "Khó"
        else -> Color(0xFFEA580C) to "Trung bình"
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(BetterMeTokens.CardRadius.Body))
            .background(BetterMeColors.BackGround.BackgroundSecondary)
            .border(
                width = 1.dp,
                color = accent.copy(alpha = BetterMeTokens.AccentAlpha.Subtle),
                shape = RoundedCornerShape(BetterMeTokens.CardRadius.Body)
            )
            .padding(12.dp),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(accent.copy(alpha = BetterMeTokens.AccentAlpha.Soft)),
            contentAlignment = Alignment.Center
        ) {
            Text(text = suggestion.emoji, fontSize = 20.sp)
        }

        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = suggestion.title,
                    style = BetterMeTypography.Title.Small.SemiBold,
                    color = BetterMeColors.Text.TextPrimary,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f)
                )
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(BetterMeTokens.CardRadius.Pill))
                        .background(badgeColor.copy(alpha = 0.12f))
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = badgeLabel,
                        style = BetterMeTypography.Body.Small.Medium,
                        color = badgeColor,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
            if (suggestion.description.isNotBlank()) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = suggestion.description,
                    style = BetterMeTypography.Body.Small.Medium,
                    color = BetterMeColors.Text.TextSecondary
                )
            }
            if (suggestion.estimatedImpact.isNotBlank()) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "✨ ${suggestion.estimatedImpact}",
                    style = BetterMeTypography.Body.Small.Medium,
                    color = accent,
                    fontWeight = FontWeight.Medium
                )
            }
            Spacer(Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(BetterMeTokens.CardRadius.Pill))
                    .background(accent)
                    .clickable { onAdd(suggestion) }
                    .padding(horizontal = 14.dp, vertical = 8.dp)
            ) {
                Text(
                    text = "+ Thêm thói quen",
                    style = BetterMeTypography.Body.Small.Medium,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun ErrorRow(message: String, onRetry: () -> Unit) {
    val errorColor = BetterMeColors.Red
    Column {
        Text(
            text = message,
            style = BetterMeTypography.Body.Medium,
            color = errorColor
        )
        Spacer(Modifier.height(12.dp))
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(BetterMeTokens.CardRadius.Pill))
                .background(errorColor.copy(alpha = 0.10f))
                .clickable { onRetry() }
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Text(
                text = "↻  Thử lại",
                style = BetterMeTypography.Body.Small.Medium,
                color = errorColor,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}
