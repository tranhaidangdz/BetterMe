package com.example.betterme.presentation.dailyhabits.schedule

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.betterme.domain.ai.schedule.BurnoutRisk
import com.example.betterme.domain.ai.schedule.ConflictType
import com.example.betterme.domain.ai.schedule.OptimizedHabitTime
import com.example.betterme.domain.ai.schedule.ScheduleAnalysis
import com.example.betterme.domain.ai.schedule.ScheduleConflict
import com.example.betterme.presentation.theme.BetterMeColors
import com.example.betterme.presentation.theme.BetterMeTokens
import com.example.betterme.presentation.theme.BetterMeTypography

/**
 * Premium bottom sheet that renders [ScheduleAnalysisUi].
 *
 * Sections (when Success):
 * - Header: title + close
 * - Score ring (Canvas, accent color) + burnout-risk pill
 * - Positive feedback line (skipped when empty)
 * - Conflicts list (cards, max 3)
 * - Optimized schedule list (rows, max 5) + "Áp dụng đề xuất" CTA when non-empty
 * - "Phân tích lại" pill (forces a fresh AI call)
 *
 * Loading shows a spinner + a one-line "đang phân tích…" message. Error shows
 * a friendly Vietnamese line + a retry pill. Idle returns nothing — the parent
 * shouldn't render this composable at all when state is Idle, but the guard
 * keeps the composition safe.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduleAnalysisBottomSheet(
    state: ScheduleAnalysisState,
    onIntent: (ScheduleAnalysisIntent) -> Unit
) {
    if (state.ui is ScheduleAnalysisUi.Idle) return

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = { onIntent(ScheduleAnalysisIntent.Dismiss) },
        sheetState = sheetState,
        containerColor = Color.White,
        dragHandle = null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 18.dp)
                .heightIn(min = 200.dp)
        ) {
            Header(onClose = { onIntent(ScheduleAnalysisIntent.Dismiss) })
            Spacer(Modifier.height(12.dp))

            when (val ui = state.ui) {
                ScheduleAnalysisUi.Idle -> Unit
                ScheduleAnalysisUi.Loading -> LoadingBody()
                is ScheduleAnalysisUi.Success -> SuccessBody(
                    analysis = ui.analysis,
                    isApplying = state.isApplying,
                    appliedCount = ui.appliedCount,
                    onApply = { onIntent(ScheduleAnalysisIntent.ApplySuggestions) },
                    onRegenerate = {
                        onIntent(ScheduleAnalysisIntent.Analyze(forceRefresh = true))
                    },
                    onClearToast = { onIntent(ScheduleAnalysisIntent.ClearAppliedToast) }
                )
                is ScheduleAnalysisUi.Error -> ErrorBody(
                    message = ui.message,
                    onRetry = { onIntent(ScheduleAnalysisIntent.Analyze(forceRefresh = true)) }
                )
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun Header(onClose: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Phân tích lịch trình",
                style = BetterMeTypography.Title.Medium.Bold,
                color = BetterMeColors.Text.TextPrimary,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Huấn luyện viên AI sẽ giúp bạn cân bằng nhiệm vụ trong ngày",
                style = BetterMeTypography.Body.Small.Medium,
                color = BetterMeColors.Text.TextTertiary
            )
        }
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .clickable { onClose() },
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "✕",
                color = BetterMeColors.Text.TextTertiary,
                fontSize = 18.sp
            )
        }
    }
}

@Composable
private fun LoadingBody() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 24.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(22.dp),
            color = BetterMeColors.Primary.Primary,
            strokeWidth = 2.5.dp
        )
        Text(
            text = "Đang phân tích lịch trình của bạn…",
            style = BetterMeTypography.Body.Medium,
            color = BetterMeColors.Text.TextSecondary
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
        Pill(
            label = "↻  Thử lại",
            color = errorColor,
            background = errorColor.copy(alpha = 0.10f),
            onClick = onRetry
        )
    }
}

@Composable
private fun SuccessBody(
    analysis: ScheduleAnalysis,
    isApplying: Boolean,
    appliedCount: Int?,
    onApply: () -> Unit,
    onRegenerate: () -> Unit,
    onClearToast: () -> Unit
) {
    // Auto-clear the applied-count snackbar marker after ~2s so the sheet
    // doesn't sit permanently in a "just applied N" state if the user lingers.
    LaunchedEffect(appliedCount) {
        if (appliedCount != null) {
            kotlinx.coroutines.delay(2400)
            onClearToast()
        }
    }

    val accent = BetterMeColors.Primary.Primary

    Column {
        // ─── Hero row: ring + summary + burnout pill ─────────────────
        Row(verticalAlignment = Alignment.CenterVertically) {
            ScoreRing(score = analysis.scheduleScore, accent = accent)
            Spacer(Modifier.size(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                BurnoutPill(risk = analysis.burnoutRisk)
                Spacer(Modifier.height(6.dp))
                Text(
                    text = analysis.summary.ifBlank { "Không có nhận xét tổng thể." },
                    style = BetterMeTypography.Body.Medium,
                    color = BetterMeColors.Text.TextPrimary
                )
            }
        }

        if (analysis.positiveFeedback.isNotBlank()) {
            Spacer(Modifier.height(10.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(BetterMeTokens.CardRadius.Body))
                    .background(BetterMeColors.Green.copy(alpha = 0.08f))
                    .border(
                        width = 1.dp,
                        color = BetterMeColors.Green.copy(alpha = 0.30f),
                        shape = RoundedCornerShape(BetterMeTokens.CardRadius.Body)
                    )
                    .padding(horizontal = 12.dp, vertical = 10.dp)
            ) {
                Text(
                    text = "🌱  ${analysis.positiveFeedback}",
                    style = BetterMeTypography.Body.Small.Medium,
                    color = BetterMeColors.Text.TextPrimary
                )
            }
        }

        // ─── Conflicts ───────────────────────────────────────────────
        if (analysis.conflicts.isNotEmpty()) {
            Spacer(Modifier.height(16.dp))
            Text(
                text = "Cần điều chỉnh",
                style = BetterMeTypography.Title.Small.Bold,
                color = BetterMeColors.Text.TextPrimary,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(8.dp))
            // The list is bounded to ≤3 by the prompt and the repo's parser, so
            // a Column is fine — no LazyColumn nesting headache inside the sheet.
            analysis.conflicts.forEach { conflict ->
                ConflictCard(conflict = conflict)
                Spacer(Modifier.height(8.dp))
            }
        }

        // ─── Optimized schedule + Apply ──────────────────────────────
        if (analysis.optimizedSchedule.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Gợi ý sắp xếp lại",
                style = BetterMeTypography.Title.Small.Bold,
                color = BetterMeColors.Text.TextPrimary,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(6.dp))
            analysis.optimizedSchedule.forEach { opt ->
                OptimizedRow(opt)
                Spacer(Modifier.height(4.dp))
            }
            Spacer(Modifier.height(10.dp))
            ApplyButton(
                isApplying = isApplying,
                appliedCount = appliedCount,
                onApply = onApply
            )
        }

        // ─── Footer actions ─────────────────────────────────────────
        Spacer(Modifier.height(14.dp))
        Row {
            Pill(
                label = "↻  Phân tích lại",
                color = accent,
                background = accent.copy(alpha = BetterMeTokens.AccentAlpha.Soft),
                onClick = onRegenerate
            )
            if (analysis.isCanned) {
                Spacer(Modifier.size(8.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(BetterMeTokens.CardRadius.Pill))
                        .background(BetterMeColors.Gray.Gray3)
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "Bản phân tích nhanh (ngoại tuyến)",
                        style = BetterMeTypography.Body.Small.Medium,
                        color = BetterMeColors.Text.TextTertiary
                    )
                }
            }
        }
    }
}

@Composable
private fun ScoreRing(score: Int, accent: Color) {
    val clamped = score.coerceIn(0, 100)
    val fraction = clamped / 100f
    val color = when {
        clamped >= 70 -> Color(0xFF16A34A)
        clamped >= 50 -> Color(0xFFEA580C)
        else -> Color(0xFFDC2626)
    }
    Box(
        modifier = Modifier.size(84.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(84.dp)) {
            val stroke = 9.dp.toPx()
            val pad = stroke / 2
            drawArc(
                color = accent.copy(alpha = 0.10f),
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
                sweepAngle = 360f * fraction,
                useCenter = false,
                topLeft = Offset(pad, pad),
                size = Size(size.width - stroke, size.height - stroke),
                style = Stroke(width = stroke, cap = StrokeCap.Round)
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "$clamped",
                style = BetterMeTypography.Title.Small.Bold,
                color = BetterMeColors.Text.TextPrimary,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "điểm",
                style = BetterMeTypography.Body.Small.Medium,
                color = BetterMeColors.Text.TextTertiary
            )
        }
    }
}

@Composable
private fun BurnoutPill(risk: BurnoutRisk) {
    val (label, color) = when (risk) {
        BurnoutRisk.LOW -> "Cân đối" to Color(0xFF16A34A)
        BurnoutRisk.MODERATE -> "Tải vừa phải" to Color(0xFFEA580C)
        BurnoutRisk.HIGH -> "Nguy cơ kiệt sức" to Color(0xFFDC2626)
    }
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(BetterMeTokens.CardRadius.Pill))
            .background(color.copy(alpha = 0.12f))
            .padding(horizontal = 10.dp, vertical = 3.dp)
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
private fun ConflictCard(conflict: ScheduleConflict) {
    val (emoji, accent) = decorationFor(conflict.type)
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
                .size(36.dp)
                .clip(CircleShape)
                .background(accent.copy(alpha = BetterMeTokens.AccentAlpha.Soft)),
            contentAlignment = Alignment.Center
        ) {
            Text(text = emoji, fontSize = 18.sp)
        }
        Column(modifier = Modifier.weight(1f)) {
            val title = when {
                conflict.habitB.isNullOrBlank() -> conflict.habitA
                else -> "${conflict.habitA} ↔ ${conflict.habitB}"
            }
            Text(
                text = title,
                style = BetterMeTypography.Body.Medium.copy(fontWeight = FontWeight.SemiBold),
                color = BetterMeColors.Text.TextPrimary
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = conflict.issue,
                style = BetterMeTypography.Body.Small.Medium,
                color = BetterMeColors.Text.TextSecondary
            )
            if (conflict.suggestion.isNotBlank()) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "💡  ${conflict.suggestion}",
                    style = BetterMeTypography.Body.Small.Medium,
                    color = accent,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
private fun OptimizedRow(opt: OptimizedHabitTime) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "•  ${opt.habit}",
            style = BetterMeTypography.Body.Medium,
            color = BetterMeColors.Text.TextPrimary,
            modifier = Modifier.weight(1f)
        )
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(BetterMeTokens.CardRadius.Pill))
                .background(BetterMeColors.Primary.Primary.copy(alpha = BetterMeTokens.AccentAlpha.Soft))
                .padding(horizontal = 10.dp, vertical = 3.dp)
        ) {
            Text(
                text = "⏰ ${opt.suggestedTime}",
                style = BetterMeTypography.Body.Small.Medium,
                color = BetterMeColors.Primary.Primary,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun ApplyButton(
    isApplying: Boolean,
    appliedCount: Int?,
    onApply: () -> Unit
) {
    val accent = BetterMeColors.Primary.Primary
    val label = when {
        isApplying -> "Đang áp dụng…"
        appliedCount != null -> "✓ Đã áp dụng $appliedCount đề xuất"
        else -> "Áp dụng đề xuất"
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(BetterMeTokens.CardRadius.Pill))
            .background(if (appliedCount != null) Color(0xFF16A34A) else accent)
            .clickable(enabled = !isApplying && appliedCount == null) { onApply() }
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = BetterMeTypography.Body.Medium,
            color = Color.White,
            fontWeight = FontWeight.SemiBold
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

private fun decorationFor(type: ConflictType): Pair<String, Color> = when (type) {
    ConflictType.OVERLAP -> "🔀" to Color(0xFFEA580C)
    ConflictType.OVERLOAD -> "🚦" to Color(0xFFEA580C)
    ConflictType.POOR_SLEEP -> "🌙" to Color(0xFF6366F1)
    ConflictType.LATE_NIGHT -> "🌒" to Color(0xFF6366F1)
    ConflictType.TRANSITION -> "↔" to Color(0xFFEA580C)
}
