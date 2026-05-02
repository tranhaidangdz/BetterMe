package com.example.betterme.presentation.habitdetail

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
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
                .statusBarsPadding()
                .padding(bottom = 80.dp), // Space for bottom button
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(bottom = 24.dp)
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
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(0.dp)
                ) {
                    HabitDetailTab.entries.forEach { tab ->
                        val isSelected = tab == state.selectedTab
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clickable { onIntent(HabitDetailIntent.SelectTab(tab)) }
                                .padding(bottom = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = tab.label,
                                    style = BetterMeTypography.Body.Small.Medium,
                                    color = if (isSelected) BetterMeColors.Primary.Primary
                                    else BetterMeColors.Text.TextTertiary
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(0.6f)
                                        .height(2.dp)
                                        .clip(RoundedCornerShape(1.dp))
                                        .background(
                                            if (isSelected) BetterMeColors.Primary.Primary
                                            else BetterMeColors.White.copy(alpha = 0f)
                                        )
                                )
                            }
                        }
                    }
                }
            }

            // ===== TAB CONTENT =====
            when (state.selectedTab) {
                HabitDetailTab.HISTORY -> {
                    // Calendar
                    item(key = "calendar") {
                        CheckInCalendar(
                            title = state.calendarTitle,
                            days = state.calendarDays,
                            onPreviousMonth = { onIntent(HabitDetailIntent.PreviousMonth) },
                            onNextMonth = { onIntent(HabitDetailIntent.NextMonth) },
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }

                    // Check-in logs
                    if (state.checkInLogs.isEmpty()) {
                        item(key = "empty_logs") {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(80.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Chưa có lịch sử check-in",
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

        // ===== BOTTOM BUTTON — Check in ngay =====
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(BetterMeColors.BackGround.BackgroundSecondary)
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Button(
                onClick = { onIntent(HabitDetailIntent.CheckInToday) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (state.isCompletedToday) BetterMeColors.Green
                    else BetterMeColors.Primary.Primary
                )
            ) {
                Text(
                    text = if (state.isCompletedToday) "✓ Đã check in hôm nay" else "+ Check in ngay",
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
                    .background(BetterMeColors.Black.copy(alpha = 0.1f)),
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
