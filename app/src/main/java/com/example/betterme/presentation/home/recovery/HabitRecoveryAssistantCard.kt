package com.example.betterme.presentation.home.recovery

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.betterme.domain.ai.recovery.HabitRecoveryAction
import com.example.betterme.domain.ai.recovery.HabitRecoveryAnalysis
import com.example.betterme.domain.ai.recovery.RecoveryActionType
import com.example.betterme.domain.ai.recovery.RecoveryIntensity
import com.example.betterme.domain.ai.recovery.RecoveryTrigger
import com.example.betterme.domain.ai.recovery.StrugglingHabit
import com.example.betterme.presentation.theme.BetterMeColors
import com.example.betterme.presentation.theme.BetterMeTokens
import com.example.betterme.presentation.theme.BetterMeTypography
import kotlinx.coroutines.delay

/**
 * Inline Adaptive Habit Recovery card mounted at the top of Home when the
 * use case detects struggle signals. Hidden completely when the user is
 * healthy — no empty shell, no "everything looks fine" filler.
 *
 * Layout when [HabitRecoveryUi.Success]:
 *   - Header: tone-emoji disc + "BetterMe đề xuất phục hồi" + tone pill
 *   - Coaching message (1-2 sentences from the AI)
 *   - Trigger chips (low completion / miss streak / overload / late-night)
 *   - "Thói quen đang gặp khó" block listing struggling habits with
 *     7d/14d % + miss streak
 *   - Recovery actions list with per-action CTA + spinner + applied state
 *   - Footer: "Phân tích lại" pill + "Ẩn" link + offline chip if isCanned
 */
@Composable
fun HabitRecoveryAssistantCard(
    state: HabitRecoveryState,
    onRefresh: () -> Unit,
    onDismiss: () -> Unit,
    onApply: (Int, HabitRecoveryAction) -> Unit
) {
    when (val ui = state.ui) {
        HabitRecoveryUi.Idle, HabitRecoveryUi.Hidden -> Unit
        HabitRecoveryUi.Loading -> LoadingCard()
        is HabitRecoveryUi.Success -> SuccessCard(
            ui = ui,
            onRefresh = onRefresh,
            onDismiss = onDismiss,
            onApply = onApply
        )
        is HabitRecoveryUi.Error -> ErrorCard(ui.message, onRefresh)
    }
}

@Composable
private fun CardSurface(toneAccent: Color, content: @Composable () -> Unit) {
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
                    colors = listOf(Color.White, toneAccent.copy(alpha = 0.05f))
                )
            )
            .border(
                width = 1.dp,
                color = toneAccent.copy(alpha = BetterMeTokens.AccentAlpha.Medium),
                shape = RoundedCornerShape(BetterMeTokens.CardRadius.Hero)
            )
            .padding(18.dp)
    ) { content() }
}

