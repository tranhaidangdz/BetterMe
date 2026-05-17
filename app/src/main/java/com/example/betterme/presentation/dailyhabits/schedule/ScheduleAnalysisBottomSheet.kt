package com.example.betterme.presentation.dailyhabits.schedule

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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
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
import kotlinx.coroutines.delay

/**
 * Premium bottom sheet that renders [ScheduleAnalysisUi].
 *
 * Visual hierarchy:
 *   header → hero (score ring + burnout pill + summary) → optional positive
 *   feedback card → conflicts → optimized schedule + Apply CTA → footer pills.
 *
 * Empty-state polish:
 *   - "Chưa đủ dữ liệu" gets a calm sun-icon hero with helper copy instead of
 *     the standard zero-conflict success treatment.
 *   - hasConflict = false with non-empty habits gets a green celebratory hero.
 *   - All-models-failed (isCanned = true) shows a quiet "Bản phân tích nhanh
 *     (ngoại tuyến)" chip so the user knows why suggestions are sparse.
 *
 * Loading message rotates every ~1.8s. The rotation is implemented as a
 * single [LaunchedEffect] inside the Loading branch, so it dies with the
 * Loading composition and never leaks past a state transition.
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
            Spacer(Modifier.height(14.dp))

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

// ============================================================
// HEADER
// ============================================================
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
                text = "Huấn luyện viên AI giúp bạn cân bằng nhiệm vụ trong ngày",
                style = BetterMeTypography.Body.Small.Medium,
                color = BetterMeColors.Text.TextTertiary
            )
        }
        // 48dp touch target for accessibility (visual 36dp circle, transparent
        // padding extends the click area).
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .clickable { onClose() }
                .semantics { contentDescription = "Đóng phân tích lịch trình" },
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(BetterMeColors.Gray.Gray3),
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
}

// ============================================================
// LOADING — rotating messages
// ============================================================
@Composable
private fun LoadingBody() {
    val messages = remember {
        listOf(
            "Đang phân tích lịch trình của bạn…",
            "Soát cân bằng giữa các thói quen…",
            "Tìm khoảng nghỉ phục hồi…",
            "Kiểm tra giờ ngủ và giờ làm việc…",
            "Phát hiện các điểm có nguy cơ kiệt sức…"
        )
    }
    var index by remember { mutableStateOf(0) }
    // Single LaunchedEffect scoped to the Loading composition. When the UI
    // transitions to Success/Error the whole branch leaves composition and
    // this coroutine is cancelled automatically — no timers leak past the
    // visible Loading state.
    LaunchedEffect(messages) {
        while (true) {
            delay(1800)
            index = (index + 1) % messages.size
        }
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(28.dp),
            color = BetterMeColors.Primary.Primary,
            strokeWidth = 2.6.dp
        )
        AnimatedContent(
            targetState = messages[index],
            transitionSpec = {
                (fadeIn(tween(280)) togetherWith fadeOut(tween(180)))
            },
            label = "loading_rotator"
        ) { msg ->
            Text(
                text = msg,
                style = BetterMeTypography.Body.Medium,
                color = BetterMeColors.Text.TextSecondary
            )
        }
    }
}

// ============================================================
// ERROR
// ============================================================
@Composable
private fun ErrorBody(message: String, onRetry: () -> Unit) {
    val errorColor = BetterMeColors.Red
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = "⚠️  $message",
            style = BetterMeTypography.Body.Medium,
            color = errorColor
        )
        Pill(
            label = "↻  Thử lại",
            color = errorColor,
            background = errorColor.copy(alpha = 0.10f),
            onClick = onRetry
        )
    }
}

// ============================================================
// SUCCESS — main composable
// ============================================================
@Composable
private fun SuccessBody(
    analysis: ScheduleAnalysis,
    isApplying: Boolean,
    appliedCount: Int?,
    onApply: () -> Unit,
    onRegenerate: () -> Unit,
    onClearToast: () -> Unit
) {
    // Auto-clear the applied-count snackbar marker after ~2.4s. Effect is keyed
    // on appliedCount so re-triggers don't pile up.
    LaunchedEffect(appliedCount) {
        if (appliedCount != null) {
            delay(2400)
            onClearToast()
        }
    }

    val isEmptyData = analysis.summary == "Chưa đủ dữ liệu để phân tích"

    when {
        isEmptyData -> EmptyDataHero(onRegenerate = onRegenerate)
        !analysis.hasConflict && analysis.conflicts.isEmpty() -> CelebratoryHero(
            analysis = analysis,
            onRegenerate = onRegenerate
        )
        else -> ConflictsBody(
            analysis = analysis,
            isApplying = isApplying,
            appliedCount = appliedCount,
            onApply = onApply,
            onRegenerate = onRegenerate
        )
    }
}

// ============================================================
// EMPTY-DATA hero (<2 habits with reminders)
// ============================================================
@Composable
private fun EmptyDataHero(onRegenerate: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(BetterMeColors.Primary.Primary.copy(alpha = BetterMeTokens.AccentAlpha.Soft)),
            contentAlignment = Alignment.Center
        ) {
            Text(text = "🌤️", fontSize = 34.sp)
        }
        Spacer(Modifier.height(12.dp))
        Text(
            text = "Cần thêm dữ liệu để phân tích",
            style = BetterMeTypography.Title.Small.Bold,
            color = BetterMeColors.Text.TextPrimary,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = "Thêm vài thói quen có giờ nhắc, mình sẽ giúp bạn cân đối lịch trình hơn.",
            style = BetterMeTypography.Body.Small.Medium,
            color = BetterMeColors.Text.TextSecondary,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        Spacer(Modifier.height(14.dp))
        Pill(
            label = "↻  Thử lại",
            color = BetterMeColors.Primary.Primary,
            background = BetterMeColors.Primary.Primary.copy(alpha = BetterMeTokens.AccentAlpha.Soft),
            onClick = onRegenerate
        )
    }
}

// ============================================================
// NO-CONFLICTS celebratory hero
// ============================================================
@Composable
private fun CelebratoryHero(
    analysis: ScheduleAnalysis,
    onRegenerate: () -> Unit
) {
    val green = Color(0xFF16A34A)
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            ScoreRing(score = analysis.scheduleScore, accentOverride = green)
            Spacer(Modifier.size(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                BurnoutPill(risk = analysis.burnoutRisk)
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "✨ ${analysis.summary}",
                    style = BetterMeTypography.Body.Medium,
                    color = BetterMeColors.Text.TextPrimary
                )
            }
        }
        if (analysis.positiveFeedback.isNotBlank()) {
            Spacer(Modifier.height(12.dp))
            PositiveCard(text = analysis.positiveFeedback)
        }
        Spacer(Modifier.height(16.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Pill(
                label = "↻  Phân tích lại",
                color = BetterMeColors.Primary.Primary,
                background = BetterMeColors.Primary.Primary.copy(alpha = BetterMeTokens.AccentAlpha.Soft),
                onClick = onRegenerate
            )
            if (analysis.isCanned) {
                Spacer(Modifier.size(8.dp))
                OfflineChip()
            }
        }
    }
}

// ============================================================
// CONFLICTS body (analysis has issues to surface)
// ============================================================
@Composable
private fun ConflictsBody(
    analysis: ScheduleAnalysis,
    isApplying: Boolean,
    appliedCount: Int?,
    onApply: () -> Unit,
    onRegenerate: () -> Unit
) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            ScoreRing(score = analysis.scheduleScore)
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
            Spacer(Modifier.height(12.dp))
            PositiveCard(text = analysis.positiveFeedback)
        }

        if (analysis.conflicts.isNotEmpty()) {
            Spacer(Modifier.height(18.dp))
            SectionLabel(text = "Cần điều chỉnh", count = analysis.conflicts.size)
            Spacer(Modifier.height(10.dp))
            analysis.conflicts.forEach { conflict ->
                ConflictCard(conflict = conflict)
                Spacer(Modifier.height(8.dp))
            }
        }

        if (analysis.optimizedSchedule.isNotEmpty()) {
            Spacer(Modifier.height(10.dp))
            SectionLabel(text = "Gợi ý sắp xếp lại", count = analysis.optimizedSchedule.size)
            Spacer(Modifier.height(8.dp))
            analysis.optimizedSchedule.forEach { opt ->
                OptimizedRow(opt)
                Spacer(Modifier.height(6.dp))
            }
            Spacer(Modifier.height(12.dp))
            ApplyButton(
                isApplying = isApplying,
                appliedCount = appliedCount,
                onApply = onApply
            )
        }

        Spacer(Modifier.height(16.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Pill(
                label = "↻  Phân tích lại",
                color = BetterMeColors.Primary.Primary,
                background = BetterMeColors.Primary.Primary.copy(alpha = BetterMeTokens.AccentAlpha.Soft),
                onClick = onRegenerate
            )
            if (analysis.isCanned) {
                Spacer(Modifier.size(8.dp))
                OfflineChip()
            }
        }
    }
}

// ============================================================
// SUB-COMPONENTS
// ============================================================
@Composable
private fun SectionLabel(text: String, count: Int) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = text,
            style = BetterMeTypography.Title.Small.Bold,
            color = BetterMeColors.Text.TextPrimary,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f)
        )
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(BetterMeTokens.CardRadius.Pill))
                .background(BetterMeColors.Gray.Gray3)
                .padding(horizontal = 10.dp, vertical = 2.dp)
        ) {
            Text(
                text = "$count",
                style = BetterMeTypography.Body.Small.Medium,
                color = BetterMeColors.Text.TextSecondary,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun PositiveCard(text: String) {
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
            text = "🌱  $text",
            style = BetterMeTypography.Body.Small.Medium,
            color = BetterMeColors.Text.TextPrimary
        )
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
private fun ScoreRing(score: Int, accentOverride: Color? = null) {
    val clamped = score.coerceIn(0, 100)
    val fraction = clamped / 100f
    val color = accentOverride ?: when {
        clamped >= 70 -> Color(0xFF16A34A)
        clamped >= 50 -> Color(0xFFEA580C)
        else -> Color(0xFFDC2626)
    }
    val label = when {
        clamped >= 70 -> "Bền vững"
        clamped >= 50 -> "Tạm ổn"
        else -> "Cần điều chỉnh"
    }
    Box(
        modifier = Modifier
            .size(88.dp)
            .semantics { contentDescription = "Điểm lịch trình $clamped trên 100, $label" },
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(88.dp)) {
            val stroke = 9.dp.toPx()
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
                text = label,
                style = BetterMeTypography.Body.Small.Medium,
                color = BetterMeColors.Text.TextTertiary,
                fontSize = 10.sp
            )
        }
    }
}

@Composable
private fun BurnoutPill(risk: BurnoutRisk) {
    val (emoji, label, color) = when (risk) {
        BurnoutRisk.LOW -> Triple("✅", "Cân đối", Color(0xFF16A34A))
        BurnoutRisk.MODERATE -> Triple("⚠️", "Tải vừa phải", Color(0xFFEA580C))
        BurnoutRisk.HIGH -> Triple("🔥", "Nguy cơ kiệt sức", Color(0xFFDC2626))
    }
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(BetterMeTokens.CardRadius.Pill))
            .background(color.copy(alpha = 0.12f))
            .padding(horizontal = 10.dp, vertical = 3.dp)
            .semantics { contentDescription = "Mức cảnh báo kiệt sức: $label" }
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
private fun ConflictCard(conflict: ScheduleConflict) {
    val (emoji, accent) = decorationFor(conflict.type)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(BetterMeTokens.CardRadius.Body))
            .background(BetterMeColors.BackGround.BackgroundSecondary)
            .border(
                width = 1.dp,
                color = accent.copy(alpha = BetterMeTokens.AccentAlpha.Medium),
                shape = RoundedCornerShape(BetterMeTokens.CardRadius.Body)
            )
            .padding(14.dp),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(accent.copy(alpha = BetterMeTokens.AccentAlpha.Soft)),
            contentAlignment = Alignment.Center
        ) {
            Text(text = emoji, fontSize = 18.sp)
        }
        Column(modifier = Modifier.weight(1f)) {
            val title = when {
                conflict.habitB.isNullOrBlank() -> conflict.habitA
                else -> "${conflict.habitA}  ↔  ${conflict.habitB}"
            }
            Text(
                text = title,
                style = BetterMeTypography.Body.Medium.copy(fontWeight = FontWeight.SemiBold),
                color = BetterMeColors.Text.TextPrimary
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = conflict.issue,
                style = BetterMeTypography.Body.Small.Medium,
                color = BetterMeColors.Text.TextSecondary
            )
            if (conflict.suggestion.isNotBlank()) {
                Spacer(Modifier.height(6.dp))
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
            .clip(RoundedCornerShape(BetterMeTokens.CardRadius.Body))
            .background(BetterMeColors.Primary.Primary.copy(alpha = BetterMeTokens.AccentAlpha.Subtle))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = opt.habit,
            style = BetterMeTypography.Body.Medium,
            color = BetterMeColors.Text.TextPrimary,
            modifier = Modifier.weight(1f)
        )
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(BetterMeTokens.CardRadius.Pill))
                .background(BetterMeColors.Primary.Primary)
                .padding(horizontal = 10.dp, vertical = 4.dp)
        ) {
            Text(
                text = "⏰ ${opt.suggestedTime}",
                style = BetterMeTypography.Body.Small.Medium,
                color = Color.White,
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
    val isSuccess = appliedCount != null
    val label = when {
        isApplying -> "Đang áp dụng…"
        isSuccess -> "🎉  Đã áp dụng $appliedCount đề xuất"
        else -> "Áp dụng đề xuất ngay"
    }
    val backgroundColor = if (isSuccess) Color(0xFF16A34A) else accent
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .clip(RoundedCornerShape(BetterMeTokens.CardRadius.Pill))
            .background(backgroundColor)
            .clickable(enabled = !isApplying && !isSuccess) { onApply() }
            .padding(vertical = 12.dp)
            .semantics {
                contentDescription = if (isSuccess) "Đã áp dụng đề xuất"
                else "Áp dụng các đề xuất lịch trình"
            },
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

private fun decorationFor(type: ConflictType): Pair<String, Color> = when (type) {
    ConflictType.OVERLAP -> "🔀" to Color(0xFFEA580C)
    ConflictType.OVERLOAD -> "🚦" to Color(0xFFDB7B0A)
    ConflictType.POOR_SLEEP -> "🌙" to Color(0xFF6366F1)
    ConflictType.LATE_NIGHT -> "🌒" to Color(0xFF6366F1)
    ConflictType.TRANSITION -> "↔" to Color(0xFFEA580C)
}
