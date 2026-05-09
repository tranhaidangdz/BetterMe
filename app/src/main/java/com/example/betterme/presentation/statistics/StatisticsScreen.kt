package com.example.betterme.presentation.statistics

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.betterme.presentation.statistics.components.*
import com.example.betterme.presentation.theme.BetterMeColors
import com.example.betterme.presentation.theme.BetterMeTypography
import org.koin.androidx.compose.koinViewModel

@Composable
fun StatisticsScreen(
    onBackClick: () -> Unit = {},
    viewModel: StatisticsViewModel = koinViewModel()
) {
    val state by viewModel.viewState.collectAsState()

    StatisticsContent(
        state = state,
        onTabSelected = { viewModel.processIntent(StatisticsIntent.SelectTab(it)) },
        onToggleSection = { viewModel.processIntent(StatisticsIntent.ToggleSection(it)) },
        onBackClick = onBackClick
    )
}

@Composable
fun StatisticsContent(
    state: StatisticsState,
    onTabSelected: (StatisticsTab) -> Unit = {},
    onToggleSection: (ExpandedSection) -> Unit = {},
    onBackClick: () -> Unit = {}
) {
    var showContent by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { showContent = true }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF2F8FF))
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // ===== HEADER + TABS =====
            item(key = "header") {
                StatisticsHeader(
                    selectedTab = state.selectedTab,
                    onTabSelected = onTabSelected,
                    onBackClick = onBackClick
                )
            }

            // ===== OVERVIEW SECTION =====
            item(key = "overview") {
                AnimatedVisibility(
                    visible = showContent,
                    enter = fadeIn(tween(400)) + slideInVertically(
                        initialOffsetY = { it / 4 },
                        animationSpec = tween(400)
                    )
                ) {
                    OverviewCards(stats = state.overview)
                }
            }

            // ===== PIE CHART SECTION =====
            item(key = "pie_chart") {
                AnimatedVisibility(
                    visible = showContent,
                    enter = fadeIn(tween(500, delayMillis = 100)) + slideInVertically(
                        initialOffsetY = { it / 4 },
                        animationSpec = tween(500, delayMillis = 100)
                    )
                ) {
                    PieChartCard(stats = state.overview)
                }
            }

            // ===== STREAK ANALYTICS SECTION =====
            item(key = "streaks") {
                AnimatedVisibility(
                    visible = showContent,
                    enter = fadeIn(tween(500, delayMillis = 200)) + slideInVertically(
                        initialOffsetY = { it / 4 },
                        animationSpec = tween(500, delayMillis = 200)
                    )
                ) {
                    StreakCard(streaks = state.streakAnalytics)
                }
            }

            // ===== FEATURED HABITS SECTION =====
            item(key = "featured_header") {
                AnimatedVisibility(
                    visible = showContent,
                    enter = fadeIn(tween(500, delayMillis = 300))
                ) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Thói quen nổi bật",
                                style = BetterMeTypography.Title.Medium.SemiBold,
                                color = BetterMeColors.Text.TextPrimary
                            )
                            Text(
                                text = "Xem tất cả →",
                                style = BetterMeTypography.Body.Small.Medium,
                                color = BetterMeColors.Primary.Primary
                            )
                        }
                    }
                }
            }

            itemsIndexed(
                items = state.featuredHabits,
                key = { index, habit -> "featured_$index" }
            ) { index, habit ->
                AnimatedVisibility(
                    visible = showContent,
                    enter = fadeIn(tween(400, delayMillis = 350 + index * 80)) + slideInVertically(
                        initialOffsetY = { it / 3 },
                        animationSpec = tween(400, delayMillis = 350 + index * 80)
                    )
                ) {
                    FeaturedHabitCard(
                        habit = habit,
                        index = index
                    )
                }
            }

            // ===== HABIT JOURNEY SECTION =====
            item(key = "journey") {
                AnimatedVisibility(
                    visible = showContent,
                    enter = fadeIn(tween(500, delayMillis = 500)) + slideInVertically(
                        initialOffsetY = { it / 4 },
                        animationSpec = tween(500, delayMillis = 500)
                    )
                ) {
                    HabitJourneyCard(journey = state.habitJourney)
                }
            }

            // ===== HABIT STATUS LISTS (Expandable) =====
            item(key = "habit_list_header") {
                AnimatedVisibility(
                    visible = showContent,
                    enter = fadeIn(tween(500, delayMillis = 600))
                ) {
                    Text(
                        text = "Danh sách thói quen",
                        style = BetterMeTypography.Title.Medium.SemiBold,
                        color = BetterMeColors.Text.TextPrimary
                    )
                }
            }

            // Completed habits
            item(key = "completed_section") {
                AnimatedVisibility(
                    visible = showContent,
                    enter = fadeIn(tween(400, delayMillis = 650)) + slideInVertically(
                        initialOffsetY = { it / 4 },
                        animationSpec = tween(400, delayMillis = 650)
                    )
                ) {
                    HabitStatusSection(
                        title = "Hoàn thành",
                        section = ExpandedSection.COMPLETED,
                        isExpanded = state.expandedSection == ExpandedSection.COMPLETED,
                        habits = state.completedHabits,
                        onToggle = { onToggleSection(ExpandedSection.COMPLETED) }
                    )
                }
            }

            // Failed habits
            item(key = "failed_section") {
                AnimatedVisibility(
                    visible = showContent,
                    enter = fadeIn(tween(400, delayMillis = 700)) + slideInVertically(
                        initialOffsetY = { it / 4 },
                        animationSpec = tween(400, delayMillis = 700)
                    )
                ) {
                    HabitStatusSection(
                        title = "Thất bại",
                        section = ExpandedSection.FAILED,
                        isExpanded = state.expandedSection == ExpandedSection.FAILED,
                        habits = state.failedHabits,
                        onToggle = { onToggleSection(ExpandedSection.FAILED) }
                    )
                }
            }

            // Ongoing habits
            item(key = "ongoing_section") {
                AnimatedVisibility(
                    visible = showContent,
                    enter = fadeIn(tween(400, delayMillis = 750)) + slideInVertically(
                        initialOffsetY = { it / 4 },
                        animationSpec = tween(400, delayMillis = 750)
                    )
                ) {
                    HabitStatusSection(
                        title = "Đang thực hiện",
                        section = ExpandedSection.ONGOING,
                        isExpanded = state.expandedSection == ExpandedSection.ONGOING,
                        habits = state.ongoingHabits,
                        onToggle = { onToggleSection(ExpandedSection.ONGOING) }
                    )
                }
            }

            // ===== ADDITIONAL INSIGHTS =====
            item(key = "insights") {
                AnimatedVisibility(
                    visible = showContent,
                    enter = fadeIn(tween(500, delayMillis = 800)) + slideInVertically(
                        initialOffsetY = { it / 4 },
                        animationSpec = tween(500, delayMillis = 800)
                    )
                ) {
                    InsightCard(insights = state.additionalInsights)
                }
            }

            // Bottom spacer for navigation bar
            item(key = "bottom_spacer") {
                Spacer(modifier = Modifier.height(120.dp))
            }
        }

        // Loading overlay
        if (state.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(BetterMeColors.Black.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = BetterMeColors.Primary.Primary)
            }
        }
    }
}