@Composable
private fun LoadingCard() {
    val messages = remember {
        listOf(
            "Đang xem lại chuỗi của bạn…",
            "Soát các thói quen đang đuối…",
            "Tìm cách điều chỉnh nhẹ nhàng hơn…"
        )
    }
    var index by remember { mutableStateOf(0) }
    LaunchedEffect(messages) {
        while (true) {
            delay(1800)
            index = (index + 1) % messages.size
        }
    }
    CardSurface(toneAccent = BetterMeColors.Primary.Primary) {
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
                label = "recovery_loading_rotator"
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
    CardSurface(toneAccent = BetterMeColors.Red) {
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
private fun SuccessCard(
    ui: HabitRecoveryUi.Success,
    onRefresh: () -> Unit,
    onDismiss: () -> Unit,
    onApply: (Int, HabitRecoveryAction) -> Unit
) {
    val analysis = ui.analysis
    val tone = toneFor(analysis.overallTone)
    CardSurface(toneAccent = tone.color) {
        Column {
            // ─── Header ──────────────────────────────────────────
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(tone.color.copy(alpha = BetterMeTokens.AccentAlpha.Soft))
                        .border(
                            width = 1.dp,
                            color = tone.color.copy(alpha = BetterMeTokens.AccentAlpha.Medium),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = tone.emoji, fontSize = 20.sp)
                }
                Spacer(Modifier.size(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "BetterMe đề xuất phục hồi",
                        style = BetterMeTypography.Title.Small.Bold,
                        color = BetterMeColors.Text.TextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Dựa trên 14 ngày gần nhất",
                        style = BetterMeTypography.Body.Small.Medium,
                        color = BetterMeColors.Text.TextTertiary
                    )
                }
                TonePill(tone)
            }

            // ─── Coaching message ────────────────────────────────
            if (analysis.coachingMessage.isNotBlank()) {
                Spacer(Modifier.height(14.dp))
                Text(
                    text = "💬  ${analysis.coachingMessage}",
                    style = BetterMeTypography.Body.Medium.copy(fontWeight = FontWeight.SemiBold),
                    color = BetterMeColors.Text.TextPrimary
                )
            }

            // ─── Trigger chips ───────────────────────────────────
            if (analysis.triggerReasons.isNotEmpty()) {
                Spacer(Modifier.height(12.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Render up to 3 chips so the row doesn't wrap weirdly
                    analysis.triggerReasons.take(3).forEach { TriggerChip(it) }
                }
            }

            // ─── Struggling habits ───────────────────────────────
            if (analysis.struggling.isNotEmpty()) {
                Spacer(Modifier.height(14.dp))
                Text(
                    text = "Thói quen đang gặp khó",
                    style = BetterMeTypography.Title.Small.Bold,
                    color = BetterMeColors.Text.TextPrimary,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(8.dp))
                analysis.struggling.forEach { s ->
                    StrugglingRow(s)
                    Spacer(Modifier.height(6.dp))
                }
            }

            // ─── Recovery actions ────────────────────────────────
            if (analysis.recoveryActions.isNotEmpty()) {
                Spacer(Modifier.height(14.dp))
                Text(
                    text = "Hành động đề xuất",
                    style = BetterMeTypography.Title.Small.Bold,
                    color = BetterMeColors.Text.TextPrimary,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(8.dp))
                analysis.recoveryActions.forEachIndexed { idx, action ->
                    ActionRow(
                        action = action,
                        applying = ui.applyingIndex == idx,
                        applied = idx in ui.appliedIndexes,
                        canApply = canMaterialize(action.type),
                        onApply = { onApply(idx, action) }
                    )
                    Spacer(Modifier.height(8.dp))
                }
            }

            // ─── Footer ──────────────────────────────────────────
            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Pill(
                    label = "↻  Phân tích lại",
                    color = tone.color,
                    background = tone.color.copy(alpha = BetterMeTokens.AccentAlpha.Soft),
                    onClick = onRefresh
                )
                Spacer(Modifier.size(8.dp))
                Pill(
                    label = "Ẩn",
                    color = BetterMeColors.Text.TextTertiary,
                    background = BetterMeColors.Gray.Gray3,
                    onClick = onDismiss
                )
                if (analysis.isCanned) {
                    Spacer(Modifier.size(8.dp))
                    OfflineChip()
                }
            }
        }
    }
}

@Composable
private fun TriggerChip(trigger: RecoveryTrigger) {
    val (label, color) = labelFor(trigger)
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(BetterMeTokens.CardRadius.Pill))
            .background(color.copy(alpha = 0.12f))
            .padding(horizontal = 10.dp, vertical = 4.dp)
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
private fun StrugglingRow(s: StrugglingHabit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(BetterMeTokens.CardRadius.Body))
            .background(BetterMeColors.BackGround.BackgroundSecondary)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = s.title,
                style = BetterMeTypography.Body.Medium.copy(fontWeight = FontWeight.SemiBold),
                color = BetterMeColors.Text.TextPrimary
            )
            if (s.recoveryReason.isNotBlank()) {
                Spacer(Modifier.height(2.dp))
                Text(
                    text = s.recoveryReason,
                    style = BetterMeTypography.Body.Small.Medium,
                    color = BetterMeColors.Text.TextTertiary
                )
            }
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = "${s.completionRate14d}%",
                style = BetterMeTypography.Body.Medium.copy(fontWeight = FontWeight.Bold),
                color = percentColor(s.completionRate14d)
            )
            if (s.missStreak > 0) {
                Text(
                    text = "Bỏ lỡ ${s.missStreak} ngày",
                    style = BetterMeTypography.Body.Small.Medium,
                    color = BetterMeColors.Red
                )
            }
        }
    }
}

@Composable
private fun ActionRow(
    action: HabitRecoveryAction,
    applying: Boolean,
    applied: Boolean,
    canApply: Boolean,
    onApply: () -> Unit
) {
    val (emoji, accent) = decorationFor(action.type)
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
                text = action.title,
                style = BetterMeTypography.Body.Medium.copy(fontWeight = FontWeight.SemiBold),
                color = BetterMeColors.Text.TextPrimary
            )
            if (action.description.isNotBlank()) {
                Spacer(Modifier.height(2.dp))
                Text(
                    text = action.description,
                    style = BetterMeTypography.Body.Small.Medium,
                    color = BetterMeColors.Text.TextTertiary
                )
            }
            if (action.suggestedValue.isNotBlank()) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "💡 ${action.suggestedValue}",
                    style = BetterMeTypography.Body.Small.Medium,
                    color = accent,
                    fontWeight = FontWeight.Medium
                )
            }
            Spacer(Modifier.height(8.dp))
            when {
                applied -> Pill(
                    label = "✓  Đã áp dụng",
                    color = Color(0xFF16A34A),
                    background = Color(0xFF16A34A).copy(alpha = 0.12f),
                    onClick = {}
                )
                applying -> Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(BetterMeTokens.CardRadius.Pill))
                        .background(accent.copy(alpha = 0.12f))
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        color = accent,
                        strokeWidth = 2.dp
                    )
                }
                canApply -> Pill(
                    label = ctaLabelFor(action.type),
                    color = accent,
                    background = accent.copy(alpha = BetterMeTokens.AccentAlpha.Soft),
                    onClick = onApply
                )
                else -> Text(
                    text = "Gợi ý tham khảo — bạn điều chỉnh trong Thói quen",
                    style = BetterMeTypography.Body.Small.Medium,
                    color = BetterMeColors.Text.TextTertiary
                )
            }
        }
    }
}

