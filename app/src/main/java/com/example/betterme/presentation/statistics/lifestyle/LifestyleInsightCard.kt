package com.example.betterme.presentation.statistics.lifestyle

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.betterme.domain.ai.lifestyle.AdaptiveSuggestion
import com.example.betterme.domain.ai.lifestyle.LifestyleInsight
import com.example.betterme.domain.ai.lifestyle.OverallTrend
import com.example.betterme.domain.ai.lifestyle.SuggestionType
import com.example.betterme.domain.ai.schedule.BurnoutRisk
import com.example.betterme.presentation.theme.BetterMeColors
import com.example.betterme.presentation.theme.BetterMeTokens
import com.example.betterme.presentation.theme.BetterMeTypography
import kotlinx.coroutines.delay

/**
 * Inline "Coach insight" card for the Statistics screen. Renders the
 * Adaptive Lifestyle Insight Engine's output as a passive read surface
 * (no modal dialog) — the user scrolls past it as part of their stats
 * review.
 *
 * Layout when [LifestyleInsightUi.Success]:
 *   - Header: 🌱 disc + "Lời khuyên từ huấn luyện viên AI" + trend pill
 *   - Two scores side-by-side: consistency ring + recovery ring
 *   - Burnout pill (color-coded)
 *   - Primary insight + coaching message
 *   - Adaptive suggestions list (1-4 cards)
 *   - "Phân tích lại" pill + offline chip if isCanned
 *
 * Loading shows a soft skeleton + rotating Vietnamese message (lifecycle-
 * safe LaunchedEffect, dies with the Loading branch). Idle hides itself.
 */
@Composable
fun LifestyleInsightCard(
    state: LifestyleInsightState,
    onRefresh: () -> Unit
) {
    when (val ui = state.ui) {
        LifestyleInsightUi.Idle -> Unit
        LifestyleInsightUi.Loading -> LoadingCard()
        is LifestyleInsightUi.Success -> SuccessCard(ui.insight, onRefresh)
        is LifestyleInsightUi.Error -> ErrorCard(ui.message, onRefresh)
    }
}

@Composable
private fun CardSurface(content: @Composable () -> Unit) {
    val accent = BetterMeColors.Primary.Primary
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = BetterMeTokens.CardElevation.Body,
                shape = RoundedCornerShape(BetterMeTokens.CardRadius.Hero),
                ambientColor = BetterMeTokens.NeutralShadow.Ambient,
                spotColor = BetterMeTokens.NeutralShadow.Spot
            )
            .clip(RoundedCornerShape(BetterMeTokens.CardRadius.Hero))
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color.White, accent.copy(alpha = 0.05f))
                )
            )
            .border(
                width = 1.dp,
                color = accent.copy(alpha = BetterMeTokens.AccentAlpha.Medium),
                shape = RoundedCornerShape(BetterMeTokens.CardRadius.Hero)
            )
            .padding(18.dp)
    ) { content() }
}

@Composable
private fun LoadingCard() {
    val messages = remember {
        listOf(
            "Đang đọc nhịp 14 ngày của bạn…",
            "Tìm mô hình thói quen lặp lại…",
            "Soát cân bằng phục hồi và năng lượng…"
        )
    }
    var index by remember { mutableStateOf(0) }
    LaunchedEffect(messages) {
        while (true) {
            delay(1800)
            index = (index + 1) % messages.size
        }
    }
    CardSurface {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(24.dp),
                color = BetterMeColors.Primary.Primary,
                strokeWidth = 2.4.dp
            )
            AnimatedContent(
                targetState = messages[index],
                transitionSpec = { fadeIn(tween(280)) togetherWith fadeOut(tween(180)) },
                label = "lifestyle_loading_rotator"
            ) { msg ->
                Text(
                    text = msg,
                    style = BetterMeTypography.Body.Medium,
                    color = BetterMeColors.Text.TextSecondary
                )
            }
        }
    }
}

@Composable
private fun ErrorCard(message: String, onRetry: () -> Unit) {
    CardSurface {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                text = "⚠️  $message",
                style = BetterMeTypography.Body.Medium,
                color = BetterMeColors.Red
            )
            Pill(
                label = "↻  Thử lại",
                color = BetterMeColors.Red,
                background = BetterMeColors.Red.copy(alpha = 0.10f),
                onClick = onRetry
            )
        }
    }
}

