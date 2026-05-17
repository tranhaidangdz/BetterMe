package com.example.betterme.presentation.addhabit.aicreation

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.betterme.domain.ai.habitcreation.CreationRiskLevel
import com.example.betterme.domain.ai.habitcreation.CreationSuggestionType
import com.example.betterme.domain.ai.habitcreation.HabitCreationAnalysis
import com.example.betterme.domain.ai.habitcreation.HabitCreationSuggestion
import com.example.betterme.domain.ai.habitcreation.HabitCreationWarning
import com.example.betterme.domain.ai.habitcreation.WarningType
import com.example.betterme.presentation.theme.BetterMeColors
import com.example.betterme.presentation.theme.BetterMeTokens
import com.example.betterme.presentation.theme.BetterMeTypography
import kotlinx.coroutines.delay

/**
 * Pre-save advisory bottom sheet that surfaces the AI Habit Creation
 * Assistant's analysis. Always offers "Vẫn tạo" as an escape hatch — the
 * assistant never blocks habit creation.
 *
 * Layout when [HabitCreationAssistantUi.Success]:
 *  - Header with sparkle disc + "Trợ lý tạo thói quen"
 *  - Risk badge (color-coded LOW/MODERATE/HIGH)
 *  - Encouragement line in a soft green card
 *  - Warnings list (max 3, type-tinted)
 *  - Suggestions list (max 3, type-tinted)
 *  - Two buttons at the bottom: "Áp dụng gợi ý" (close, advisory) + "Vẫn tạo"
 *  - Offline chip if isCanned
 *
 * Loading shows a rotating Vietnamese message via a lifecycle-safe
 * LaunchedEffect; Error shows a friendly line with a "Vẫn tạo" escape.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HabitCreationAssistantBottomSheet(
    state: HabitCreationAssistantState,
    onIntent: (HabitCreationAssistantIntent) -> Unit
) {
    if (state.ui is HabitCreationAssistantUi.Idle) return

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = { onIntent(HabitCreationAssistantIntent.Dismiss) },
        sheetState = sheetState,
        containerColor = Color.White,
        dragHandle = null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 18.dp)
                .heightIn(min = 220.dp)
        ) {
            Header(onClose = { onIntent(HabitCreationAssistantIntent.Dismiss) })
            Spacer(Modifier.height(14.dp))

            when (val ui = state.ui) {
                HabitCreationAssistantUi.Idle -> Unit
                HabitCreationAssistantUi.Loading -> LoadingBody()
                is HabitCreationAssistantUi.Success -> SuccessBody(
                    analysis = ui.analysis,
                    onConfirm = { onIntent(HabitCreationAssistantIntent.ConfirmSave) },
                    onApplyAll = {
                        onIntent(
                            HabitCreationAssistantIntent.ApplySuggestions(
                                ui.analysis.suggestions.filter { it.hasApplicableMutation }
                            )
                        )
                    },
                    onApplyOne = { s ->
                        onIntent(HabitCreationAssistantIntent.ApplySuggestions(listOf(s)))
                    }
                )
                is HabitCreationAssistantUi.Error -> ErrorBody(
                    message = ui.message,
                    onConfirm = { onIntent(HabitCreationAssistantIntent.ConfirmSave) }
                )
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun Header(onClose: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(BetterMeColors.Primary.Primary.copy(alpha = BetterMeTokens.AccentAlpha.Soft)),
            contentAlignment = Alignment.Center
        ) {
            Text(text = "✨", fontSize = 20.sp)
        }
        Spacer(Modifier.size(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Trợ lý tạo thói quen",
                style = BetterMeTypography.Title.Medium.Bold,
                color = BetterMeColors.Text.TextPrimary,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Vài lời khuyên nhẹ trước khi bạn lưu",
                style = BetterMeTypography.Body.Small.Medium,
                color = BetterMeColors.Text.TextTertiary
            )
        }
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .clickable { onClose() }
                .semantics { contentDescription = "Đóng trợ lý" },
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(BetterMeColors.Gray.Gray3),
                contentAlignment = Alignment.Center
            ) {
                Text(text = "✕", color = BetterMeColors.Text.TextTertiary, fontSize = 18.sp)
            }
        }
    }
}

@Composable
private fun LoadingBody() {
    val messages = remember {
        listOf(
            "Đang đối chiếu với thói quen hiện có…",
            "Soát giờ giấc và khối lượng…",
            "Tìm cách giúp bạn duy trì bền vững nhất…"
        )
    }
    var index by remember { mutableStateOf(0) }
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
            modifier = Modifier.size(26.dp),
            color = BetterMeColors.Primary.Primary,
            strokeWidth = 2.6.dp
        )
        AnimatedContent(
            targetState = messages[index],
            transitionSpec = { fadeIn(tween(280)) togetherWith fadeOut(tween(180)) },
            label = "habit_creation_loading_rotator"
        ) { msg ->
            Text(
                text = msg,
                style = BetterMeTypography.Body.Medium,
                color = BetterMeColors.Text.TextSecondary
            )
        }
    }
}

@Composable
private fun ErrorBody(message: String, onConfirm: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = message,
            style = BetterMeTypography.Body.Medium,
            color = BetterMeColors.Text.TextSecondary
        )
        // Error path must still let the user proceed — the assistant is
        // advisory and never blocks creation.
        PrimaryButton(label = "Vẫn tạo", onClick = onConfirm)
    }
}

@Composable
private fun SuccessBody(
    analysis: HabitCreationAnalysis,
    onConfirm: () -> Unit,
    onApplyAll: () -> Unit,
    onApplyOne: (HabitCreationSuggestion) -> Unit
) {
    Column {
        // ─── Risk badge ──────────────────────────────────────────
        RiskBadge(analysis.overallRisk)

        // ─── Encouragement card ─────────────────────────────────
        if (analysis.encouragement.isNotBlank()) {
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
                    text = analysis.encouragement,
                    style = BetterMeTypography.Body.Medium,
                    color = BetterMeColors.Text.TextPrimary
                )
            }
        }

        // ─── Warnings ───────────────────────────────────────────
        if (analysis.warnings.isNotEmpty()) {
            Spacer(Modifier.height(16.dp))
            SectionLabel("Lưu ý", analysis.warnings.size)
            Spacer(Modifier.height(8.dp))
            analysis.warnings.forEach { w ->
                WarningCard(w)
                Spacer(Modifier.height(8.dp))
            }
        }

        // ─── Suggestions ────────────────────────────────────────
        val applicableSuggestions = analysis.suggestions.filter { it.hasApplicableMutation }
        if (analysis.suggestions.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            SectionLabel("Gợi ý điều chỉnh", analysis.suggestions.size)
            Spacer(Modifier.height(8.dp))
            analysis.suggestions.forEach { s ->
                SuggestionCard(
                    suggestion = s,
                    onApply = if (s.hasApplicableMutation) {
                        { onApplyOne(s) }
                    } else null
                )
                Spacer(Modifier.height(8.dp))
            }
        }

        // ─── Buttons ────────────────────────────────────────────
        // The global "Áp dụng gợi ý" button is meaningful only when at
        // least one suggestion carries structured fields the form can
        // accept. Otherwise it would silently no-op, so we hide it and
        // let the user proceed via "Vẫn tạo" alone.
        Spacer(Modifier.height(14.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            if (applicableSuggestions.isNotEmpty()) {
                SecondaryButton(
                    label = if (applicableSuggestions.size > 1) "Áp dụng tất cả" else "Áp dụng gợi ý",
                    onClick = onApplyAll,
                    modifier = Modifier.weight(1f)
                )
            }
            PrimaryButton(
                label = "Vẫn tạo",
                onClick = onConfirm,
                modifier = Modifier.weight(1f)
            )
        }

        if (analysis.isCanned) {
            Spacer(Modifier.height(10.dp))
            OfflineChip()
        }
    }
}

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
private fun RiskBadge(level: CreationRiskLevel) {
    val (label, color) = when (level) {
        CreationRiskLevel.LOW -> "Mức an toàn" to Color(0xFF16A34A)
        CreationRiskLevel.MODERATE -> "Cần lưu ý nhẹ" to Color(0xFFEA580C)
        CreationRiskLevel.HIGH -> "Có rủi ro cao" to Color(0xFFDC2626)
    }
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(BetterMeTokens.CardRadius.Pill))
            .background(color.copy(alpha = 0.12f))
            .padding(horizontal = 12.dp, vertical = 4.dp)
            .semantics { contentDescription = "Mức rủi ro: $label" }
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
private fun WarningCard(w: HabitCreationWarning) {
    val accent = warningAccent(w.type)
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
            .padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(accent.copy(alpha = BetterMeTokens.AccentAlpha.Soft)),
            contentAlignment = Alignment.Center
        ) {
            Text(text = warningLabelEmoji(w.type), fontSize = 16.sp)
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = warningLabel(w.type),
                style = BetterMeTypography.Body.Small.Medium,
                color = accent,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = w.message,
                style = BetterMeTypography.Body.Medium,
                color = BetterMeColors.Text.TextPrimary
            )
        }
    }
}

@Composable
private fun SuggestionCard(
    suggestion: HabitCreationSuggestion,
    onApply: (() -> Unit)? = null
) {
    val accent = BetterMeColors.Primary.Primary
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(BetterMeTokens.CardRadius.Body))
            .background(accent.copy(alpha = BetterMeTokens.AccentAlpha.Subtle))
            .border(
                width = 1.dp,
                color = accent.copy(alpha = BetterMeTokens.AccentAlpha.Soft),
                shape = RoundedCornerShape(BetterMeTokens.CardRadius.Body)
            )
            .padding(12.dp)
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(accent.copy(alpha = BetterMeTokens.AccentAlpha.Soft)),
                contentAlignment = Alignment.Center
            ) {
                Text(text = suggestionLabelEmoji(suggestion.type), fontSize = 16.sp)
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = suggestionLabel(suggestion.type),
                    style = BetterMeTypography.Body.Small.Medium,
                    color = accent,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = suggestion.message,
                    style = BetterMeTypography.Body.Medium,
                    color = BetterMeColors.Text.TextPrimary
                )
                // Render a structured-value preview if any field is set —
                // gives the user a clear sense of what tapping "Áp dụng"
                // will actually change on the form.
                val hint = suggestionMutationHint(suggestion)
                if (hint.isNotBlank()) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "💡 $hint",
                        style = BetterMeTypography.Body.Small.Medium,
                        color = accent,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
        // Per-card "Áp dụng" pill — only shown when the suggestion has at
        // least one field the form can actually accept.
        if (onApply != null) {
            Spacer(Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .align(Alignment.End)
                    .clip(RoundedCornerShape(BetterMeTokens.CardRadius.Pill))
                    .background(accent.copy(alpha = BetterMeTokens.AccentAlpha.Soft))
                    .border(
                        width = 1.dp,
                        color = accent.copy(alpha = BetterMeTokens.AccentAlpha.Medium),
                        shape = RoundedCornerShape(BetterMeTokens.CardRadius.Pill)
                    )
                    .clickable { onApply() }
                    .padding(horizontal = 14.dp, vertical = 8.dp)
                    .semantics { contentDescription = "Áp dụng gợi ý" }
            ) {
                Text(
                    text = "✓  Áp dụng",
                    style = BetterMeTypography.Body.Small.Medium,
                    color = accent,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

/**
 * Builds a short Vietnamese preview of the structured fields on a
 * suggestion — used as the 💡 hint line under the message. Returns
 * blank when no field would mutate the form.
 */
