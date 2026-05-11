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
import com.example.betterme.presentation.categorydetail.AiReviewState
import com.example.betterme.presentation.theme.BetterMeColors
import com.example.betterme.presentation.theme.BetterMeTokens
import com.example.betterme.presentation.theme.BetterMeTypography

/**
 * AI coach insight panel slotted into the Habit Group screen.
 *
 * Three visual states, picked by the sealed [AiReviewState]:
 * - [AiReviewState.Loading]   — accent-tinted card with a spinner + "Đang phân tích…".
 * - [AiReviewState.Success]   — accent-tinted card with the model's reply + a
 *                                "Đóng" action to dismiss. Tap "Tạo lại" to re-run.
 * - [AiReviewState.Error]     — soft red surface with the error and a "Thử lại" link.
 *
 * The whole panel animates in from below when the state flips out of Idle, and slides
 * out when the user dismisses. AnimatedVisibility wraps the card so the surrounding
 * LazyColumn doesn't jitter on entry/exit.
 *
 * accent is piped in from `paletteFor(categoryId).accent` so the AI card inherits
 * the category's identity color — keeps the screen visually unified.
 */
@Composable
fun AiReviewCard(
    state: AiReviewState,
    accent: Color,
    onGenerateAgain: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = state !is AiReviewState.Idle,
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
            // Header: avatar disc + title + dismiss
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
                    Text(text = "✨", fontSize = 18.sp)
                }
                Spacer(Modifier.size(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Huấn luyện viên AI",
                        style = BetterMeTypography.Title.Small.Bold,
                        color = BetterMeColors.Text.TextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = when (state) {
                            is AiReviewState.Loading -> "Đang phân tích thói quen của bạn…"
                            is AiReviewState.Success -> "Phân tích cá nhân hoá"
                            is AiReviewState.Error -> "Không thể tạo nhận xét"
                            AiReviewState.Idle -> ""
                        },
                        style = BetterMeTypography.Body.Small.Medium,
                        color = BetterMeColors.Text.TextTertiary
                    )
                }
                if (state !is AiReviewState.Loading) {
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

            // Body
            when (state) {
                is AiReviewState.Loading -> LoadingBody(accent)
                is AiReviewState.Success -> SuccessBody(
                    text = state.text,
                    accent = accent,
                    onRegenerate = onGenerateAgain
                )
                is AiReviewState.Error -> ErrorBody(
                    message = state.message,
                    onRetry = onGenerateAgain
                )
                AiReviewState.Idle -> Unit
            }
        }
    }
}

@Composable
private fun LoadingBody(accent: Color) {
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
            text = "Đang gửi dữ liệu lên AI và chờ phản hồi…",
            style = BetterMeTypography.Body.Medium,
            color = BetterMeColors.Text.TextSecondary
        )
    }
}

@Composable
private fun SuccessBody(text: String, accent: Color, onRegenerate: () -> Unit) {
    Column {
        Text(
            text = text,
            style = BetterMeTypography.Body.Medium,
            color = BetterMeColors.Text.TextPrimary
        )
        Spacer(Modifier.height(14.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
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
}

@Composable
private fun ErrorBody(message: String, onRetry: () -> Unit) {
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