@Composable
private fun SuccessCard(insight: LifestyleInsight, onRefresh: () -> Unit) {
    val accent = BetterMeColors.Primary.Primary
    CardSurface {
        Column {
            // ─── Header ──────────────────────────────────────────
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(accent.copy(alpha = BetterMeTokens.AccentAlpha.Soft))
                        .border(
                            width = 1.dp,
                            color = accent.copy(alpha = BetterMeTokens.AccentAlpha.Medium),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "🌱", fontSize = 20.sp)
                }
                Spacer(Modifier.size(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Lời khuyên từ huấn luyện viên AI",
                        style = BetterMeTypography.Title.Small.Bold,
                        color = BetterMeColors.Text.TextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Phân tích 14 ngày qua",
                        style = BetterMeTypography.Body.Small.Medium,
                        color = BetterMeColors.Text.TextTertiary
                    )
                }
                TrendPill(insight.overallTrend)
            }

            Spacer(Modifier.height(14.dp))

            // ─── Score rings ─────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ScoreRing(
                    score = insight.consistencyScore,
                    label = "Nhất quán",
                    modifier = Modifier.weight(1f)
                )
                ScoreRing(
                    score = insight.recoveryScore,
                    label = "Phục hồi",
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(Modifier.height(12.dp))

            BurnoutPill(insight.burnoutRisk)

            // ─── Insight + coaching ──────────────────────────────
            if (insight.primaryInsight.isNotBlank()) {
                Spacer(Modifier.height(12.dp))
                Text(
                    text = "💬  ${insight.primaryInsight}",
                    style = BetterMeTypography.Body.Medium.copy(fontWeight = FontWeight.SemiBold),
                    color = BetterMeColors.Text.TextPrimary
                )
            }
            if (insight.coachingMessage.isNotBlank()) {
                Spacer(Modifier.height(6.dp))
                Text(
                    text = insight.coachingMessage,
                    style = BetterMeTypography.Body.Small.Medium,
                    color = BetterMeColors.Text.TextSecondary
                )
            }

            // ─── Adaptive suggestions ────────────────────────────
            if (insight.adaptiveSuggestions.isNotEmpty()) {
                Spacer(Modifier.height(14.dp))
                Text(
                    text = "Gợi ý điều chỉnh",
                    style = BetterMeTypography.Title.Small.Bold,
                    color = BetterMeColors.Text.TextPrimary,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(8.dp))
                insight.adaptiveSuggestions.forEach { s ->
                    SuggestionRow(s)
                    Spacer(Modifier.height(8.dp))
                }
            }

            // ─── Footer ──────────────────────────────────────────
            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Pill(
                    label = "↻  Phân tích lại",
                    color = accent,
                    background = accent.copy(alpha = BetterMeTokens.AccentAlpha.Soft),
                    onClick = onRefresh
                )
                if (insight.isCanned) {
                    Spacer(Modifier.size(8.dp))
                    OfflineChip()
                }
            }
        }
    }
}

