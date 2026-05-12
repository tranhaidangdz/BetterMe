package com.example.betterme.presentation.dailyhabits.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
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
import androidx.compose.runtime.getValue
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
import com.example.betterme.presentation.dailyhabits.DateUiModel
import com.example.betterme.presentation.theme.BetterMeColors
import com.example.betterme.presentation.theme.BetterMeTokens
import com.example.betterme.presentation.theme.BetterMeTypography

/**
 * Hero card for the Tasks tab. Sits directly under the top bar and replaces the
 * prior plain header.
 *
 * Visual anatomy (left → right):
 * - Left column: greeting / motivational subtitle, two info chips (today date
 *   + momentum), and the "X/Y nhiệm vụ" + "còn lại" counters.
 * - Right column: an animated circular progress ring rendered with Canvas so the
 *   stroke width and rounded caps look intentional (Material's LinearProgressIndicator
 *   reads as utilitarian; a custom ring fits the premium tone of the rest of
 *   the app — see CategorySummaryCard, ChallengeDetail hero).
 *
 * The ring's sweep animates with [animateFloatAsState] so a check-in nudges the
 * ring smoothly instead of jumping. The percentage label inside the ring uses the
 * same animated value so the number visibly counts up.
 */
@Composable
fun TasksHeroCard(
    completedCount: Int,
    totalCount: Int,
    selectedDate: DateUiModel?,
    modifier: Modifier = Modifier
) {
    val fraction = if (totalCount > 0) completedCount.toFloat() / totalCount else 0f
    val animatedFraction by animateFloatAsState(
        targetValue = fraction.coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 600),
        label = "tasks_progress"
    )
    val remaining = (totalCount - completedCount).coerceAtLeast(0)
    val momentum = momentumLabel(fraction)
    val accent = BetterMeColors.Primary.Primary

    Box(
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
                Brush.linearGradient(
                    colors = listOf(
                        Color.White,
                        accent.copy(alpha = 0.08f)
                    )
                )
            )
            .border(
                width = 1.dp,
                color = accent.copy(alpha = BetterMeTokens.AccentAlpha.Medium),
                shape = RoundedCornerShape(BetterMeTokens.CardRadius.Hero)
            )
            .padding(horizontal = 18.dp, vertical = 18.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Hôm nay của bạn",
                    style = BetterMeTypography.Body.Small.Medium,
                    color = BetterMeColors.Text.TextTertiary
                )
                Text(
                    text = "Hoàn thành từng bước nhỏ hôm nay ✨",
                    style = BetterMeTypography.Title.Small.Bold,
                    color = BetterMeColors.Text.TextPrimary,
                    fontWeight = FontWeight.Bold
                )
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (selectedDate != null) {
                        InfoChip(
                            label = "📅 ${dateChipLabel(selectedDate)}",
                            accent = accent
                        )
                    }
                    InfoChip(
                        label = momentum.label,
                        accent = momentum.color
                    )
                }
                Spacer(Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = "$completedCount",
                        style = BetterMeTypography.Headline.Small.Bold,
                        color = BetterMeColors.Text.TextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "/$totalCount nhiệm vụ",
                        style = BetterMeTypography.Body.Medium,
                        color = BetterMeColors.Text.TextSecondary,
                        modifier = Modifier.padding(start = 4.dp, bottom = 3.dp)
                    )
                }
                Text(
                    text = if (totalCount == 0) "Hôm nay chưa có nhiệm vụ nào"
                    else if (remaining == 0) "Tuyệt vời — bạn đã làm hết!"
                    else "Còn $remaining nhiệm vụ chưa hoàn thành",
                    style = BetterMeTypography.Body.Small.Medium,
                    color = BetterMeColors.Text.TextTertiary
                )
            }
            Spacer(Modifier.size(14.dp))
            ProgressRing(
                fraction = animatedFraction,
                accent = accent,
                percent = (animatedFraction * 100).toInt()
            )
        }
    }
}

@Composable
private fun InfoChip(label: String, accent: Color) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(BetterMeTokens.CardRadius.Pill))
            .background(accent.copy(alpha = BetterMeTokens.AccentAlpha.Soft))
            .border(
                width = 1.dp,
                color = accent.copy(alpha = BetterMeTokens.AccentAlpha.Medium),
                shape = RoundedCornerShape(BetterMeTokens.CardRadius.Pill)
            )
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(
            text = label,
            style = BetterMeTypography.Body.Small.Medium,
            color = accent,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun ProgressRing(
    fraction: Float,
    accent: Color,
    percent: Int
) {
    Box(
        modifier = Modifier.size(96.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(96.dp)) {
            val stroke = 10.dp.toPx()
            val padding = stroke / 2
            // Background ring — subtle so the foreground reads clearly even at 0%.
            drawArc(
                color = accent.copy(alpha = 0.12f),
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = Offset(padding, padding),
                size = Size(size.width - stroke, size.height - stroke),
                style = Stroke(width = stroke, cap = StrokeCap.Round)
            )
            // Foreground sweep.
            drawArc(
                color = accent,
                startAngle = -90f,
                sweepAngle = 360f * fraction,
                useCenter = false,
                topLeft = Offset(padding, padding),
                size = Size(size.width - stroke, size.height - stroke),
                style = Stroke(width = stroke, cap = StrokeCap.Round)
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "$percent%",
                style = BetterMeTypography.Title.Small.Bold,
                color = BetterMeColors.Text.TextPrimary,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "hôm nay",
                style = BetterMeTypography.Body.Small.Medium,
                color = BetterMeColors.Text.TextTertiary
            )
        }
    }
}

private data class Momentum(val label: String, val color: Color)

/**
 * Maps completion fraction to a momentum chip — purely decorative copy so the
 * card has personality at any state. Tunable from one place if the user wants
 * a different tone.
 */
private fun momentumLabel(fraction: Float): Momentum = when {
    fraction <= 0f -> Momentum("🌱 Bắt đầu nào", Color(0xFFEA580C))
    fraction < 0.34f -> Momentum("⚡ Đang khởi động", Color(0xFFEA580C))
    fraction < 0.67f -> Momentum("🔥 Đang đà tốt", Color(0xFFDB7B0A))
    fraction < 1f -> Momentum("💪 Sắp về đích", Color(0xFF16A34A))
    else -> Momentum("✨ Hoàn hảo", Color(0xFF16A34A))
}

private fun dateChipLabel(date: DateUiModel): String =
    if (date.isToday) "Hôm nay • ${date.day}/${date.month}"
    else "${date.weekDay} • ${date.day}/${date.month}"
