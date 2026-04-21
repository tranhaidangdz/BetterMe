package com.example.betterme.presentation.categorydetail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.betterme.presentation.categorydetail.components.CategoryActionButton
import com.example.betterme.presentation.categorydetail.components.CategorySummaryCard
import com.example.betterme.presentation.categorydetail.components.HabitDetailCard
import com.example.betterme.presentation.theme.BetterMeColors
import com.example.betterme.presentation.theme.BetterMeTypography

// =========================
// SCREEN — nhận state/callback từ MainScreen
// =========================
@Composable
fun CategoryDetailScreen(
    state: CategoryDetailState,
    onBack: () -> Unit,
    onAiReviewClick: () -> Unit,
    onAddHabitClick: () -> Unit,
    onAiSuggestClick: () -> Unit,
) {
    val habitCardPalettes = listOf(
        Pair(Color(0xFFFFE8CC), Color(0xFFF39B2F)),
        Pair(Color(0xFFE8F6DB), Color(0xFF6EB448)),
        Pair(Color(0xFFDDF3FA), Color(0xFF2F94FF)),
    )
    val habitEmojis = listOf("🚶", "🧘", "🏋️", "🏃", "🚴")

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(BetterMeColors.BackGround.BackgroundSecondary)
            .statusBarsPadding()
            .imePadding(),
        verticalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        // ===== TOP BAR =====
        item(key = "topbar") {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(BetterMeColors.BackGround.BackgroundPrimary)
                    .padding(horizontal = 8.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Quay lại",
                        tint = BetterMeColors.Text.TextPrimary
                    )
                }
                Text(
                    text = state.categoryName.ifBlank { "Nhóm thói quen" },
                    style = BetterMeTypography.Title.Medium.Bold,
                    color = BetterMeColors.Text.TextPrimary,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = {}) {
                    Icon(
                        imageVector = Icons.Default.Notifications,
                        contentDescription = "Thông báo",
                        tint = BetterMeColors.Text.TextPrimary
                    )
                }
            }
        }

        // ===== CATEGORY SUMMARY CARD =====
        item(key = "summary") {
            Spacer(Modifier.height(12.dp))
            CategorySummaryCard(
                state = state,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            Spacer(Modifier.height(20.dp))
        }

        // ===== SECTION HEADER =====
        item(key = "habits_header") {
            Text(
                text = "Danh sách thói quen",
                style = BetterMeTypography.Title.Small.Bold,
                color = BetterMeColors.Text.TextPrimary,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            Spacer(Modifier.height(10.dp))
        }

        // ===== HABIT LIST =====
        if (state.habits.isEmpty()) {
            item(key = "empty") {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Chưa có thói quen nào trong nhóm này",
                        style = BetterMeTypography.Body.Medium,
                        color = BetterMeColors.Text.TextTertiary
                    )
                }
            }
        } else {
            itemsIndexed(items = state.habits, key = { _, habit -> habit.id }) { index, habit ->
                val palette = habitCardPalettes[index % habitCardPalettes.size]
                HabitDetailCard(
                    habit = habit,
                    emoji = habitEmojis[index % habitEmojis.size],
                    cardColor = palette.first,
                    accentColor = palette.second,
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
                Spacer(Modifier.height(10.dp))
            }
        }

        // ===== ACTION BUTTONS =====
        item(key = "actions") {
            Spacer(Modifier.height(8.dp))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                CategoryActionButton(
                    icon = "🤖",
                    label = "AI nhận xét nhóm này",
                    backgroundColor = Color(0xFFD7E9FF),
                    onClick = onAiReviewClick
                )
                CategoryActionButton(
                    icon = "➕",
                    label = "Thêm thói quen mới vào nhóm",
                    backgroundColor = Color(0xFFD7E9FF),
                    onClick = onAddHabitClick
                )
                CategoryActionButton(
                    icon = "🤖",
                    label = "AI gợi ý thói quen mới",
                    backgroundColor = Color(0xFFD7E9FF),
                    onClick = onAiSuggestClick
                )
            }
            Spacer(Modifier.height(120.dp)) // Bottom nav + FAB padding
        }
    }
}
