package com.example.betterme.presentation.onboarding.ai

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
import com.example.betterme.domain.ai.onboarding.Difficulty
import com.example.betterme.domain.ai.onboarding.OnboardingSuggestedHabit
import com.example.betterme.presentation.theme.BetterMeColors
import com.example.betterme.presentation.theme.BetterMeTokens
import com.example.betterme.presentation.theme.BetterMeTypography
import kotlinx.coroutines.delay

/**
 * Bottom sheet that presents the AI onboarding suggestions. Mirrors the
 * visual language of the Schedule Analyzer sheet so users see one consistent
 * "AI assistant" surface across BetterMe.
 *
 * Sections:
 *   - Header (title + close)
 *   - Summary card (energy pill + recommendedFocus)
 *   - Habit list (per-row Apply with "Đã thêm" success state)
 *   - "Áp dụng tất cả" CTA with rewarding success transition
 *   - "Tạo lại" pill + offline hint if applicable
 *
 * Loading shows a rotating Vietnamese message every ~1.8s via a single
 * LaunchedEffect scoped to the Loading branch — dies with the composition.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnboardingAiBottomSheet(
    state: OnboardingAiState,
    onIntent: (OnboardingAiIntent) -> Unit
) {
    if (state.ui is OnboardingAiUi.Idle) return

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = { onIntent(OnboardingAiIntent.Dismiss) },
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
            Header(onClose = { onIntent(OnboardingAiIntent.Dismiss) })
            Spacer(Modifier.height(14.dp))

            when (val ui = state.ui) {
                OnboardingAiUi.Idle -> Unit
                OnboardingAiUi.Loading -> LoadingBody()
                is OnboardingAiUi.Success -> SuccessBody(
                    ui = ui,
                    isApplyingBulk = state.isApplyingBulk,
                    onAccept = { onIntent(OnboardingAiIntent.Accept(it)) },
                    onAcceptAll = { onIntent(OnboardingAiIntent.AcceptAll) },
                    onClearToast = { onIntent(OnboardingAiIntent.ClearBulkAppliedToast) }
                )
                is OnboardingAiUi.Error -> ErrorBody(ui.message)
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
                text = "Gợi ý thói quen khởi đầu",
                style = BetterMeTypography.Title.Medium.Bold,
                color = BetterMeColors.Text.TextPrimary,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "AI sẽ chọn 4–6 thói quen nhẹ nhàng, bền vững cho bạn",
                style = BetterMeTypography.Body.Small.Medium,
                color = BetterMeColors.Text.TextTertiary
            )
        }
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .clickable { onClose() }
                .semantics { contentDescription = "Đóng gợi ý" },
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
            "Đang phân tích nhịp sống của bạn…",
            "Chọn các thói quen phù hợp với mục tiêu…",
            "Cân bằng giữa năng lượng và phục hồi…",
            "Sắp đến rồi…"
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
            modifier = Modifier.size(28.dp),
            color = BetterMeColors.Primary.Primary,
            strokeWidth = 2.6.dp
        )
        AnimatedContent(
            targetState = messages[index],
            transitionSpec = { fadeIn(tween(280)) togetherWith fadeOut(tween(180)) },
            label = "onboarding_loading_rotator"
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
private fun ErrorBody(message: String) {
    Text(
        text = "⚠️  $message",
        style = BetterMeTypography.Body.Medium,
        color = BetterMeColors.Red,
        modifier = Modifier.padding(vertical = 16.dp)
    )
}

@Composable
private fun SuccessBody(
    ui: OnboardingAiUi.Success,
    isApplyingBulk: Boolean,
    onAccept: (OnboardingSuggestedHabit) -> Unit,
    onAcceptAll: () -> Unit,
    onClearToast: () -> Unit
) {
    LaunchedEffect(ui.bulkAppliedCount) {
        if (ui.bulkAppliedCount != null) {
            delay(2400)
            onClearToast()
        }
    }

    val accent = BetterMeColors.Primary.Primary
    val totalCount = ui.suggestion.habits.size
    val acceptedCount = ui.acceptedTitles.size
    val remainingCount = totalCount - acceptedCount

    Column {
        // ─── Summary card ────────────────────────────────────────
        if (ui.suggestion.summary.isNotBlank() || ui.suggestion.recommendedFocus.isNotBlank()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(BetterMeTokens.CardRadius.Hero))
                    .background(accent.copy(alpha = BetterMeTokens.AccentAlpha.Soft))
                    .border(
                        width = 1.dp,
                        color = accent.copy(alpha = BetterMeTokens.AccentAlpha.Medium),
                        shape = RoundedCornerShape(BetterMeTokens.CardRadius.Hero)
                    )
                    .padding(14.dp)
            ) {
                Column {
                    if (ui.suggestion.summary.isNotBlank()) {
                        Text(
                            text = "✨ ${ui.suggestion.summary}",
                            style = BetterMeTypography.Body.Medium,
                            color = BetterMeColors.Text.TextPrimary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    if (ui.suggestion.recommendedFocus.isNotBlank()) {
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = ui.suggestion.recommendedFocus,
                            style = BetterMeTypography.Body.Small.Medium,
                            color = BetterMeColors.Text.TextSecondary
                        )
                    }
                }
            }
            Spacer(Modifier.height(14.dp))
        }

        // ─── Habit list ──────────────────────────────────────────
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "Thói quen đề xuất",
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
                    text = "$totalCount",
                    style = BetterMeTypography.Body.Small.Medium,
                    color = BetterMeColors.Text.TextSecondary,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
        Spacer(Modifier.height(10.dp))

        ui.suggestion.habits.forEach { habit ->
            HabitSuggestionRow(
                habit = habit,
                accepted = habit.title in ui.acceptedTitles,
                onAccept = { onAccept(habit) }
            )
            Spacer(Modifier.height(8.dp))
        }

        // ─── Apply All CTA ───────────────────────────────────────
        if (remainingCount > 0 || ui.bulkAppliedCount != null) {
            Spacer(Modifier.height(8.dp))
            ApplyAllButton(
                remaining = remainingCount,
                isApplying = isApplyingBulk,
                bulkAppliedCount = ui.bulkAppliedCount,
                onApply = onAcceptAll
            )
        } else if (acceptedCount == totalCount && totalCount > 0) {
            Spacer(Modifier.height(8.dp))
            AllAcceptedBanner()
        }

    }
}

@Composable
private fun HabitSuggestionRow(
    habit: OnboardingSuggestedHabit,
    accepted: Boolean,
    onAccept: () -> Unit
) {
    val (badgeColor, badgeLabel) = when (habit.difficulty) {
        Difficulty.EASY -> Color(0xFF16A34A) to "Dễ"
        Difficulty.MEDIUM -> Color(0xFFEA580C) to "Trung bình"
        Difficulty.HARD -> Color(0xFFDC2626) to "Khó"
    }
    val accent = BetterMeColors.Primary.Primary
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(BetterMeTokens.CardRadius.Body))
            .background(
                if (accepted) Color(0xFF16A34A).copy(alpha = 0.06f)
                else BetterMeColors.BackGround.BackgroundSecondary
            )
            .border(
                width = 1.dp,
                color = if (accepted) Color(0xFF16A34A).copy(alpha = 0.32f)
                else accent.copy(alpha = BetterMeTokens.AccentAlpha.Subtle),
                shape = RoundedCornerShape(BetterMeTokens.CardRadius.Body)
            )
            .padding(14.dp),
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
            Text(text = habit.emoji.ifBlank { "✨" }, fontSize = 20.sp)
        }
        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = habit.title,
                    style = BetterMeTypography.Body.Medium.copy(fontWeight = FontWeight.SemiBold),
                    color = BetterMeColors.Text.TextPrimary,
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
            if (habit.description.isNotBlank()) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = habit.description,
                    style = BetterMeTypography.Body.Small.Medium,
                    color = BetterMeColors.Text.TextSecondary
                )
            }
            Spacer(Modifier.height(6.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                MetaChip(text = "⏰ ${habit.reminderTime}", color = accent)
                MetaChip(text = "⏱ ${habit.estimatedMinutes} phút", color = accent)
            }
            if (habit.motivation.isNotBlank()) {
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "💡 ${habit.motivation}",
                    style = BetterMeTypography.Body.Small.Medium,
                    color = accent,
                    fontWeight = FontWeight.Medium
                )
            }
            Spacer(Modifier.height(10.dp))
            if (accepted) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(BetterMeTokens.CardRadius.Pill))
                        .background(Color(0xFF16A34A).copy(alpha = 0.14f))
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = "✓  Đã thêm",
                        style = BetterMeTypography.Body.Small.Medium,
                        color = Color(0xFF16A34A),
                        fontWeight = FontWeight.SemiBold
                    )
                }
            } else {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(BetterMeTokens.CardRadius.Pill))
                        .background(accent)
                        .clickable { onAccept() }
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                        .semantics {
                            contentDescription = "Thêm thói quen ${habit.title}"
                        }
                ) {
                    Text(
                        text = "+ Thêm",
                        style = BetterMeTypography.Body.Small.Medium,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun MetaChip(text: String, color: Color) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(BetterMeTokens.CardRadius.Pill))
            .background(color.copy(alpha = BetterMeTokens.AccentAlpha.Soft))
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(
            text = text,
            style = BetterMeTypography.Body.Small.Medium,
            color = color,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun ApplyAllButton(
    remaining: Int,
    isApplying: Boolean,
    bulkAppliedCount: Int?,
    onApply: () -> Unit
) {
    val accent = BetterMeColors.Primary.Primary
    val isSuccess = bulkAppliedCount != null
    val label = when {
        isApplying -> "Đang áp dụng…"
        isSuccess -> "🎉  Đã thêm $bulkAppliedCount thói quen"
        else -> "Áp dụng tất cả ($remaining)"
    }
    val background = if (isSuccess) Color(0xFF16A34A) else accent
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .clip(RoundedCornerShape(BetterMeTokens.CardRadius.Pill))
            .background(background)
            .clickable(enabled = !isApplying && !isSuccess) { onApply() }
            .padding(vertical = 12.dp)
            .semantics {
                contentDescription = if (isSuccess) "Đã áp dụng tất cả"
                else "Áp dụng tất cả thói quen được đề xuất"
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
private fun AllAcceptedBanner() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(BetterMeTokens.CardRadius.Pill))
            .background(Color(0xFF16A34A).copy(alpha = 0.12f))
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "🎉  Tất cả thói quen đã được thêm",
            style = BetterMeTypography.Body.Medium,
            color = Color(0xFF16A34A),
            fontWeight = FontWeight.SemiBold
        )
    }
}