@Composable
private fun ScoreRing(score: Int, label: String, modifier: Modifier = Modifier) {
    val clamped = score.coerceIn(0, 100)
    val color = when {
        clamped >= 70 -> Color(0xFF16A34A)
        clamped >= 50 -> Color(0xFFEA580C)
        else -> Color(0xFFDC2626)
    }
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(BetterMeTokens.CardRadius.Body))
            .background(BetterMeColors.BackGround.BackgroundSecondary)
            .padding(vertical = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(modifier = Modifier.size(56.dp), contentAlignment = Alignment.Center) {
                Canvas(modifier = Modifier.size(56.dp)) {
                    val stroke = 6.dp.toPx()
                    val pad = stroke / 2
                    drawArc(
                        color = color.copy(alpha = 0.12f),
                        startAngle = -90f,
                        sweepAngle = 360f,
                        useCenter = false,
                        topLeft = Offset(pad, pad),
                        size = Size(size.width - stroke, size.height - stroke),
                        style = Stroke(width = stroke, cap = StrokeCap.Round)
                    )
                    drawArc(
                        color = color,
                        startAngle = -90f,
                        sweepAngle = 360f * (clamped / 100f),
                        useCenter = false,
                        topLeft = Offset(pad, pad),
                        size = Size(size.width - stroke, size.height - stroke),
                        style = Stroke(width = stroke, cap = StrokeCap.Round)
                    )
                }
                Text(
                    text = "$clamped",
                    style = BetterMeTypography.Body.Medium.copy(fontWeight = FontWeight.Bold),
                    color = BetterMeColors.Text.TextPrimary
                )
            }
            Text(
                text = label,
                style = BetterMeTypography.Body.Small.Medium,
                color = BetterMeColors.Text.TextSecondary,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun TrendPill(trend: OverallTrend) {
    val (emoji, label, color) = when (trend) {
        OverallTrend.IMPROVING -> Triple("📈", "Đang lên", Color(0xFF16A34A))
        OverallTrend.STABLE -> Triple("⚖️", "Ổn định", BetterMeColors.Primary.Primary)
        OverallTrend.DECLINING -> Triple("📉", "Đang giảm", Color(0xFFEA580C))
    }
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(BetterMeTokens.CardRadius.Pill))
            .background(color.copy(alpha = 0.12f))
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(
            text = "$emoji  $label",
            style = BetterMeTypography.Body.Small.Medium,
            color = color,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun BurnoutPill(risk: BurnoutRisk) {
    val (emoji, label, color) = when (risk) {
        BurnoutRisk.LOW -> Triple("✅", "Cân đối", Color(0xFF16A34A))
        BurnoutRisk.MODERATE -> Triple("⚠️", "Cần chú ý nhẹ", Color(0xFFEA580C))
        BurnoutRisk.HIGH -> Triple("🔥", "Nguy cơ kiệt sức", Color(0xFFDC2626))
    }
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(BetterMeTokens.CardRadius.Pill))
            .background(color.copy(alpha = 0.12f))
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(
            text = "$emoji  $label",
            style = BetterMeTypography.Body.Small.Medium,
            color = color,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun SuggestionRow(s: AdaptiveSuggestion) {
    val (emoji, accent) = decorationFor(s.type)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(BetterMeTokens.CardRadius.Body))
            .background(BetterMeColors.BackGround.BackgroundSecondary)
            .border(
                width = 1.dp,
                color = accent.copy(alpha = BetterMeTokens.AccentAlpha.Soft),
                shape = RoundedCornerShape(BetterMeTokens.CardRadius.Body)
            )
            .padding(12.dp),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(accent.copy(alpha = BetterMeTokens.AccentAlpha.Soft)),
            contentAlignment = Alignment.Center
        ) {
            Text(text = emoji, fontSize = 16.sp)
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = s.title,
                style = BetterMeTypography.Body.Medium.copy(fontWeight = FontWeight.SemiBold),
                color = BetterMeColors.Text.TextPrimary
            )
            if (s.reason.isNotBlank()) {
                Spacer(Modifier.height(2.dp))
                Text(
                    text = s.reason,
                    style = BetterMeTypography.Body.Small.Medium,
                    color = BetterMeColors.Text.TextTertiary
                )
            }
            if (s.suggestion.isNotBlank()) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "💡 ${s.suggestion}",
                    style = BetterMeTypography.Body.Small.Medium,
                    color = accent,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
private fun OfflineChip() {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(BetterMeTokens.CardRadius.Pill))
            .background(BetterMeColors.Gray.Gray3)
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Text(
            text = "📴 Bản phân tích nhanh",
            style = BetterMeTypography.Body.Small.Medium,
            color = BetterMeColors.Text.TextTertiary
        )
    }
}

@Composable
private fun Pill(
    label: String,
    color: Color,
    background: Color,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(BetterMeTokens.CardRadius.Pill))
            .background(background)
            .heightIn(min = 40.dp)
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = BetterMeTypography.Body.Small.Medium,
            color = color,
            fontWeight = FontWeight.SemiBold
        )
    }
}

private fun decorationFor(type: SuggestionType): Pair<String, Color> = when (type) {
    SuggestionType.REDUCE_INTENSITY -> "🌿" to Color(0xFF16A34A)
    SuggestionType.SIMPLIFY_ROUTINE -> "✂️" to Color(0xFF16A34A)
    SuggestionType.IMPROVE_SLEEP -> "🌙" to Color(0xFF6366F1)
    SuggestionType.REDUCE_OVERLOAD -> "🚦" to Color(0xFFEA580C)
    SuggestionType.ADD_RECOVERY -> "💆" to Color(0xFF6366F1)
    SuggestionType.IMPROVE_CONSISTENCY -> "📅" to Color(0xFFEA580C)
    SuggestionType.MAINTAIN_STABILITY -> "🎯" to Color(0xFF16A34A)
}
