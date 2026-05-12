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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.betterme.presentation.dailyhabits.DateUiModel
import com.example.betterme.presentation.theme.BetterMeColors
import com.example.betterme.presentation.theme.BetterMeTokens
import com.example.betterme.presentation.theme.BetterMeTypography

/**
 * Hero card for the Tasks tab.
 *
 * Layout
 * - Left column (Column with spacedBy(4dp)) holds the date title + secondary
 *   subtitle stacked vertically. Previously these lived in a horizontal chip
 *   row that wrapped awkwardly when the Vietnamese label "🌱 Bắt đầu nào" met
 *   the 96dp progress ring. Stacking matches the spec the user gave and never
 *   overflows.
 * - Right column: animated 96dp progress ring (Canvas, rounded caps) with a
 *   percent label that counts up via animateFloatAsState.
 *
 * Date-awareness
 * - When [isToday] is true, the title reads "Hôm nay" and the subtitle uses
 *   a motivational momentum line that shifts with completion fraction.
 * - When viewing the past, the title reads "Hôm qua" or "N ngày trước" and
 *   the subtitle calls out read-only mode + how the day went.
 * - When viewing the future, the title reads "Ngày mai" / "N ngày tới" and
 *   the subtitle frames the day as "Chỉ xem — chưa thể check-in".
 *
 * Read-only signal: when [isToday] is false, the progress ring uses a softer
 * accent (50% alpha) and the ring's center label drops the "hôm nay" caption
 * — both communicate "you're looking at history/preview, not interacting".
 */
@Composable
fun TasksHeroCard(
    completedCount: Int,
    totalCount: Int,
    selectedDate: DateUiModel?,
    isToday: Boolean,
    dayOffset: Int,
    modifier: Modifier = Modifier
) {
    val fraction = if (totalCount > 0) completedCount.toFloat() / totalCount else 0f
    val animatedFraction by animateFloatAsState(
        targetValue = fraction.coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 600),
        label = "tasks_progress"
    )
    val accent = BetterMeColors.Primary.Primary
    val titleText = titleFor(isToday, dayOffset, selectedDate)
    val subtitleText = subtitleFor(isToday, dayOffset, fraction, completedCount, totalCount)

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
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Title — vertical stack per spec. "Hôm nay" / "Hôm qua" / "Ngày mai".
                Text(
                    text = titleText,
                    style = BetterMeTypography.Title.Medium.Bold,
                    color = BetterMeColors.Text.TextPrimary,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                // Secondary subtitle — motivational on today, read-only on other days.
                Text(
                    text = subtitleText,
                    style = BetterMeTypography.Body.Small.Medium,
                    color = BetterMeColors.Text.TextSecondary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(Modifier.height(6.dp))

                // Counter block.
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
                    text = footerFor(isToday, completedCount, totalCount),
                    style = BetterMeTypography.Body.Small.Medium,
                    color = BetterMeColors.Text.TextTertiary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(Modifier.size(14.dp))
            ProgressRing(
                fraction = animatedFraction,
                accent = accent,
                percent = (animatedFraction * 100).toInt(),
                isToday = isToday
            )
        }
    }
}

@Composable
private fun ProgressRing(
    fraction: Float,
    accent: Color,
    percent: Int,
    isToday: Boolean
) {
    val foregroundAlpha = if (isToday) 1f else 0.5f
    Box(
        modifier = Modifier.size(96.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(96.dp)) {
            val stroke = 10.dp.toPx()
            val padding = stroke / 2
            // Background ring — subtle so foreground reads clearly even at 0%.
            drawArc(
                color = accent.copy(alpha = 0.12f),
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = Offset(padding, padding),
                size = Size(size.width - stroke, size.height - stroke),
                style = Stroke(width = stroke, cap = StrokeCap.Round)
            )
            // Foreground sweep. Dimmer on non-today so the ring communicates
            // "view-only" without changing visual structure.
            drawArc(
                color = accent.copy(alpha = foregroundAlpha),
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
                color = BetterMeColors.Text.TextPrimary.copy(alpha = if (isToday) 1f else 0.7f),
                fontWeight = FontWeight.Bold
            )
            Text(
                text = if (isToday) "hôm nay" else "đã xong",
                style = BetterMeTypography.Body.Small.Medium,
                color = BetterMeColors.Text.TextTertiary
            )
        }
    }
}

// ============================================================
// COPY HELPERS
// ============================================================

/**
 * Big title line. Today + immediate neighbours get spoken-language labels;
 * further days fall back to absolute "d/m" so the user gets a precise anchor.
 */
private fun titleFor(isToday: Boolean, dayOffset: Int, selectedDate: DateUiModel?): String {
    if (isToday) return "Hôm nay"
    return when (dayOffset) {
        -1 -> "Hôm qua"
        1 -> "Ngày mai"
        in Int.MIN_VALUE..-2 -> "${-dayOffset} ngày trước"
        in 2..Int.MAX_VALUE -> "$dayOffset ngày tới"
        else -> selectedDate?.let { "${it.day}/${it.month}" } ?: ""
    }
}

/**
 * Subtitle. Today: motivational (momentum-aware). Past: how the day went.
 * Future: framed as planning/preview. Always one line conceptually.
 */
private fun subtitleFor(
    isToday: Boolean,
    dayOffset: Int,
    fraction: Float,
    completed: Int,
    total: Int
): String {
    if (isToday) {
        return when {
            total == 0 -> "Hôm nay chưa có nhiệm vụ nào"
            fraction <= 0f -> "Bắt đầu nào ✨"
            fraction < 0.34f -> "Đang khởi động — bước nhỏ là đủ"
            fraction < 0.67f -> "Đang đà tốt — tiếp tục nhé"
            fraction < 1f -> "Sắp về đích rồi 💪"
            else -> "Hoàn hảo — bạn đã làm hết ✨"
        }
    }
    val past = dayOffset < 0
    if (total == 0) {
        return if (past) "Ngày này không có nhiệm vụ nào"
        else "Ngày này chưa có nhiệm vụ nào lên kế hoạch"
    }
    return if (past) "Chỉ xem — $completed/$total đã hoàn thành"
    else "Chỉ xem — chưa thể check-in"
}

/** Trailing helper line under the counter. */
private fun footerFor(isToday: Boolean, completed: Int, total: Int): String {
    val remaining = (total - completed).coerceAtLeast(0)
    if (!isToday) return "Chỉ xem — không thể check-in ngày này"
    return when {
        total == 0 -> "Thêm thói quen để bắt đầu"
        remaining == 0 -> "Tuyệt vời — bạn đã làm hết!"
        else -> "Còn $remaining nhiệm vụ chưa hoàn thành"
    }
}