@Composable
private fun TonePill(tone: ToneSpec) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(BetterMeTokens.CardRadius.Pill))
            .background(tone.color.copy(alpha = 0.12f))
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(
            text = "${tone.emoji}  ${tone.label}",
            style = BetterMeTypography.Body.Small.Medium,
            color = tone.color,
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
            text = "📴 Bản đề xuất nhanh",
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
            .heightIn(min = 36.dp)
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 8.dp),
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

private data class ToneSpec(val emoji: String, val label: String, val color: Color)

private fun toneFor(intensity: RecoveryIntensity): ToneSpec = when (intensity) {
    RecoveryIntensity.LIGHT -> ToneSpec("🌿", "Nhẹ nhàng", Color(0xFF16A34A))
    RecoveryIntensity.MODERATE -> ToneSpec("⚖️", "Cần điều chỉnh", Color(0xFFEA580C))
    RecoveryIntensity.AGGRESSIVE -> ToneSpec("🛑", "Cần thay đổi lớn", Color(0xFFDC2626))
}

private fun labelFor(trigger: RecoveryTrigger): Pair<String, Color> = when (trigger) {
    RecoveryTrigger.LOW_COMPLETION -> "Tỉ lệ hoàn thành thấp" to Color(0xFFEA580C)
    RecoveryTrigger.SKIP_STREAK -> "Bỏ lỡ liên tục" to Color(0xFFEA580C)
    RecoveryTrigger.CONSECUTIVE_FAILS -> "Bỏ ≥ 4 ngày" to Color(0xFFDC2626)
    RecoveryTrigger.HARD_HABIT_FAILING -> "Thói quen khó đang đuối" to Color(0xFFDC2626)
    RecoveryTrigger.LATE_NIGHT_FAILURES -> "Khó duy trì buổi tối" to Color(0xFF6366F1)
    RecoveryTrigger.TOO_MANY_HABITS -> "Quá nhiều thói quen" to Color(0xFFEA580C)
    RecoveryTrigger.BURNOUT_RISK -> "Nguy cơ kiệt sức" to Color(0xFFDC2626)
}

private fun decorationFor(type: RecoveryActionType): Pair<String, Color> = when (type) {
    RecoveryActionType.REDUCE_DIFFICULTY -> "🌿" to Color(0xFF16A34A)
    RecoveryActionType.REDUCE_FREQUENCY -> "📅" to Color(0xFF16A34A)
    RecoveryActionType.REDUCE_DURATION -> "⏱️" to Color(0xFF16A34A)
    RecoveryActionType.SWITCH_ALTERNATIVE -> "🔄" to Color(0xFF6366F1)
    RecoveryActionType.SPLIT_HABIT -> "✂️" to Color(0xFF6366F1)
    RecoveryActionType.ADD_RECOVERY_HABIT -> "💆" to Color(0xFF6366F1)
    RecoveryActionType.PAUSE_TEMPORARILY -> "⏸️" to Color(0xFFEA580C)
    RecoveryActionType.CHANGE_TIME -> "🕒" to Color(0xFFEA580C)
}

private fun ctaLabelFor(type: RecoveryActionType): String = when (type) {
    RecoveryActionType.CHANGE_TIME -> "Áp dụng giờ mới"
    RecoveryActionType.PAUSE_TEMPORARILY -> "Tạm dừng"
    else -> "Áp dụng"
}

/**
 * Which action types the use case can actually mutate today. The others
 * are advisory and show a "you'll need to edit in Thói quen" line instead
 * of a misleading CTA that does nothing.
 */
private fun canMaterialize(type: RecoveryActionType): Boolean = when (type) {
    RecoveryActionType.CHANGE_TIME,
    RecoveryActionType.PAUSE_TEMPORARILY -> true
    else -> false
}

private fun percentColor(pct: Int): Color = when {
    pct >= 70 -> Color(0xFF16A34A)
    pct >= 50 -> Color(0xFFEA580C)
    else -> Color(0xFFDC2626)
}