private fun suggestionMutationHint(s: HabitCreationSuggestion): String {
    val parts = mutableListOf<String>()
    s.suggestedReminderTime?.takeIf { it.isNotBlank() }?.let { parts += "giờ → $it" }
    s.suggestedCategory?.takeIf { it.isNotBlank() }?.let { parts += "nhóm → $it" }
    s.suggestedTitle?.takeIf { it.isNotBlank() }?.let { parts += "tên → $it" }
    s.suggestedDifficulty?.takeIf { it.isNotBlank() }?.let { parts += "mức độ → $it" }
    s.suggestedDurationMinutes?.let { parts += "thời lượng → $it phút" }
    s.suggestedFrequency?.takeIf { it.isNotBlank() }?.let { parts += "tần suất → $it" }
    s.suggestedReplacementHabit?.takeIf { it.isNotBlank() }?.let { parts += "thay cho \"$it\"" }
    return parts.joinToString(" · ")
}

@Composable
private fun PrimaryButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .clip(RoundedCornerShape(BetterMeTokens.CardRadius.Pill))
            .background(BetterMeColors.Primary.Primary)
            .clickable { onClick() }
            .padding(vertical = 12.dp)
            .semantics { contentDescription = label },
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
private fun SecondaryButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val accent = BetterMeColors.Primary.Primary
    Box(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .clip(RoundedCornerShape(BetterMeTokens.CardRadius.Pill))
            .background(accent.copy(alpha = BetterMeTokens.AccentAlpha.Soft))
            .border(
                width = 1.dp,
                color = accent.copy(alpha = BetterMeTokens.AccentAlpha.Medium),
                shape = RoundedCornerShape(BetterMeTokens.CardRadius.Pill)
            )
            .clickable { onClick() }
            .padding(vertical = 12.dp)
            .semantics { contentDescription = label },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = BetterMeTypography.Body.Medium,
            color = accent,
            fontWeight = FontWeight.SemiBold
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
            text = "Bản phân tích nhanh (ngoại tuyến)",
            style = BetterMeTypography.Body.Small.Medium,
            color = BetterMeColors.Text.TextTertiary
        )
    }
}

