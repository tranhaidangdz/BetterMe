package com.example.betterme.presentation.home.progression

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
import com.example.betterme.domain.ai.progression.HabitProgressionAction
import com.example.betterme.domain.ai.progression.HabitProgressionAnalysis
import com.example.betterme.domain.ai.progression.ProgressionActionType
import com.example.betterme.domain.ai.progression.ProgressionPace
import com.example.betterme.domain.ai.progression.ProgressionTrigger
import com.example.betterme.domain.ai.progression.VibrantHabit
import com.example.betterme.presentation.theme.BetterMeColors
import com.example.betterme.presentation.theme.BetterMeTokens
import com.example.betterme.presentation.theme.BetterMeTypography
import kotlinx.coroutines.delay

/**
 * Inline Smart Habit Progression card mounted at the top of Home when
 * the use case's gates all pass. Hidden completely when the user isn't
 * thriving — no empty shell, no false praise.
 *
 * Visual differs from Recovery deliberately: a green/emerald palette
 * signaling growth instead of recovery's red/orange. The same surface
 * primitives (CardSurface, Pill, OfflineChip) are local copies to keep
 * the file self-contained.
 *
 * Actions render with a "Tham khảo" hint instead of an Apply CTA —
 * HabitEntity doesn't store duration/frequency today, so a real Apply
 * button would be misleading.
 */
@Composable
fun HabitProgressionAssistantCard(
    state: HabitProgressionState,
    onRefresh: () -> Unit,
    onDismiss: () -> Unit
) {
    when (val ui = state.ui) {
        HabitProgressionUi.Idle, HabitProgressionUi.Hidden -> Unit
        HabitProgressionUi.Loading -> LoadingCard()
        is HabitProgressionUi.Success -> SuccessCard(
            analysis = ui.analysis,
            onRefresh = onRefresh,
            onDismiss = onDismiss
        )
        is HabitProgressionUi.Error -> ErrorCard(ui.message, onRefresh)
    }
}

private val GrowthGreen = Color(0xFF16A34A)
private val GrowthBlue = Color(0xFF0EA5E9)

@Composable
private fun CardSurface(content: @Composable () -> Unit) {
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
                    colors = listOf(Color.White, GrowthGreen.copy(alpha = 0.05f))
                )
            )
            .border(
                width = 1.dp,
                color = GrowthGreen.copy(alpha = BetterMeTokens.AccentAlpha.Medium),
                shape = RoundedCornerShape(BetterMeTokens.CardRadius.Hero)
            )
            .padding(18.dp)
    ) { content() }
}

