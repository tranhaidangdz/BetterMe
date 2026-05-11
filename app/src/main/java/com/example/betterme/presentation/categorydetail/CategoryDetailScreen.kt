package com.example.betterme.presentation.categorydetail

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.betterme.R
import com.example.betterme.presentation.categorydetail.components.AiReviewCard
import com.example.betterme.presentation.categorydetail.components.CategoryActionButton
import com.example.betterme.presentation.categorydetail.components.CategorySummaryCard
import com.example.betterme.presentation.categorydetail.components.HabitDetailCard
import com.example.betterme.presentation.categorydetail.components.paletteFor
import com.example.betterme.presentation.components.view.BetterMeTopBar
import com.example.betterme.presentation.theme.BetterMeColors
import com.example.betterme.presentation.theme.BetterMeTypography

// =========================
// SCREEN — nhận state/callback từ MainScreen
// =========================
//
// Visual redesign only — the LazyColumn structure, item keys, ViewModel wiring and
// navigation flow are unchanged. What changed:
// - Every component receives the category's accent color from [paletteFor(categoryId)]
//   so the whole screen reads as one tinted identity (no more random palette per row).
// - Habit emoji per row falls back to the category icon if no per-habit emoji is
//   available, so empty/missing slots don't break the visual rhythm.
// - Empty state is now a centered illustration + motivational message instead of a
//   single tertiary-text line.
// - Section headers, top-bar tint, and content padding all align with the redesigned
//   Home screen's design system (18-22dp corner radius, accent-tinted shadows, soft
//   gradient surfaces).
@Composable
fun CategoryDetailScreen(
    state: CategoryDetailState,
    onBack: () -> Unit,
    onHabitClick: (Int) -> Unit = {},
    onAiReviewClick: () -> Unit,
    onAddHabitClick: () -> Unit,
    onAiSuggestClick: () -> Unit,
    onDismissAiReview: () -> Unit = {},
) {
    val palette = paletteFor(state.categoryId)
    // Fallback emoji set used when the habit doesn't expose its own. Kept here as a
    // last-resort visual — the category icon is the primary glyph.
    val habitEmojiFallback = listOf("🎯", "✨", "🌱", "⭐", "🔥")

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(BetterMeColors.BackGround.BackgroundSecondary)
            .statusBarsPadding()
            .imePadding()
            // Smooth out the list when habits load in / empty-state transitions.
            // Capped to a short tween so the change feels native, not animated for
            // animation's sake.
            .animateContentSize(animationSpec = tween(durationMillis = 220)),
        verticalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        item(key = "topbar") {
            BetterMeTopBar(
                leadingIconRes = R.drawable.ic_arrow_left,
                title = state.categoryName.ifBlank { "Nhóm thói quen" },
                onLeadingClick = onBack
            )
        }

        // ===== HERO SUMMARY =====
        item(key = "summary") {
            Spacer(Modifier.height(8.dp))
            CategorySummaryCard(
                state = state,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            Spacer(Modifier.height(16.dp))
        }

        // ===== AI REVIEW CARD =====
        // Stays invisible (AnimatedVisibility wraps the card itself) while aiReview
        // is Idle. Slides into view when the user taps the AI button below.
        item(key = "ai_review") {
            AiReviewCard(
                state = state.aiReview,
                accent = palette.accent,
                onGenerateAgain = onAiReviewClick,
                onDismiss = onDismissAiReview,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            if (state.aiReview !is AiReviewState.Idle) {
                Spacer(Modifier.height(16.dp))
            }
        }

        // ===== SECTION HEADER — "Danh sách thói quen" =====
        item(key = "habits_header") {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Danh sách thói quen",
                    style = BetterMeTypography.Title.Small.Bold,
                    color = BetterMeColors.Text.TextPrimary,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                if (state.habits.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(999.dp))
                            .background(palette.accent.copy(alpha = 0.14f))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "${state.habits.size}",
                            style = BetterMeTypography.Body.Small.Medium,
                            color = palette.accent,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
        }

        // ===== HABIT LIST or EMPTY STATE =====
        if (state.habits.isEmpty()) {
            item(key = "empty") {
                EmptyHabitsState(
                    accent = palette.accent,
                    soft = palette.soft,
                    onAddClick = onAddHabitClick,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
                Spacer(Modifier.height(16.dp))
            }
        } else {
            itemsIndexed(items = state.habits, key = { _, habit -> habit.id }) { index, habit ->
                Box(
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .clickable { onHabitClick(habit.id) }
                ) {
                    HabitDetailCard(
                        habit = habit,
                        emoji = habitEmojiFallback[index % habitEmojiFallback.size],
                        cardColor = palette.soft,
                        accentColor = palette.accent
                    )
                }
                Spacer(Modifier.height(12.dp))
            }
        }

        // ===== ACTION BUTTONS =====
        item(key = "actions") {
            Spacer(Modifier.height(20.dp))
            Text(
                text = "Hành động",
                style = BetterMeTypography.Title.Small.Bold,
                color = BetterMeColors.Text.TextPrimary,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            Spacer(Modifier.height(12.dp))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                CategoryActionButton(
                    icon = "🤖",
                    label = "AI nhận xét nhóm này",
                    accentColor = palette.accent,
                    onClick = onAiReviewClick
                )
                CategoryActionButton(
                    icon = "➕",
                    label = "Thêm thói quen mới vào nhóm",
                    accentColor = palette.accent,
                    onClick = onAddHabitClick
                )
                CategoryActionButton(
                    icon = "✨",
                    label = "AI gợi ý thói quen mới",
                    accentColor = palette.accent,
                    onClick = onAiSuggestClick
                )
            }
            Spacer(Modifier.height(120.dp)) // Bottom nav + FAB padding
        }
    }
}

/**
 * Empty-state panel for a category with no habits yet. Centered illustration disc
 * (category emoji on a soft gradient), motivational headline + body, and an inline
 * accent CTA. Consistent with the rest of the redesigned cards.
 */
@Composable
private fun EmptyHabitsState(
    accent: Color,
    soft: Color,
    onAddClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color.White, soft.copy(alpha = 0.55f))
                )
            )
            .padding(vertical = 28.dp, horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            accent.copy(alpha = 0.26f),
                            accent.copy(alpha = 0.12f)
                        )
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(text = "🌱", fontSize = 34.sp)
        }
        Spacer(Modifier.height(14.dp))
        Text(
            text = "Bắt đầu hành trình mới",
            style = BetterMeTypography.Title.Small.Bold,
            color = BetterMeColors.Text.TextPrimary,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = "Chưa có thói quen nào trong nhóm này. Thêm thói quen đầu tiên để bắt đầu.",
            style = BetterMeTypography.Body.Small.Medium,
            color = BetterMeColors.Text.TextTertiary,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(16.dp))
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(999.dp))
                .background(accent)
                .clickable { onAddClick() }
                .padding(horizontal = 18.dp, vertical = 10.dp)
        ) {
            Text(
                text = "+ Thêm thói quen",
                style = BetterMeTypography.Body.Medium,
                color = Color.White,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}
