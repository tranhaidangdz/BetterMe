package com.example.betterme.presentation.habitdetail

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.betterme.R
import com.example.betterme.presentation.components.view.BetterMeTopBar
import com.example.betterme.presentation.habitdetail.components.*
import com.example.betterme.presentation.theme.BetterMeColors
import com.example.betterme.presentation.theme.BetterMeTypography
import kotlinx.coroutines.flow.collectLatest
import org.koin.androidx.compose.koinViewModel

@Composable
fun HabitDetailScreen(
    habitId: Int,
    onBackClick: () -> Unit,
    viewModel: HabitDetailViewModel = koinViewModel()
) {
    val state by viewModel.viewState.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(habitId) {
        viewModel.processIntent(HabitDetailIntent.LoadHabit(habitId))
    }

    LaunchedEffect(Unit) {
        viewModel.singleEvent.collectLatest { event ->
            when (event) {
                is HabitDetailEvent.ShowMessage ->
                    Toast.makeText(context, event.message, Toast.LENGTH_SHORT).show()
            }
        }
    }

    HabitDetailContent(
        state = state,
        onBackClick = onBackClick,
        onIntent = viewModel::processIntent
    )
}

@Composable
fun HabitDetailContent(
    state: HabitDetailState,
    onBackClick: () -> Unit,
    onIntent: (HabitDetailIntent) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BetterMeColors.BackGround.BackgroundSecondary)
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding(),
            contentPadding = PaddingValues(bottom = 100.dp)
        ) {
            // ===== TOP BAR =====
            item(key = "topbar") {
                BetterMeTopBar(
                    leadingIconRes = R.drawable.ic_arrow_left,
                    title = "Chi tiết thói quen",
                    onLeadingClick = onBackClick
                )
            }

            // ===== HABIT INFO CARD =====
            item(key = "info") {
                Spacer(modifier = Modifier.height(8.dp))
                HabitInfoCard(
                    title = state.habitTitle,
                    categoryName = state.categoryName,
                    categoryIcon = state.categoryIcon,
                    isCompletedToday = state.isCompletedToday,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }

            // ===== STREAK CARD =====
            item(key = "streak") {
                Spacer(modifier = Modifier.height(10.dp))
                StreakCard(
                    currentStreak = state.currentStreak,
                    longestStreak = state.longestStreak,
                    weeklyProgress = state.weeklyProgress,
                    weeklyTotal = state.weeklyTotal,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }

            // ===== MINI TABS =====
            item(key = "tabs") {
                Spacer(modifier = Modifier.height(14.dp))
                MiniTabBar(
                    selectedTab = state.selectedTab,
                    onSelectTab = { onIntent(HabitDetailIntent.SelectTab(it)) },
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
                Spacer(modifier = Modifier.height(10.dp))
            }

            // ===== TAB CONTENT =====
            when (state.selectedTab) {
                HabitDetailTab.HISTORY -> {
                    item(key = "calendar") {
                        CheckInCalendar(
                            title = state.calendarTitle,
                            days = state.calendarDays,
                            onPreviousMonth = { onIntent(HabitDetailIntent.PreviousMonth) },
                            onNextMonth = { onIntent(HabitDetailIntent.NextMonth) },
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }

                    if (state.checkInLogs.isEmpty()) {
                        item(key = "empty_logs") {
                            Spacer(modifier = Modifier.height(8.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(BetterMeColors.White)
                                    .padding(vertical = 32.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "📋 Chưa có lịch sử check-in",
                                    style = BetterMeTypography.Body.Medium,
                                    color = BetterMeColors.Text.TextTertiary
                                )
                            }
                        }
                    } else {
                        items(
                            items = state.checkInLogs,
                            key = { it.logId }
                        ) { log ->
                            Spacer(modifier = Modifier.height(8.dp))
                            CheckInLogCard(
                                log = log,
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )
                        }
                    }
                }

                HabitDetailTab.AI_SUGGEST -> {
                    item(key = "ai_suggest") {
                        AiSuggestTabContent(
                            habitTitle = state.habitTitle,
                            categoryName = state.categoryName,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }
                }

                HabitDetailTab.STATS -> {
                    item(key = "stats") {
                        StatsTabContent(
                            stats = state.stats,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }
                }
            }
        }

        // ===== STICKY BOTTOM BUTTON =====
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .shadow(
                    elevation = 8.dp,
                    shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
                    clip = false
                )
                .background(
                    color = BetterMeColors.White,
                    shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
                )
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 14.dp)
        ) {
            Button(
                onClick = { onIntent(HabitDetailIntent.CheckInToday) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (state.isCompletedToday) BetterMeColors.Green
                    else BetterMeColors.Primary.Primary
                ),
                elevation = ButtonDefaults.buttonElevation(
                    defaultElevation = 2.dp,
                    pressedElevation = 0.dp
                )
            ) {
                Text(
                    text = if (state.isCompletedToday) "✓ Đã check in hôm nay"
                    else "+ Check in ngay",
                    style = BetterMeTypography.Title.Small.Bold,
                    color = BetterMeColors.White
                )
            }
        }

        // Loading overlay
        if (state.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(BetterMeColors.Black.copy(alpha = 0.08f)),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(40.dp),
                    color = BetterMeColors.Primary.Primary,
                    strokeWidth = 3.dp
                )
            }
        }
    }
}

// ============================================================
// MINI TAB BAR — Lịch sử | Nhắc nhở | Thống kê
// ============================================================
@Composable
private fun MiniTabBar(
    selectedTab: HabitDetailTab,
    onSelectTab: (HabitDetailTab) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(BetterMeColors.White)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        HabitDetailTab.entries.forEach { tab ->
            val isSelected = tab == selectedTab
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(
                        if (isSelected) BetterMeColors.Primary.Primary.copy(alpha = 0.1f)
                        else BetterMeColors.White
                    )
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { onSelectTab(tab) }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = tab.label,
                    style = BetterMeTypography.Body.Small.Medium.copy(
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                    ),
                    color = if (isSelected) BetterMeColors.Primary.Primary
                    else BetterMeColors.Text.TextTertiary,
                    maxLines = 1
                )
            }
        }
    }
}
