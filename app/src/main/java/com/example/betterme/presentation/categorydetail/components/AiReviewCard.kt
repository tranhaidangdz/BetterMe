package com.example.betterme.presentation.categorydetail.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.betterme.presentation.categorydetail.AiReviewState
import com.example.betterme.presentation.theme.BetterMeColors
import com.example.betterme.presentation.theme.BetterMeTokens
import com.example.betterme.presentation.theme.BetterMeTypography
import kotlinx.coroutines.delay

/**
 * AI coach insight panel slotted into the Habit Group screen.
 *
 * Visual treatment (premium pass):
 * - Surface is a soft vertical gradient white → accent-tinted, so the card reads
 *   as "alive" rather than a flat panel — but the gradient is intentionally subtle
 *   to keep text contrast high.
 * - Success state gets a glow: a wider, lower-opacity outer shadow tinted with the
 *   accent on top of the standard neutral shadow.
 * - Loading uses a shimmer-strip skeleton — three pill-shaped placeholders animate
 *   a moving highlight across them so it never feels stuck.
 * - On Success the response is revealed with a soft typing animation (one substring
 *   tick per ~12ms) — fast enough that the full response appears in ~1s for typical
 *   length, slow enough that the eye registers it as "AI is talking to me".
 *
 * Actions (Success state):
 * - Tạo lại    — calls onGenerateAgain (parent forces cache bypass).
 * - Sao chép   — pushes the response into the system clipboard.
 * - Ẩn         — same as the header ✕.
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
        val surface = Brush.verticalGradient(
            colors = listOf(
                Color.White,
                accent.copy(alpha = BetterMeTokens.AccentAlpha.Soft * 0.6f)
            )
        )
        val borderWidth = if (state is AiReviewState.Success) 1.6.dp else 1.2.dp
        val borderColor = accent.copy(
            alpha = if (state is AiReviewState.Success) BetterMeTokens.AccentAlpha.Strong
            else BetterMeTokens.AccentAlpha.Medium
        )

        Column(
            modifier = modifier
                .fillMaxWidth()
                // Outer glow only on Success — a second, larger shadow with the accent
                // color on top of the neutral one fakes a soft halo without needing
                // a custom Modifier or RenderEffect.
                .let { m ->
                    if (state is AiReviewState.Success) {
                        m.shadow(
                            elevation = 10.dp,
                            shape = RoundedCornerShape(BetterMeTokens.CardRadius.Hero),
                            ambientColor = accent.copy(alpha = 0.25f),
                            spotColor = accent.copy(alpha = 0.30f)
                        )
                    } else m
                }
                .shadow(
                    elevation = BetterMeTokens.CardElevation.Body,
                    shape = RoundedCornerShape(BetterMeTokens.CardRadius.Hero),
                    ambientColor = BetterMeTokens.NeutralShadow.Ambient,
                    spotColor = BetterMeTokens.NeutralShadow.Spot
                )
                .clip(RoundedCornerShape(BetterMeTokens.CardRadius.Hero))
                .background(surface)
                .border(
                    width = borderWidth,
                    color = borderColor,
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
                    onRegenerate = onGenerateAgain,
                    onDismiss = onDismiss
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

/**
 * Three shimmering pill skeletons. Width-modulated so it doesn't look like a bar
 * chart — varies (full, 80%, 60%) for natural-looking copy. The highlight sweep
 * comes from a moving linear gradient driven by an infinite transition.
 */
@Composable
private fun LoadingBody(accent: Color) {
    val transition = rememberInfiniteTransition(label = "ai_shimmer")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ai_shimmer_phase"
    )

    Column {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(18.dp),
                color = accent,
                strokeWidth = 2.dp
            )
            Text(
                text = "Đang tổng hợp dữ liệu và gửi lên AI…",
                style = BetterMeTypography.Body.Small.Medium,
                color = BetterMeColors.Text.TextSecondary
            )
        }
        Spacer(Modifier.height(12.dp))
        ShimmerBar(phase = phase, widthFraction = 1.0f, accent = accent)
        Spacer(Modifier.height(8.dp))
        ShimmerBar(phase = phase, widthFraction = 0.8f, accent = accent)
        Spacer(Modifier.height(8.dp))
        ShimmerBar(phase = phase, widthFraction = 0.6f, accent = accent)
    }
}

@Composable
private fun ShimmerBar(phase: Float, widthFraction: Float, accent: Color) {
    val base = accent.copy(alpha = 0.08f)
    val highlight = accent.copy(alpha = 0.22f)
    // Sweep the highlight from -0.3 to 1.3 so it enters/exits the bar smoothly.
    val start = (phase * 1.6f) - 0.3f
    val brush = Brush.horizontalGradient(
        colorStops = arrayOf(
            (start - 0.2f).coerceIn(0f, 1f) to base,
            start.coerceIn(0f, 1f) to highlight,
            (start + 0.2f).coerceIn(0f, 1f) to base
        )
    )
    Box(
        modifier = Modifier
            .fillMaxWidth(widthFraction)
            .height(12.dp)
            .clip(RoundedCornerShape(999.dp))
            .background(brush)
    )
}

@Composable
private fun SuccessBody(
    text: String,
    accent: Color,
    onRegenerate: () -> Unit,
    onDismiss: () -> Unit
) {
    // Typing animation: progressively reveal `text` from 0 → length over time.
    // Re-keyed on `text` so a regeneration re-triggers from the start instead of
    // jumping to the new full string.
    var typedUpTo by remember(text) { mutableStateOf(0) }
    LaunchedEffect(text) {
        // Tick interval scales with length so very short responses still get a
        // perceivable animation, and long ones don't drag past ~1.2s.
        val targetMs = 1000
        val perChar = (targetMs / text.length.coerceAtLeast(1)).coerceIn(6, 25)
        while (typedUpTo < text.length) {
            typedUpTo += 2
            delay(perChar.toLong())
        }
        typedUpTo = text.length
    }
    val visible = text.substring(0, typedUpTo.coerceAtMost(text.length))

    val clipboard = LocalClipboardManager.current
    var copied by remember(text) { mutableStateOf(false) }
    LaunchedEffect(copied) {
        if (copied) {
            delay(1600)
            copied = false
        }
    }

    Column {
        AiRichText(raw = visible, accent = accent)
        Spacer(Modifier.height(14.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            PillButton(
                label = "↻  Tạo lại",
                color = accent,
                background = accent.copy(alpha = BetterMeTokens.AccentAlpha.Soft),
                onClick = onRegenerate
            )
            PillButton(
                label = if (copied) "✓  Đã chép" else "⧉  Sao chép",
                color = accent,
                background = accent.copy(alpha = BetterMeTokens.AccentAlpha.Soft),
                onClick = {
                    clipboard.setText(AnnotatedString(text))
                    copied = true
                }
            )
            Spacer(Modifier.weight(1f))
            PillButton(
                label = "Ẩn",
                color = BetterMeColors.Text.TextTertiary,
                background = Color(0xFFF1F2F4),
                onClick = onDismiss
            )
        }
    }
}

@Composable
private fun PillButton(
    label: String,
    color: Color,
    background: Color,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(BetterMeTokens.CardRadius.Pill))
            .background(background)
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Text(
            text = label,
            style = BetterMeTypography.Body.Small.Medium,
            color = color,
            fontWeight = FontWeight.SemiBold
        )
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
        Row(verticalAlignment = Alignment.CenterVertically) {
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
}