private fun warningAccent(type: WarningType): Color = when (type) {
    WarningType.SLEEP_CONFLICT, WarningType.OVERLOAD_RISK -> Color(0xFFDC2626)
    WarningType.TIME_CONFLICT, WarningType.TOO_INTENSE,
    WarningType.TOO_FREQUENT, WarningType.TOO_MANY_HABITS -> Color(0xFFEA580C)
    WarningType.DUPLICATE_INTENT, WarningType.REDUNDANT_CATEGORY -> Color(0xFFDB7B0A)
}

private fun warningLabel(type: WarningType): String = when (type) {
    WarningType.TIME_CONFLICT -> "Giờ trùng nhau"
    WarningType.DUPLICATE_INTENT -> "Có thể trùng ý định"
    WarningType.OVERLOAD_RISK -> "Quá tải"
    WarningType.SLEEP_CONFLICT -> "Ảnh hưởng giấc ngủ"
    WarningType.TOO_MANY_HABITS -> "Quá nhiều thói quen"
    WarningType.REDUNDANT_CATEGORY -> "Chồng chéo nhóm"
    WarningType.TOO_INTENSE -> "Khá nặng"
    WarningType.TOO_FREQUENT -> "Tần suất cao"
}

private fun warningLabelEmoji(type: WarningType): String = when (type) {
    WarningType.TIME_CONFLICT -> "⏰"
    WarningType.DUPLICATE_INTENT -> "🔁"
    WarningType.OVERLOAD_RISK -> "🚦"
    WarningType.SLEEP_CONFLICT -> "🌙"
    WarningType.TOO_MANY_HABITS -> "📚"
    WarningType.REDUNDANT_CATEGORY -> "📂"
    WarningType.TOO_INTENSE -> "⚡"
    WarningType.TOO_FREQUENT -> "📅"
}