@Composable
private fun LoadingCard() {
    val messages = remember {
        listOf(
            "Đang xem nhịp 14 ngày của bạn…",
            "Tìm bước tiếp theo phù hợp…",
            "Cân nhắc cách nâng nhẹ độ thử thách…"
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
                color = GrowthGreen,
                strokeWidth = 2.4.dp
            )
            AnimatedContent(
                targetState = messages[index],
                transitionSpec = { fadeIn(tween(280)) togetherWith fadeOut(tween(180)) },
                label = "progression_loading_rotator"
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
private fun SuccessCard(
    analysis: HabitProgressionAnalysis,
    onRefresh: () -> Unit,
    onDismiss: () -> Unit
) {
    val pace = paceFor(analysis.overallPace)
    CardSurface {
        Column {
            // ─── Header ──────────────────────────────────────────
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(GrowthGreen.copy(alpha = BetterMeTokens.AccentAlpha.Soft))
                        .border(
                            width = 1.dp,
                            color = GrowthGreen.copy(alpha = BetterMeTokens.AccentAlpha.Medium),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "🚀", fontSize = 20.sp)
                }
                Spacer(Modifier.size(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Sẵn sàng tiến thêm một bước?",
                        style = BetterMeTypography.Title.Small.Bold,
                        color = BetterMeColors.Text.TextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Dựa trên 14 ngày qua",
                        style = BetterMeTypography.Body.Small.Medium,
                        color = BetterMeColors.Text.TextTertiary
                    )
                }
                PacePill(pace)
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
                    analysis.triggerReasons.take(3).forEach { TriggerChip(it) }
                }
            }

            // ─── Vibrant habits ──────────────────────────────────
            if (analysis.vibrant.isNotEmpty()) {
                Spacer(Modifier.height(14.dp))
                Text(
                    text = "Thói quen đang vững",
                    style = BetterMeTypography.Title.Small.Bold,
                    color = BetterMeColors.Text.TextPrimary,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(8.dp))
                analysis.vibrant.forEach { v ->
                    VibrantRow(v)
                    Spacer(Modifier.height(6.dp))
                }
            }

            // ─── Progression actions ─────────────────────────────
            if (analysis.progressionActions.isNotEmpty()) {
                Spacer(Modifier.height(14.dp))
                Text(
                    text = "Bước tiếp theo (mềm)",
                    style = BetterMeTypography.Title.Small.Bold,
                    color = BetterMeColors.Text.TextPrimary,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(8.dp))
                analysis.progressionActions.forEach { action ->
                    ActionRow(action)
                    Spacer(Modifier.height(8.dp))
                }
            }

            // ─── Footer ──────────────────────────────────────────
            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Pill(
                    label = "↻  Phân tích lại",
                    color = GrowthGreen,
                    background = GrowthGreen.copy(alpha = BetterMeTokens.AccentAlpha.Soft),
                    onClick = onRefresh
                )
                Spacer(Modifier.size(8.dp))
                Pill(
                    label = "Để sau",
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
private fun TriggerChip(trigger: ProgressionTrigger) {
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
private fun VibrantRow(v: VibrantHabit) {
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
                text = v.title,
                style = BetterMeTypography.Body.Medium.copy(fontWeight = FontWeight.SemiBold),
                color = BetterMeColors.Text.TextPrimary
            )
            if (v.readinessReason.isNotBlank()) {
                Spacer(Modifier.height(2.dp))
                Text(
                    text = v.readinessReason,
                    style = BetterMeTypography.Body.Small.Medium,
                    color = BetterMeColors.Text.TextTertiary
                )
            }
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = "${v.completionRate14d}%",
                style = BetterMeTypography.Body.Medium.copy(fontWeight = FontWeight.Bold),
                color = GrowthGreen
            )
            if (v.currentStreak > 0) {
                Text(
                    text = "${v.currentStreak} ngày ✓",
                    style = BetterMeTypography.Body.Small.Medium,
                    color = GrowthGreen
                )
            }
        }
    }
}

@Composable
private fun ActionRow(action: HabitProgressionAction) {
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
            // No Apply CTA — HabitEntity doesn't store duration/frequency
            // today. A real button would be misleading. Show a hint badge
            // directing the user to edit the habit themselves.
            Text(
                text = "Tham khảo — bạn điều chỉnh trong Thói quen",
                style = BetterMeTypography.Body.Small.Medium,
                color = BetterMeColors.Text.TextTertiary
            )
        }
    }
}

@Composable
private fun PacePill(pace: PaceSpec) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(BetterMeTokens.CardRadius.Pill))
            .background(pace.color.copy(alpha = 0.12f))
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(
            text = "${pace.emoji}  ${pace.label}",
            style = BetterMeTypography.Body.Small.Medium,
            color = pace.color,
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

private data class PaceSpec(val emoji: String, val label: String, val color: Color)

private fun paceFor(pace: ProgressionPace): PaceSpec = when (pace) {
    ProgressionPace.GENTLE -> PaceSpec("🌿", "Nhẹ nhàng", GrowthGreen)
    ProgressionPace.STEADY -> PaceSpec("📈", "Tiến đều", GrowthBlue)
}

private fun labelFor(trigger: ProgressionTrigger): Pair<String, Color> = when (trigger) {
    ProgressionTrigger.HIGH_COMPLETION -> "Nhất quán cao" to GrowthGreen
    ProgressionTrigger.NO_MISS_STREAK -> "Không bỏ lỡ" to GrowthGreen
    ProgressionTrigger.STABLE_STREAK -> "Chuỗi vững" to GrowthBlue
    ProgressionTrigger.HEADROOM_FOR_GROWTH -> "Còn dư địa" to GrowthBlue
    ProgressionTrigger.NO_RECOVERY_NEEDED -> "Phục hồi tốt" to GrowthGreen
}

private fun decorationFor(type: ProgressionActionType): Pair<String, Color> = when (type) {
    ProgressionActionType.INCREASE_DURATION -> "⏱️" to GrowthGreen
    ProgressionActionType.INCREASE_FREQUENCY -> "🔁" to GrowthGreen
    ProgressionActionType.LEVEL_UP_VARIATION -> "✨" to GrowthBlue
    ProgressionActionType.ADD_COMPLEMENTARY_HABIT -> "🌿" to GrowthBlue
    ProgressionActionType.CONSISTENCY_REWARD -> "🏅" to Color(0xFFCA8A04)
}