private fun suggestionLabel(type: CreationSuggestionType): String = when (type) {
    CreationSuggestionType.MERGE_EXISTING -> "Gộp với thói quen sẵn có"
    CreationSuggestionType.REPLACE_EXISTING -> "Nâng cấp thói quen hiện tại"
    CreationSuggestionType.REDUCE_INTENSITY -> "Giảm cường độ"
    CreationSuggestionType.REDUCE_FREQUENCY -> "Giảm tần suất"
    CreationSuggestionType.CHANGE_TIME -> "Dời giờ"
    CreationSuggestionType.START_SMALLER -> "Bắt đầu nhỏ hơn"
    CreationSuggestionType.TRY_ALTERNATIVE -> "Thử lựa chọn khác"
}

private fun suggestionLabelEmoji(type: CreationSuggestionType): String = when (type) {
    CreationSuggestionType.MERGE_EXISTING -> "🔗"
    CreationSuggestionType.REPLACE_EXISTING -> "♻"
    CreationSuggestionType.REDUCE_INTENSITY -> "🌿"
    CreationSuggestionType.REDUCE_FREQUENCY -> "📉"
    CreationSuggestionType.CHANGE_TIME -> "🕒"
    CreationSuggestionType.START_SMALLER -> "🌱"
    CreationSuggestionType.TRY_ALTERNATIVE -> "💡"
}
